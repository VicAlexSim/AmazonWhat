package com.example.amazonwhat

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MenuActivity : AppCompatActivity() {

    private lateinit var appTitle: TextView
    private lateinit var scoreboard: TextView
    private lateinit var btnStart: Button

    private var highscore = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.menu)  // ← using manu.xml here

        // Bind views
        appTitle = findViewById(R.id.app_title)
        scoreboard = findViewById(R.id.scoreboard)
        btnStart = findViewById(R.id.btnStart)

        // Get values from MainActivity
        val finalScore = intent.getIntExtra("finalScore", 0)
        val isStart = intent.getBooleanExtra("isStart", true)

        // Update UI
        if (isStart) {
            btnStart.text = "Start"
        } else {
            btnStart.text = "Restart"
            highscore = if (finalScore > highscore) finalScore else highscore
            scoreboard.text = "Highscore: $highscore"
        }

        // Start game
        btnStart.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }
}
