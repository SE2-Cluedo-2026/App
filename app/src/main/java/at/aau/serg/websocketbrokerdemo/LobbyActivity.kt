package at.aau.serg.websocketbrokerdemo

import android.os.Bundle
import android.widget.Button
import androidx.activity.ComponentActivity
import com.example.myapplication.R

class LobbyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_lobby)

        val btnLeave = findViewById<Button>(R.id.btnLeave)

        btnLeave.setOnClickListener {
            finish()
        }


    }



}