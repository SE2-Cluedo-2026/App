package at.aau.serg.websocketbrokerdemo

import android.os.Bundle
import android.widget.Button
import androidx.activity.ComponentActivity
import com.example.myapplication.R
import android.widget.ImageView

class LearnActivity : ComponentActivity() {

    private var page = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_learn)

        val imgLearnPage = findViewById<ImageView>(R.id.imgLearnPage)
        val btnNext = findViewById<Button>(R.id.btnNext)
        val btnBack = findViewById<Button>(R.id.btnBack)
        val btnPrevious = findViewById<Button>(R.id.btnPrevious)

        btnPrevious.setOnClickListener {
            if (page == 2) {
                imgLearnPage.setImageResource(R.drawable.learnpage)
                page = 1
            }
        }

        btnNext.setOnClickListener {
            if (page == 1) {
                imgLearnPage.setImageResource(R.drawable.learnpage2)
                page = 2
            }
        }
        btnBack.setOnClickListener {
                finish()
        }
    }
}