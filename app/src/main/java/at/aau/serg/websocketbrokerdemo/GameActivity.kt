package at.aau.serg.websocketbrokerdemo

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.core.content.res.ResourcesCompat
import at.aau.serg.websocketbrokerdemo.model.BoardColors
import com.example.myapplication.R
import at.aau.serg.websocketbrokerdemo.model.BoardConfig
import at.aau.serg.websocketbrokerdemo.model.ClientState
import at.aau.serg.websocketbrokerdemo.network.game.GameHandler

class GameActivity : ComponentActivity() {
    private lateinit var rootLayout: ViewGroup
    private lateinit var boardImage: ImageView
    private lateinit var gridOverlay: ViewGroup

    private lateinit var checklistOverlay: ViewGroup
    private lateinit var characterPanel: android.widget.LinearLayout

    private lateinit var dialogOverlay: ViewGroup

    private val playerDots = mutableMapOf<String, View>()
    private val characterHighlights = mutableMapOf<String, View>()
    private var currentRoomId: String? = null
    private var hiddenWayUsed = false
    private var boardSetupDone = false

    private lateinit var btnRollDice: Button
    private lateinit var btnHiddenWay: Button
    private lateinit var btnSuggest: Button
    private lateinit var btnAccuse: Button
    private lateinit var btnLeave: Button

    private val playerStatusViews = mutableMapOf<String, TextView>()
    private var pauseOverlay: View? = null
    private var countdownHandler: android.os.Handler? = null
    private var countdownRunnable: Runnable? = null
    private var cheatWindowOverlay: View? = null
    private var cheatDecisionOverlay: View? = null
    private var cheatWindowHandler: android.os.Handler? = null
    private var cheatWindowRunnable: Runnable? = null
    private var cheatDecisionHandler: android.os.Handler? = null
    private var cheatDecisionRunnable: Runnable? = null
    private var sensorManager: android.hardware.SensorManager? = null
    private var shakeDetector: ShakeDetector? = null

    private var bgMusic: MediaPlayer? = null
    private var waitingMusic: MediaPlayer? = null

    private var isLeaving = false
    private val bgDisconnectHandler = Handler(Looper.getMainLooper())
    private val bgDisconnectRunnable = Runnable {
        if (!isLeaving) {
            isLeaving = true
            stopDisconnectService()
            MyStomp.instance.disconnect()
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun startDisconnectService() {
        startService(Intent(this, DisconnectService::class.java).apply {
            putExtra(DisconnectService.EXTRA_MODE, DisconnectService.MODE_GAME)
        })
    }

    private fun stopDisconnectService() {
        stopService(Intent(this, DisconnectService::class.java))
    }

    private fun playSound(resId: Int) {
        val player = MediaPlayer.create(this, resId)
        player?.start()
        player?.setOnCompletionListener { it.release() }
    }

    private fun stopWaitingMusic() {
        waitingMusic?.stop()
        waitingMusic?.release()
        waitingMusic = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)
        window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))

        startDisconnectService()

        bgMusic = MediaPlayer.create(this, R.raw.game_music)
        bgMusic?.isLooping = true
        bgMusic?.setVolume(0.1f, 0.1f)
        bgMusic?.start()

        rootLayout = findViewById(R.id.rootGameLayout)
        boardImage = findViewById(R.id.imgBoard)
        gridOverlay = findViewById(R.id.gridOverlay)
        checklistOverlay = findViewById(R.id.checklistOverlay)
        characterPanel = findViewById(R.id.characterPanel)

        dialogOverlay = findViewById(R.id.dialogOverlay)

        setupGameHandlers()
        initializePlayerPositions()

        btnRollDice = findViewById(R.id.btnRollDice)
        btnHiddenWay = findViewById(R.id.btnHiddenWay)
        btnSuggest = findViewById(R.id.btnSuggest)
        btnAccuse = findViewById(R.id.btnAccuse)
        btnLeave = findViewById(R.id.btnLeave)

        findViewById<Button>(R.id.btnRollDice).setOnClickListener { onRollDice() }
        findViewById<Button>(R.id.btnHiddenWay).setOnClickListener { onHiddenWay() }
        findViewById<Button>(R.id.btnSuggest).setOnClickListener { onSuggest() }
        findViewById<Button>(R.id.btnAccuse).setOnClickListener { onAccuse() }
        findViewById<Button>(R.id.btnLeave).setOnClickListener { onLeaveGame() }

