package com.example.amazonwhat

import android.content.Context // Make sure this is imported
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast // Import Toast
import androidx.appcompat.app.AlertDialog // For confirmation dialog
import androidx.appcompat.app.AppCompatActivity

class MenuActivity : AppCompatActivity() {

    private lateinit var appTitle: TextView
    private lateinit var scoreboard: TextView
    private lateinit var btnStart: Button
    private lateinit var btnResetHighScore: Button // Declare the new button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.menu)

        // Bind views
        appTitle = findViewById(R.id.app_title)
        scoreboard = findViewById(R.id.scoreboard)
        btnStart = findViewById(R.id.btnStart)
        btnResetHighScore = findViewById(R.id.btnResetHighScore) // Bind the new button

        // Get values from MainActivity (if any, for restart behavior)
        val isStart = intent.getBooleanExtra("isStart", true)

        if (isStart) {
            btnStart.text = "Start"
        } else {
            btnStart.text = "Restart"
        }

        // Display current high score (done in onResume too)
        // displayHighScore() // Initial display is good, onResume handles updates

        // Start game
        btnStart.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        // Set OnClickListener for the Reset High Score button
        btnResetHighScore.setOnClickListener {
            showResetConfirmationDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        // Always refresh the high score when the activity becomes visible
        displayHighScore()
    }

    private fun displayHighScore() {
        val sharedPreferences = getSharedPreferences("GamePrefs", Context.MODE_PRIVATE)
        val highScoreKey = "amazonWhatHighScore"
        val highScore = sharedPreferences.getInt(highScoreKey, 0)
        scoreboard.text = "Highscore: $highScore"
    }

    private fun showResetConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Reset High Score")
            .setMessage("Are you sure you want to reset the high score to 0?")
            .setPositiveButton("Reset") { dialog, which ->
                resetHighScore()
            }
            .setNegativeButton("Cancel", null) // null listener just dismisses the dialog
            .show()
    }

    private fun resetHighScore() {
        val sharedPreferences = getSharedPreferences("GamePrefs", Context.MODE_PRIVATE)
        val highScoreKey = "amazonWhatHighScore"

        // Set the high score to 0
        sharedPreferences.edit().putInt(highScoreKey, 0).apply()

        // Update the displayed high score
        displayHighScore()

        // Optionally, show a confirmation message
        Toast.makeText(this, "High score has been reset.", Toast.LENGTH_SHORT).show()
    }
}
