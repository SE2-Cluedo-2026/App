package at.aau.serg.websocketbrokerdemo

import android.app.AlertDialog
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.*
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import at.aau.serg.websocketbrokerdemo.messaging.dtos.ExistingPlayerDTO
import at.aau.serg.websocketbrokerdemo.model.CardRepository
import at.aau.serg.websocketbrokerdemo.model.ClientState
import at.aau.serg.websocketbrokerdemo.network.lobby.LobbyHandler
import com.example.myapplication.R
import java.util.UUID
import at.aau.serg.websocketbrokerdemo.GameActivity
class LobbyActivity : ComponentActivity() {

    private var availableCharacters: List<String> = emptyList()
    private var currentCharacterIndex = 0
    private var isLeaving = false
    private var isReady = false
    private var bgMusic: MediaPlayer? = null

    private val disconnectHandler = Handler(Looper.getMainLooper())
    private val disconnectRunnable = Runnable {
        if (!isLeaving) {
            isLeaving = true
            stopDisconnectService()
            MyStomp.instance.leaveLobby()
            MyStomp.instance.disconnect()
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun startDisconnectService() {
        startService(Intent(this, DisconnectService::class.java).apply {
            putExtra(DisconnectService.EXTRA_MODE, DisconnectService.MODE_LOBBY)
        })
    }

    private fun stopDisconnectService() {
        stopService(Intent(this, DisconnectService::class.java))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lobby)

        startDisconnectService()

        bgMusic = MediaPlayer.create(this, R.raw.lobby_music)
        bgMusic?.isLooping = true
        bgMusic?.setVolume(0.1f, 0.1f)
        bgMusic?.start()

        val loadingOverlay = findViewById<android.widget.FrameLayout>(R.id.loadingOverlay)
        loadingOverlay.visibility = android.view.View.VISIBLE

        if (ClientState.players.isNotEmpty()) {
            loadingOverlay.visibility = android.view.View.GONE
        }

        LobbyHandler.onGameStarted = {
            runOnUiThread {
                isLeaving = true
                disconnectHandler.removeCallbacks(disconnectRunnable)
                stopDisconnectService()
                bgMusic?.stop()
                bgMusic?.release()
                bgMusic = null
                val intent = Intent(this, GameActivity::class.java)
                startActivity(intent)
            }
        }
        LobbyHandler.onStartGameError = { reason ->
            runOnUiThread {
                Toast.makeText(this, reason, Toast.LENGTH_SHORT).show()
            }
        }
        LobbyHandler.onError = { reason ->
            runOnUiThread {
                Toast.makeText(this, reason, Toast.LENGTH_SHORT).show()
            }
        }
        val imgMyCharacter = findViewById<ImageView>(R.id.imgMyCharacter)

        val btnPrev = findViewById<ImageButton>(R.id.btnPrev)
        val btnNext = findViewById<ImageButton>(R.id.btnNext)
        val btnReady = findViewById<Button>(R.id.btnReady)
        val btnLeave = findViewById<Button>(R.id.btnLeave)
        val btnStartGame = findViewById<Button>(R.id.btnStartGame)

        val otherPlayerViews = listOf(
            findViewById<ImageView>(R.id.imgOtherPlayerCharacter2),
            findViewById<ImageView>(R.id.imgOtherPlayerCharacter3),
            findViewById<ImageView>(R.id.imgOtherPlayerCharacter4)
        )

        val otherReadyChecks = listOf(
            findViewById<ImageView>(R.id.imgReadyCheck2),
            findViewById<ImageView>(R.id.imgReadyCheck3),
            findViewById<ImageView>(R.id.imgReadyCheck4)
        )

        availableCharacters = ClientState.availableCharacters.toList()
        isReady = false
        btnPrev.visibility = View.VISIBLE
        btnNext.visibility = View.VISIBLE
        btnReady.isEnabled = true
        findViewById<ImageView>(R.id.imgReadyCheck).visibility = View.GONE

        Log.d("LOBBY", "INIT characters = $availableCharacters")

        val me = ClientState.players.find { it.playerId == ClientState.playerId }
        if (me != null && me.character != null) {
            ClientState.myCharacter = me.character
            if (me.ready) {
                isReady = true
                lockCharacterSelection()
            }
        } else {
            currentCharacterIndex = 0
            if (availableCharacters.isNotEmpty()) {
                ClientState.myCharacter = availableCharacters[0]
            }
        }
        updateMyCharacterImage(imgMyCharacter)
        updateOtherPlayers(ClientState.players, otherPlayerViews, otherReadyChecks)

        btnNext.setOnClickListener {
            if (availableCharacters.isEmpty()) return@setOnClickListener

            currentCharacterIndex =
                (currentCharacterIndex + 1) % availableCharacters.size
            ClientState.myCharacter = availableCharacters[currentCharacterIndex]
            updateMyCharacterImage(imgMyCharacter)
        }
        btnPrev.setOnClickListener {
            if (availableCharacters.isEmpty()) return@setOnClickListener

            currentCharacterIndex =
                (currentCharacterIndex - 1 + availableCharacters.size) % availableCharacters.size
            ClientState.myCharacter = availableCharacters[currentCharacterIndex]

            updateMyCharacterImage(imgMyCharacter)
        }
        btnReady.setOnClickListener {
            if (availableCharacters.isEmpty()) return@setOnClickListener
            val selectedCharacter = availableCharacters.getOrNull(currentCharacterIndex) ?: return@setOnClickListener
            ClientState.myCharacter = selectedCharacter
            isReady = true
            lockCharacterSelection()
            MyStomp.instance.setReady(selectedCharacter, true)
        }

        btnStartGame.setOnClickListener {
            MyStomp.instance.startGame()
        }

        btnLeave.setOnClickListener {
            if (isLeaving) return@setOnClickListener
            isLeaving = true
            btnLeave.isEnabled = false
            disconnectHandler.removeCallbacks(disconnectRunnable)
            stopDisconnectService()
            MyStomp.instance.leaveLobby()
            MyStomp.instance.disconnect()
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
        LobbyHandler.onNewPlayerJoined = { dto ->
            runOnUiThread {
                loadingOverlay.visibility = android.view.View.GONE
                playSound(R.raw.join_sound)
                ClientState.players = dto.existingPlayers
                ClientState.availableCharacters = dto.availableCharacters
                availableCharacters = dto.availableCharacters.ifEmpty {
                    ClientState.availableCharacters
                }

                Log.d("LOBBY", "UPDATED characters = $availableCharacters")

                if (availableCharacters.isNotEmpty() && !isReady) {
                    ClientState.myCharacter = availableCharacters[0]
                }

                updateMyCharacterImage(imgMyCharacter)
                updateOtherPlayers(dto.existingPlayers, otherPlayerViews, otherReadyChecks)
            }


        }

        LobbyHandler.onSetReady = { dto ->
            runOnUiThread {
                playSound(R.raw.player_ready_sound)
                ClientState.players = dto.existingPlayers
                ClientState.availableCharacters = dto.availableCharacters

                availableCharacters = dto.availableCharacters.ifEmpty {
                    ClientState.availableCharacters
                }

                updateMyCharacterImage(imgMyCharacter)
                updateOtherPlayers(dto.existingPlayers, otherPlayerViews, otherReadyChecks)
            }
        }

        LobbyHandler.onPlayerRemoved = {
            runOnUiThread {
                MyStomp.instance.disconnect()
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
            }
        }

        LobbyHandler.onOtherPlayerRemoved = { playerId ->
            runOnUiThread {
                playSound(R.raw.player_lobby_leave_sound)
                val updated = ClientState.players.filter { it.playerId != playerId }
                ClientState.players = updated
                updateOtherPlayers(updated, otherPlayerViews, otherReadyChecks)
            }
        }

        LobbyHandler.onGameFull = { dto ->
            runOnUiThread {
                AlertDialog.Builder(this)
                    .setTitle("Fehler")
                    .setMessage(dto.message)
                    .setPositiveButton("OK") { d, _ -> d.dismiss() }
                    .show()
            }
        }

        onBackPressedDispatcher.addCallback(this) {
            if (isLeaving) return@addCallback
            isLeaving = true
            disconnectHandler.removeCallbacks(disconnectRunnable)
            stopDisconnectService()
            MyStomp.instance.leaveLobby()
            MyStomp.instance.disconnect()
            val intent = Intent(this@LobbyActivity, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }


    override fun onPause() {
        super.onPause()
        bgMusic?.pause()
        if (!isLeaving) {
            disconnectHandler.postDelayed(disconnectRunnable, 5000)
        }
    }

    override fun onResume() {
        super.onResume()
        bgMusic?.start()
        disconnectHandler.removeCallbacks(disconnectRunnable)
    }

    private fun playSound(resId: Int) {
        val player = MediaPlayer.create(this, resId)
        player?.start()
        player?.setOnCompletionListener { it.release() }
    }

    override fun onDestroy() {
        bgMusic?.stop()
        bgMusic?.release()
        bgMusic = null
        // Only clear callbacks when the system destroys us (e.g. config change).
        // When isLeaving is true, MainActivity is already setting up its own
        // callbacks in onStart() — clearing here would null them out.
        if (!isLeaving) {
            LobbyHandler.onLobbyJoined = null
            LobbyHandler.onNewPlayerJoined = null
            LobbyHandler.onPlayerRejoined = null
            LobbyHandler.onPlayerRejoinedRunning = null
            LobbyHandler.onGameFull = null
            LobbyHandler.onPlayerRemoved = null
            LobbyHandler.onOtherPlayerRemoved = null
            LobbyHandler.onSetReady = null
            LobbyHandler.onGameStarted = null
            LobbyHandler.onStartGameError = null
            LobbyHandler.onError = null
        }
        super.onDestroy()
    }

    private fun lockCharacterSelection() {
        findViewById<ImageButton>(R.id.btnPrev).visibility = View.GONE
        findViewById<ImageButton>(R.id.btnNext).visibility = View.GONE
        findViewById<Button>(R.id.btnReady).isEnabled = false
        findViewById<ImageView>(R.id.imgReadyCheck).visibility = View.VISIBLE
        findViewById<Button>(R.id.btnStartGame).alpha = 1f
    }
    private fun updateMyCharacterImage(imgView: ImageView) {

        val characterId = ClientState.myCharacter

        Log.d("LOBBY", "render character = $characterId")

        if (characterId == null) {
            imgView.setImageResource(android.R.drawable.ic_menu_help)
            return
        }

        val card = CardRepository.cards.find { it.cardId == characterId }

        imgView.setImageResource(
            card?.imageResId ?: android.R.drawable.ic_menu_help
        )
    }

    private fun updateOtherPlayers(
        players: List<ExistingPlayerDTO>,
        views: List<ImageView>,
        readyChecks: List<ImageView>
    ) {
        val others = players.filter { it.playerId != ClientState.playerId }

        views.forEach { it.setImageDrawable(null) }
        readyChecks.forEach { it.visibility = View.GONE }

        others.forEachIndexed { index, player ->
            if (index >= views.size) return@forEachIndexed

            val card = player.character?.let {
                CardRepository.cards.find { c -> c.cardId == it }
            }

            views[index].setImageResource(
                card?.imageResId ?: android.R.drawable.ic_menu_help
            )
            readyChecks[index].visibility = if (player.ready) View.VISIBLE else View.GONE
        }
    }
}