        // Wait for gridOverlay to have real dimensions before setting up the board
        gridOverlay.viewTreeObserver.addOnGlobalLayoutListener(object :
            android.view.ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (gridOverlay.width > 0 && gridOverlay.height > 0) {
                    // Remove listener immediately so setupBoard() is called exactly once
                    gridOverlay.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    if (!boardSetupDone) {
                        boardSetupDone = true
                        setupBoard()
                        // Re-apply positions that may have arrived before layout was ready
                        placeAllPlayerDots()
                        updateAllPlayerStatuses()
                        updateCurrentPlayerHighlight()
                        updateButtonStates()
                    }
                }
            }
        })

        onBackPressedDispatcher.addCallback(this) {
            onLeaveGame()
            finish()
        }
    }

    private fun setupBoard() {
        val container = findViewById<View>(R.id.boardContainer)
        val imgH = container.height
        val imgY = container.top

        if (imgH == 0) return

        val gridW = gridOverlay.width
        val gridH = gridOverlay.height

        val cellW = gridW / BoardConfig.COLS
        val cellH = gridH / BoardConfig.ROWS

        gridOverlay.removeAllViews()

        for (row in 0 until BoardConfig.ROWS) {
            for (col in 0 until BoardConfig.COLS) {
                val cell = View(this)
                val clp =
                    androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(cellW, cellH)
                        .apply {
                            startToStart = androidx.constraintlayout.widget.ConstraintSet.PARENT_ID
                            topToTop = androidx.constraintlayout.widget.ConstraintSet.PARENT_ID
                            leftMargin = col * cellW
                            topMargin = row * cellH
                        }
                cell.layoutParams = clp
                cell.setBackgroundColor(Color.TRANSPARENT)
                cell.setOnClickListener { onCellTapped(col, row) }
                gridOverlay.addView(cell)
            }
        }
        val roomOverlay = findViewById<ViewGroup>(R.id.roomOverlay)
        roomOverlay?.removeAllViews()

        setupCharacterPanel(imgY, imgH)
        updateChecklist()
        updateCurrentPlayerHighlight()
        updateButtonStates()
        placeAllPlayerDots()
    }

    private fun setupCharacterPanel(imgY: Int, imgH: Int) {
        characterPanel.removeAllViews()
        characterHighlights.clear()
        playerStatusViews.clear()
        characterPanel.setBackgroundColor(Color.argb(0, 0, 0, 0))
        characterPanel.visibility = View.VISIBLE

        for (player in ClientState.players) {
            val charType = player.character ?: continue

            val itemView =
                layoutInflater.inflate(R.layout.player_panel_entry, characterPanel, false)

            val imageResId = when (charType.uppercase().replace(" ", "_")) {
                "DR_RED" -> R.drawable.cdrred
                "DR_BLUE" -> R.drawable.cdrblue
                "MRS_PINK" -> R.drawable.cmrspink
                "MRS_LAVENDER" -> R.drawable.cmrslavender
                else -> android.R.drawable.ic_menu_help
            }
            itemView.findViewById<ImageView>(R.id.imgCharacter).setImageResource(imageResId)

            val highlightView = itemView.findViewById<View>(R.id.viewActiveHighlight)
            val border = GradientDrawable()
            border.setStroke(GameUIHelper.dpToPx(this, 3), Color.parseColor("#F50057"))
            border.cornerRadius = GameUIHelper.dpToPx(this, 4).toFloat()
            border.setColor(Color.TRANSPARENT)
            highlightView.background = border
            highlightView.visibility = View.GONE
            characterHighlights[player.playerId] = highlightView

            playerStatusViews[player.playerId] = itemView.findViewById(R.id.tvStatus)
            characterPanel.addView(itemView)
        }
    }

    /*
    private fun initializePlayerPositions() {
        val players = ClientState.players
        players.forEach { player ->
            val charType = ClientState.playerCharacterMap[player.playerId] ?: player.character
            val startPos = charType?.let { BoardConfig.CHARACTER_START_POSITIONS[it] }
            if (startPos != null) {
                ClientState.playerPositions[player.playerId] =
                    "${startPos.first},${startPos.second}"
            } else {
                ClientState.playerPositions[player.playerId] = "6,4"
            }
        }
    }


     */
    private fun initializePlayerPositions() {
        val players = ClientState.players
        players.forEach { player ->
            // Nur setzen wenn noch KEINE Position bekannt ist (z.B. echter Neustart)
            if (!ClientState.playerPositions.containsKey(player.playerId)) {
                val charType = ClientState.playerCharacterMap[player.playerId] ?: player.character
                val startPos = charType?.let { BoardConfig.CHARACTER_START_POSITIONS[it] }
                ClientState.playerPositions[player.playerId] =
                    if (startPos != null) "${startPos.first},${startPos.second}" else "6,4"
            }
        }
    }
    private fun onCellTapped(col: Int, row: Int) {
        if (!isMyTurn() || ClientState.isEliminated) return

        val currentPosStr = ClientState.playerPositions[ClientState.playerId]
        if (currentPosStr != null && currentPosStr.contains(",")) {
            val parts = currentPosStr.split(",")
            val c = parts[0].trim().toIntOrNull() ?: 0
            val r = parts[1].trim().toIntOrNull() ?: 0
            if (!BoardConfig.isAdjacent(c, r, col, row)) {
                Toast.makeText(this, getString(R.string.move_not_adjacent), Toast.LENGTH_SHORT)
                    .show()
                return
            }
        }

        if (!BoardConfig.isWalkable(col, row)) {
            Toast.makeText(this, getString(R.string.move_not_walkable), Toast.LENGTH_SHORT).show()
            return
        }

        val phase = ClientState.currentPhase

        // Problem 3 Fix: Do not auto-enter room. Send move command to door field.
        if (phase == "WAITING_FOR_MOVE" && ClientState.remainingMoves > 0) {
            MyStomp.instance.move("$col,$row")
        }
    }

    private fun onRollDice() {
        if (!isMyTurn() || ClientState.isEliminated) return
        if (ClientState.currentPhase != "WAITING_FOR_ROLL") return
        MyStomp.instance.rollDice()
    }

    private fun onHiddenWay() {
        if (!isMyTurn() || ClientState.isEliminated) return
        val pos = ClientState.playerPositions[ClientState.playerId] ?: ""
        if (ClientState.currentPhase != "IN_ROOM" && ClientState.currentPhase != "WAITING_FOR_ROLL") return
        if (BoardConfig.HIDDEN_PASSAGES.containsKey(pos)) {
            MyStomp.instance.takeHiddenWay()
        } else {
            Toast.makeText(this, getString(R.string.no_hidden_passage), Toast.LENGTH_SHORT).show()
        }
    }

    private fun isMyTurn(): Boolean {
        val players = ClientState.players
        if (players.isEmpty() || ClientState.currentPlayerIndex >= players.size) return false
        return players[ClientState.currentPlayerIndex].playerId == ClientState.playerId
    }

    private fun onSuggest() {
        if (!isMyTurn() || ClientState.isEliminated) return
        val pos = ClientState.playerPositions[ClientState.playerId] ?: ""
        if (ClientState.currentPhase != "IN_ROOM" && ClientState.currentPhase != "WAITING_FOR_ROLL") return
        if (!BoardConfig.ROOM_CENTERS_PERCENT.containsKey(pos)) return

        dialogOverlay.visibility = View.VISIBLE
        GameUIHelper.showCardSelectionOverlay(
            this, dialogOverlay, "MAKE A SUGGESTION",
            includeRooms = false,
            currentRoom = pos
        ) { suspect, room, weapon ->
            dialogOverlay.visibility = View.GONE
            MyStomp.instance.makeSuggestion(suspect, room, weapon)
        }
    }

    private fun onAccuse() {
        if (!isMyTurn() || ClientState.isEliminated) return
        val pos = ClientState.playerPositions[ClientState.playerId] ?: ""
        if (ClientState.currentPhase != "IN_ROOM" && ClientState.currentPhase != "WAITING_FOR_ROLL") return
        if (!BoardConfig.ROOM_CENTERS_PERCENT.containsKey(pos)) return

        dialogOverlay.visibility = View.VISIBLE
        GameUIHelper.showCardSelectionOverlay(
            this, dialogOverlay, "MAKE AN ACCUSATION",
            includeRooms = false,
            currentRoom = pos
        ) { suspect, room, weapon ->
            dialogOverlay.visibility = View.GONE
            MyStomp.instance.makeAccusation(suspect, room, weapon)
        }
    }

    override fun onPause() {
        super.onPause()
        bgMusic?.pause()
        if (!isLeaving) {
            bgDisconnectHandler.postDelayed(bgDisconnectRunnable, 5000)
        }
    }

    override fun onResume() {
        super.onResume()
        bgDisconnectHandler.removeCallbacks(bgDisconnectRunnable)
        if (waitingMusic == null) {
            bgMusic?.start()
        }
    }

    private fun onLeaveGame() {
        if (isLeaving) return
        isLeaving = true
        bgDisconnectHandler.removeCallbacks(bgDisconnectRunnable)
        stopDisconnectService()
        bgMusic?.stop()
        bgMusic?.release()
        bgMusic = null
        stopWaitingMusic()
        // Only disconnect — the server's SessionDisconnectEvent will start the 30-second
        // pause/rejoin timer. Calling leaveLobby() before disconnect would race with
        // the session closing and could bypass the rejoin logic entirely.
        MyStomp.instance.disconnect()
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    private fun setupGameHandlers() {
        GameHandler.onRollDice = { value, newPosition ->
            runOnUiThread {
                playSound(R.raw.roll_dice_sound)
                Toast.makeText(this, getString(R.string.dice_result, value), Toast.LENGTH_SHORT)
                    .show()
                hiddenWayUsed = false
                if (newPosition != null) {
                    updatePlayerDot(ClientState.playerId, newPosition)
                }
                updateButtonStates()
                updateAllPlayerStatuses()
            }
        }

        GameHandler.onMove = { playerId, position, movesLeft ->
            runOnUiThread {
                // Update ALL players' dots (not just mine)
                updatePlayerDot(playerId, position)

                // Only update local room tracking for THIS player
                if (playerId == ClientState.playerId) {
                    val parts = position.split(",")
                    if (parts.size == 2) {
                        val col = parts[0].trim().toIntOrNull() ?: -1
                        val row = parts[1].trim().toIntOrNull() ?: -1
                        val room = BoardConfig.getRoomAtDoor(col, row)

                        if (room != null) {
                            // Problem 3 Fix: Show dialog to enter room on door field
                            val dialogView =
                                layoutInflater.inflate(R.layout.dialog_enter_room, null)
                            dialogView.findViewById<TextView>(R.id.tvTitle).text = "ENTER $room?"
                            dialogView.findViewById<TextView>(R.id.tvMessage).text =
                                "DO YOU WANT TO ENTER THE $room?"

                            val customDialog = android.app.AlertDialog.Builder(this)
                                .setView(dialogView)
                                .setCancelable(false)
                                .create()

                            dialogView.findViewById<Button>(R.id.btnYes).setOnClickListener {
                                MyStomp.instance.enterRoom(room)
                                customDialog.dismiss()
                            }
                            dialogView.findViewById<Button>(R.id.btnNo).setOnClickListener {
                                if (movesLeft == 0) MyStomp.instance.endTurn()
                                customDialog.dismiss()
                            }

                            customDialog.show()
                        }
                    }
                }
                updateButtonStates()
                updateAllPlayerStatuses()
            }
        }

        GameHandler.onEndTurn = { newIndex ->
            runOnUiThread {
                ClientState.currentPlayerIndex = newIndex
                hiddenWayUsed = false
                currentRoomId = null
                updateCurrentPlayerHighlight()
                updateButtonStates()
                updateAllPlayerStatuses()

                // Only show toast, do NOT trigger "your turn" UI for other players
                val currentPlayer = ClientState.players.getOrNull(newIndex)
                val msg = if (currentPlayer?.playerId == ClientState.playerId)
                    getString(R.string.your_turn) else getString(
                    R.string.turn_other,
                    currentPlayer?.character ?: "Player ${newIndex + 1}"
                )
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        }

        GameHandler.onEnterRoom = { playerId, roomId ->
            runOnUiThread {
                // Only update local room for THIS player
                if (playerId == ClientState.playerId) {
                    currentRoomId = roomId
                    ClientState.currentPhase = "IN_ROOM"
                    ClientState.remainingMoves = 0
                }

                updatePlayerDot(playerId, roomId)
                updateCurrentPlayerHighlight()
                updateButtonStates()
                updateAllPlayerStatuses()
            }
        }

        GameHandler.onHiddenWay = { playerId, targetRoom ->
            runOnUiThread {
                // Only update local state for THIS player
                if (playerId == ClientState.playerId) {
                    currentRoomId = targetRoom
                    hiddenWayUsed = true
                }
                updatePlayerDot(playerId, targetRoom)
                updateButtonStates()
                updateAllPlayerStatuses()
            }
        }

        GameHandler.onSuggestionResult = { suggesterID, suspect, room, weapon, matchingCards ->
            runOnUiThread {
                dismissCheatOverlays()
                if (suggesterID == ClientState.playerId) {
                    if (matchingCards.isNotEmpty()) {
                        GameUIHelper.showResultCards(this, rootLayout, matchingCards)
                        updateChecklist()
                    } else {
                        Toast.makeText(
                            this,
                            getString(
                                R.string.suggestion_made,
                                playerDisplayName(suggesterID)
                            ),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    showCheatDecisionOverlay()
                }
            }
        }

        GameHandler.onSuggestionRequest = { suggesterID, suspect, room, weapon, cheatWindowSeconds, matchingCards ->
            runOnUiThread {
                if (suggesterID != ClientState.playerId && !ClientState.cheatUsed && !ClientState.isEliminated) {
                    showCheatWindow(suggesterID, suspect, room, weapon, cheatWindowSeconds)
                }
            }

        GameHandler.onCheatResult = { cheatDetected, cheaters, revealedCard ->
            runOnUiThread {
                dismissCheatOverlays()

                if (cheatDetected) {
                    if (cheaters.any { it.first == ClientState.playerId }) {
                        ClientState.cheatUsed = true
                    }
                }

                if (cheatDetected) {
                    val allCards = cheaters.flatMap { it.second }
                    val msg = "Cheat detected! Revealed cards: ${allCards.joinToString(", ")}"
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                    if (allCards.isNotEmpty()) {
                        GameUIHelper.showResultCards(this, rootLayout, allCards, 5000)
                    }
                } else {
                    val msg = if (revealedCard != null)
                        "No cheat detected. One of your cards was revealed: $revealedCard"
                    else
                        "No cheat detected."
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                }
                updateCurrentPlayerHighlight()
                updateButtonStates()
                updateAllPlayerStatuses()
            }
        }

        GameHandler.onAccusation = { accuserID, suspect, room, weapon, correct, eliminated ->
            runOnUiThread {
                // All players see the accusation cards
                GameUIHelper.showResultCards(this, rootLayout, listOf(suspect, weapon, room), 3000)
                if (correct) {
                    bgMusic?.stop()
                    bgMusic?.release()
                    bgMusic = null
                    playSound(R.raw.win_sound)
                    val msg =
                        if (accuserID == ClientState.playerId) getString(R.string.you_won) else getString(
                            R.string.player_won,
                            playerDisplayName(accuserID)
                        )
                    android.os.Handler(mainLooper).postDelayed({
                        GameUIHelper.showGameEndOverlay(this, rootLayout, msg)
                    }, 3500)
                } else if (eliminated) {
                    playSound(R.raw.player_eliminated_sound)
                    // Only show elimination message, differentiate by playerId
                    if (accuserID == ClientState.playerId) {
                        Toast.makeText(
                            this,
                            getString(R.string.wrong_accusation),
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            this,
                            getString(
                                R.string.player_eliminated,
                                playerDisplayName(accuserID)
                            ),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

        GameHandler.onGameFinished = { winner ->
            runOnUiThread {
                bgMusic?.stop()
                bgMusic?.release()
                bgMusic = null
                playSound(R.raw.win_sound)
                val msg =
                    if (winner == ClientState.playerId) getString(R.string.you_won) else getString(
                        R.string.player_won,
                        playerDisplayName(winner)
                    )
                GameUIHelper.showGameEndOverlay(this, rootLayout, msg)
            }
        }

        GameHandler.onGamePaused = { disconnectedId, countdown ->
            runOnUiThread {
                showPauseOverlay(disconnectedId, countdown)
                bgMusic?.pause()
                val leavePlayer = MediaPlayer.create(this, R.raw.ingame_leave_sound)
                leavePlayer?.setOnCompletionListener {
                    it.release()
                    waitingMusic = MediaPlayer.create(this, R.raw.waiting_for_rejoin)
                    waitingMusic?.isLooping = true
                    waitingMusic?.start()
                }
                leavePlayer?.start()
            }
        }

        GameHandler.onContinueGame = { rejoinedId ->
            runOnUiThread {
                dismissPauseOverlay()
                stopWaitingMusic()
                playSound(R.raw.player_returned_sound)
                bgMusic?.start()
                Toast.makeText(this,
                    "Player rejoined! Game resumed.",
                    Toast.LENGTH_SHORT).show()
                updateAllPlayerStatuses()
                updateCurrentPlayerHighlight()
                updateButtonStates()
            }
        }

        GameHandler.onGameAborted = { reason ->
            runOnUiThread {
                stopWaitingMusic()
                bgMusic?.stop()
                bgMusic?.release()
                bgMusic = null
                playSound(R.raw.game_over_sound)
                val displayReason = replacePlayerIdsWithNames(reason)
                GameUIHelper.showGameEndOverlay(this, rootLayout, getString(R.string.game_over, displayReason))
                android.os.Handler(mainLooper).postDelayed({
                    val intent = Intent(this, LobbyActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                    finish()
                }, 3000)
            }
        }

        GameHandler.onGameError = { reason ->
            runOnUiThread {
                Toast.makeText(this, reason, Toast.LENGTH_SHORT).show()
            }
        }
    }
/*
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        super.onBackPressed()
        onLeaveGame()
    }*/

    override fun onDestroy() {
        bgDisconnectHandler.removeCallbacks(bgDisconnectRunnable)
        stopDisconnectService()
        bgMusic?.stop()
        bgMusic?.release()
        bgMusic = null
        stopWaitingMusic()
        dismissPauseOverlay()
        dismissCheatOverlays()
        GameHandler.onRollDice = null
        GameHandler.onMove = null
        GameHandler.onEndTurn = null
        GameHandler.onEnterRoom = null
        GameHandler.onHiddenWay = null
        GameHandler.onAccusation = null
        GameHandler.onSuggestionResult = null
        GameHandler.onSuggestionRequest = null
        GameHandler.onCheatResult = null
        GameHandler.onGameFinished = null
        GameHandler.onGameAborted = null
        GameHandler.onGamePaused = null
        GameHandler.onContinueGame = null
        GameHandler.onGameError = null
        super.onDestroy()
    }

    private fun updatePlayerDot(playerId: String, position: String) {
        if (!boardSetupDone) return
        val gridW = gridOverlay.width
        val gridH = gridOverlay.height
        if (gridW == 0 || gridH == 0) return

        val cellW = gridW / BoardConfig.COLS
        val cellH = gridH / BoardConfig.ROWS

        // Remove old dot from wherever it is
        playerDots[playerId]?.let {
            (it.parent as? ViewGroup)?.removeView(it)
        }

        val charType = ClientState.playerCharacterMap[playerId]
            ?: ClientState.players.find { it.playerId == playerId }?.character
        val color = BoardColors.CHARACTER_COLORS[charType] ?: Color.GRAY
        val dot = GameUIHelper.createPlayerDot(this, color)
        val dotSize = GameUIHelper.dpToPx(this, 16)

        if (position.contains(",")) {
            val parts = position.split(",")
            val col = parts[0].trim().toIntOrNull() ?: 0
            val row = parts[1].trim().toIntOrNull() ?: 0
            val dlp =
                androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(dotSize, dotSize)
                    .apply {
                        startToStart = androidx.constraintlayout.widget.ConstraintSet.PARENT_ID
                        topToTop = androidx.constraintlayout.widget.ConstraintSet.PARENT_ID
                        leftMargin = col * cellW + (cellW - dotSize) / 2
                        topMargin = row * cellH + (cellH - dotSize) / 2
                    }
            dot.layoutParams = dlp
            gridOverlay.addView(dot)
        } else {
            // Problem 4 Fix: Room position using roomOverlay
            val roomOverlay = findViewById<ViewGroup>(R.id.roomOverlay) ?: return

            val playersInRoom = ClientState.playerPositions.filter { it.value == position }
            val slotIndex = playersInRoom.keys.toList().indexOf(playerId).coerceIn(0, 3)

            val percent = BoardConfig.ROOM_CENTERS_PERCENT[position] ?: Pair(0.5f, 0.5f)
            val centerX = (roomOverlay.width * percent.first).toInt()
            val centerY = (roomOverlay.height * percent.second).toInt()

            val offsetX = (slotIndex % 2) * dotSize
            val offsetY = (slotIndex / 2) * dotSize

            val dlp =
                androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(dotSize, dotSize)
                    .apply {
                        startToStart = androidx.constraintlayout.widget.ConstraintSet.PARENT_ID
                        topToTop = androidx.constraintlayout.widget.ConstraintSet.PARENT_ID
                        leftMargin = centerX + offsetX - dotSize / 2
                        topMargin = centerY + offsetY - dotSize / 2
                    }
            dot.layoutParams = dlp
            roomOverlay.addView(dot)
        }

        playerDots[playerId] = dot
    }

    private fun placeAllPlayerDots() {
        for ((playerId, position) in ClientState.playerPositions) {
            updatePlayerDot(playerId, position)
        }
    }

    private fun updateChecklist() {
        checklistOverlay.post {
            GameUIHelper.buildChecklistOverlay(
                this, checklistOverlay,
                checklistOverlay.width, checklistOverlay.height
            )
        }
    }

    private fun updateCurrentPlayerHighlight() {
        val players = ClientState.players
        val currentPid = if (players.isNotEmpty() && ClientState.currentPlayerIndex < players.size)
            players[ClientState.currentPlayerIndex].playerId else ""

        characterHighlights.forEach { (pid, view) ->
            view.visibility = if (pid == currentPid) View.VISIBLE else View.GONE
        }
    }

    private fun updateButtonStates() {
        val myTurn = isMyTurn() && !ClientState.isEliminated
        val phase = ClientState.currentPhase
        val myPos = ClientState.playerPositions[ClientState.playerId] ?: ""
        val inRoom = BoardConfig.ROOM_CENTERS_PERCENT.containsKey(myPos)
        val hasHiddenPassage = BoardConfig.HIDDEN_PASSAGES.containsKey(myPos)

        setButtonActive(btnRollDice, myTurn && phase == "WAITING_FOR_ROLL")
        setButtonActive(
            btnHiddenWay,
            myTurn && hasHiddenPassage && (phase == "IN_ROOM" || phase == "WAITING_FOR_ROLL")
        )
        setButtonActive(
            btnSuggest,
            myTurn && inRoom && (phase == "IN_ROOM" || phase == "WAITING_FOR_ROLL")
        )
        setButtonActive(
            btnAccuse,
            myTurn && inRoom && (phase == "IN_ROOM" || phase == "WAITING_FOR_ROLL")
        )
        setButtonActive(btnLeave, true)
    }

    private fun setButtonActive(btn: Button, active: Boolean) {
        btn.alpha = if (active) 1.0f else 0.4f
        btn.isClickable = active
    }

    private fun playerDisplayName(playerId: String): String {
        return ClientState.playerCharacterMap[playerId]
            ?: ClientState.players.find { it.playerId == playerId }?.character
            ?: "Unknown Player"
    }

    private fun replacePlayerIdsWithNames(text: String): String {
        var result = text

        ClientState.players.forEach { player ->
            val name = playerDisplayName(player.playerId)
            result = result.replace(player.playerId, name)
        }

        ClientState.playerCharacterMap.forEach { (playerId, characterName) ->
            result = result.replace(playerId, characterName)
        }

        return result
    }

    private fun showPauseOverlay(disconnectedId: String, countdown: Int) {
        dismissPauseOverlay()

        val overlay = android.widget.FrameLayout(this).apply {
            setBackgroundColor(Color.argb(180, 0, 0, 0))
            isClickable = true // block touches to game underneath
        }
        val textView = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 20f
            gravity = android.view.Gravity.CENTER
        }
        overlay.addView(textView, android.widget.FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
            android.view.Gravity.CENTER
        ))
        rootLayout.addView(overlay, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ))
        pauseOverlay = overlay

        var remaining = countdown
        val handler = android.os.Handler(mainLooper)
        val runnable = object : Runnable {
            override fun run() {
                if (remaining > 0) {
                    textView.text = getString(R.string.player_disconnected_countdown,
                        playerDisplayName(disconnectedId), remaining)
                    remaining--
                    handler.postDelayed(this, 1000)
                }
            }
        }
        countdownHandler = handler
        countdownRunnable = runnable
        runnable.run()
    }

    private fun dismissPauseOverlay() {
        countdownRunnable?.let { countdownHandler?.removeCallbacks(it) }
        countdownRunnable = null
        countdownHandler = null
        pauseOverlay?.let { rootLayout.removeView(it) }
        pauseOverlay = null
    }

    private fun showCheatWindow(
        suggesterID: String,
        suspect: String,
        room: String,
        weapon: String,
        windowSeconds: Int
    ) {
        dismissCheatOverlays()

        val overlay = android.widget.FrameLayout(this).apply {
            setBackgroundColor(android.graphics.Color.argb(200, 0, 0, 0))
            isClickable = true
        }

        val inner = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setPadding(48, 48, 48, 48)
        }

        val tvInfo = TextView(this).apply {
            text = "${playerDisplayName(suggesterID)} suggests:\n$suspect, $room, $weapon"
            setTextColor(android.graphics.Color.WHITE)
            textSize = 14f
            gravity = android.view.Gravity.CENTER
        }

        val tvCountdown = TextView(this).apply {
            text = "Cheat window: ${windowSeconds}s"
            setTextColor(android.graphics.Color.YELLOW)
            textSize = 16f
            gravity = android.view.Gravity.CENTER
            setPadding(0, 16, 0, 16)
        }

        val tvShake = TextView(this).apply {
            text = if (ClientState.cheatUsed) "Cheat already used!" else "Shake to cheat!"
            setTextColor(if (ClientState.cheatUsed) android.graphics.Color.GRAY else android.graphics.Color.GREEN)
            textSize = 16f
            gravity = android.view.Gravity.CENTER
            setPadding(0, 16, 0, 16)
        }

        if (!ClientState.cheatUsed) {
            sensorManager = getSystemService(SENSOR_SERVICE) as android.hardware.SensorManager
            shakeDetector = ShakeDetector {
                runOnUiThread {
                    MyStomp.instance.sendCheatAttempt()
                    tvShake.text = "Cheat sent!"
                    tvShake.setTextColor(android.graphics.Color.GRAY)
                    stopShakeDetector()
                }
            }
            val accelerometer = sensorManager?.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER)
            sensorManager?.registerListener(shakeDetector, accelerometer, android.hardware.SensorManager.SENSOR_DELAY_UI)
        }

        inner.addView(tvInfo)
        inner.addView(tvCountdown)
        inner.addView(tvShake)

        overlay.addView(inner, android.widget.FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            android.view.Gravity.CENTER
        ))
        rootLayout.addView(overlay, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ))
        cheatWindowOverlay = overlay

        var remaining = windowSeconds
        cheatWindowHandler = android.os.Handler(mainLooper)
        cheatWindowRunnable = object : Runnable {
            override fun run() {
                if (remaining > 0) {
                    tvCountdown.text = "CHEAT WINDOW: ${remaining}s"
                    remaining--
                    cheatWindowHandler?.postDelayed(this, 1000)
                } else {
                    dismissCheatOverlays()
                }
            }
        }
        cheatWindowHandler?.post(cheatWindowRunnable!!)
    }

    private fun showCheatDecisionOverlay() {
        dismissCheatOverlays()

        val freckleFace = try {
            ResourcesCompat.getFont(this, R.font.freckle_face)
                ?: android.graphics.Typeface.DEFAULT
        } catch (e: Exception) {
            android.graphics.Typeface.DEFAULT
        }
        val cluedoPink = android.graphics.Color.parseColor("#F50057")

        val overlay = android.widget.FrameLayout(this).apply {
            setBackgroundColor(android.graphics.Color.argb(200, 20, 20, 20))
            isClickable = true
        }

        val inner = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setPadding(48, 48, 48, 48)
        }

        val tvInfo = TextView(this).apply {
            text = "DID SOMEONE CHEAT?"
            setTextColor(cluedoPink)
            textSize = 18f
            typeface = freckleFace
            gravity = android.view.Gravity.CENTER
            setPadding(0, 0, 0, 24)
        }

        val tvCountdown = TextView(this).apply {
            text = "YOU HAVE 5 SECONDS TO DECIDE..."
            setTextColor(android.graphics.Color.WHITE)
            textSize = 14f
            typeface = freckleFace
            gravity = android.view.Gravity.CENTER
            setPadding(0, 0, 0, 16)
        }

        val btnYes = Button(this).apply {
            text = "YES, SOMEONE CHEATED!"
            setBackgroundColor(cluedoPink)
            setTextColor(android.graphics.Color.WHITE)
            typeface = freckleFace
        }

        val btnNo = Button(this).apply {
            text = "NOBODY CHEATED!"
            setBackgroundColor(android.graphics.Color.WHITE)
            setTextColor(cluedoPink)
            typeface = freckleFace
        }

        val sendDecision = { pressed: Boolean ->
            MyStomp.instance.sendCheatButtonPressed(pressed)
            dismissCheatOverlays()
        }

        btnYes.setOnClickListener { sendDecision(true) }
        btnNo.setOnClickListener { sendDecision(false) }

        inner.addView(tvInfo)
        inner.addView(tvCountdown)
        inner.addView(btnYes)
        inner.addView(btnNo)
        overlay.addView(inner, android.widget.FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            android.view.Gravity.CENTER
        ))
        rootLayout.addView(overlay, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ))
        cheatDecisionOverlay = overlay

        cheatDecisionHandler = android.os.Handler(mainLooper)
        cheatDecisionRunnable = Runnable {
            if (cheatDecisionOverlay != null) {
                sendDecision(false)
            }
        }
        cheatDecisionHandler?.postDelayed(cheatDecisionRunnable!!, 3000)
    }

    private fun dismissCheatOverlays() {
        stopShakeDetector()

        cheatWindowRunnable?.let { cheatWindowHandler?.removeCallbacks(it) }
        cheatWindowRunnable = null
        cheatWindowHandler = null

        cheatDecisionRunnable?.let { cheatDecisionHandler?.removeCallbacks(it) }
        cheatDecisionRunnable = null
        cheatDecisionHandler = null

        cheatWindowOverlay?.let {
            if (it.parent != null) {
                rootLayout.removeView(it)
            }
        }
        cheatWindowOverlay = null

        cheatDecisionOverlay?.let {
            if (it.parent != null) {
                rootLayout.removeView(it)
            }
        }
        cheatDecisionOverlay = null
    }

    private fun stopShakeDetector() {
        sensorManager?.unregisterListener(shakeDetector)
        sensorManager = null
        shakeDetector = null
    }

    private fun updateAllPlayerStatuses() {
        val players = ClientState.players
        val currentPid = players.getOrNull(ClientState.currentPlayerIndex)?.playerId ?: ""

        for (player in players) {
            val tv = playerStatusViews[player.playerId] ?: continue

            tv.text = when {
                ClientState.eliminatedPlayers.contains(player.playerId) ->
                    getString(R.string.status_eliminated)

                player.playerId == currentPid -> {
                    val moves = ClientState.remainingMoves
                    when (ClientState.currentPhase) {
                        "WAITING_FOR_ROLL" -> getString(R.string.status_roll)
                        "WAITING_FOR_MOVE" -> getString(R.string.status_moving, moves)
                        "IN_ROOM" -> getString(R.string.status_in_room)
                        "WAITING_FOR_SUGGESTION_RESPONSE" -> getString(R.string.status_suggestion)
                        "WAITING_FOR_LIAR_DECISION" -> getString(R.string.status_liar)
                        "TURN_ENDED" -> getString(R.string.status_waiting)
                        else -> ""
                    }
                }

                else -> getString(R.string.status_waiting)
            }
        }
    }
}