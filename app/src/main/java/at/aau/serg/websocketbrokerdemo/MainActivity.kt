package at.aau.serg.websocketbrokerdemo

import MyStomp
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import com.example.myapplication.R
import android.widget.Button;
import android.content.Intent
import android.util.Log
import at.aau.serg.websocketbrokerdemo.model.ClientState
import at.aau.serg.websocketbrokerdemo.network.lobby.LobbyHandler
class MainActivity : ComponentActivity(), Callbacks {
    private lateinit var myStomp: MyStomp



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val playerId = UserPreferences.getOrCreatePlayerId(this)
        ClientState.playerId = playerId

        enableEdgeToEdge()

        setContentView(R.layout.cluedo_fragment_fullscreen)

        val btnLearn = findViewById<Button>(R.id.btnLearn)
        btnLearn.setOnClickListener {
            val intent = Intent(this, LearnActivity::class.java)
            startActivity(intent)
        }

        val btnStart = findViewById<Button>(R.id.btnStart)
        btnStart.setOnClickListener {
            findViewById<android.widget.FrameLayout>(R.id.loadingOverlay).visibility = android.view.View.VISIBLE
            myStomp.connect()
        }
    }

    override fun onStart() {
        super.onStart()

        // Re-create MyStomp so we get a clean session on every (re-)visit
        myStomp = MyStomp(this)

        val loadingOverlay = findViewById<android.widget.FrameLayout>(R.id.loadingOverlay)
        loadingOverlay.visibility = android.view.View.GONE

        LobbyHandler.onLobbyJoined = {
            runOnUiThread {
                loadingOverlay.visibility = android.view.View.GONE
                startActivity(Intent(this, LobbyActivity::class.java))
            }
        }

        LobbyHandler.onPlayerRejoined = { dto ->
            runOnUiThread {
                if (ClientState.gameStatus == "RUNNING") {
                    startActivity(Intent(this, GameActivity::class.java))
                } else {
                    startActivity(Intent(this, LobbyActivity::class.java))
                }
            }
        }
        LobbyHandler.onPlayerRejoinedRunning = {
            runOnUiThread {
                loadingOverlay.visibility = android.view.View.GONE
                startActivity(Intent(this, GameActivity::class.java))
            }
        }

        LobbyHandler.onGameFull = { dto ->
            runOnUiThread {
                findViewById<android.widget.FrameLayout>(R.id.loadingOverlay).visibility =
                    android.view.View.GONE

                android.widget.Toast.makeText(
                    this,
                    dto.message,
                    android.widget.Toast.LENGTH_LONG
                ).show()


                android.os.Handler(mainLooper).postDelayed({
                    MyStomp.instance.disconnect()
                }, 500)
            }
        }
    }

    override fun onDestroy() {
        LobbyHandler.onLobbyJoined = null
        LobbyHandler.onPlayerRejoined = null
        LobbyHandler.onPlayerRejoinedRunning = null
        LobbyHandler.onGameFull = null
        if (::myStomp.isInitialized) {
            myStomp.disconnect()
        }
        super.onDestroy()
    }

    override fun onResponse(res: String) {
        Log.d("MainActivity", "Response: $res")
    }

    override fun onConnected() {
        // Handled via LobbyHandler.onLobbyJoined callback
    }
}