package at.aau.serg.websocketbrokerdemo

import MyStomp
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import at.aau.serg.websocketbrokerdemo.network.lobby.LobbyListener
import com.example.myapplication.R

class MainActivity : ComponentActivity(), Callbacks {
    lateinit var myStomp: MyStomp
    lateinit var response: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        myStomp = MyStomp(this)

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.fragment_fullscreen)

        findViewById<Button>(R.id.connectbtn).setOnClickListener { myStomp.connect() }
        findViewById<Button>(R.id.hellobtn).setOnClickListener { myStomp.sendHello() }
        findViewById<Button>(R.id.jsonbtn).setOnClickListener { myStomp.sendJson() }
        findViewById<Button>(R.id.joinlobbybtn).setOnClickListener {
            myStomp.joinLobby("Test", "Test")
        }
        response = findViewById(R.id.response_view)
    }

    override fun onResponse(res: String) {
        response.text = res
    }

    override fun onJoinSuccess(message: String) {
        response.text = message
    }

    override fun onPlayersReceived(players: List<String>) {
        response.text = players.joinToString(", ")
    }
}

