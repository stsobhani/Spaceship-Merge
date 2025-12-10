package com.example.spaceship_merge

/* files changed: added fonts dir and several fonts, added space_background.png, edited gradle, manifest, colors, activity main, main activity */

// imports
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.AdView
import android.widget.FrameLayout
import com.google.android.material.button.MaterialButton
import android.widget.TextView
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import android.content.SharedPreferences

class MainActivity : AppCompatActivity() {

    // late init vars for our buttons and ad view
    private lateinit var leaderboardButton: MaterialButton
    private lateinit var playButton: MaterialButton
    private lateinit var tutorialButton : MaterialButton
    private lateinit var usernameText: TextView
    private lateinit var highScoreText: TextView
    private lateinit var adView: AdView

    companion object {
        private const val PREFS_NAME = "prefs"
        private const val USERNAME_KEY = "username"
        private const val HIGH_SCORE_KEY = "high_score"
        private const val USERNAME_MAX_LEN = 15
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize google mobile ads SDK
        MobileAds.initialize(this) {}

        // Instantiate buttons
        leaderboardButton = findViewById(R.id.leaderboard_button)
        playButton = findViewById(R.id.play_button)
        tutorialButton = findViewById(R.id.tutorial_button)

        // Instantiate text views
        usernameText = findViewById(R.id.username_text)
        highScoreText = findViewById(R.id.high_score_text)

        // Get shared prefs
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        // Set high score text
        val savedHighScore = prefs.getInt(HIGH_SCORE_KEY, 0)
        val savedHighScoreString = savedHighScore.toString()
        highScoreText.text = "Current High Score: $savedHighScoreString"

        // Create username if needed, set username text
        val savedUsername = prefs.getString("username", null)
        if (savedUsername.isNullOrBlank()) {
            // No username yet → force user to create one
            createUsername(prefs)
        } else {
            usernameText.text = "Username: $savedUsername"
        }

        // Get layout manager for add
        val adContainer = findViewById<FrameLayout>(R.id.ad_container)

        // Your test ad unit id
        var adUnitId: String = "ca-app-pub-3940256099942544/6300978111"

        // Create adView
        adView = AdView(this)
        adView.setAdSize(AdSize.BANNER)
        adView.adUnitId = adUnitId
        adContainer.addView(adView)

        // build/load the ad request
        val adRequest: AdRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)

        // Switch to leaderboard screen
        leaderboardButton.setOnClickListener {
            Log.d("MainActivity", "Leaderboard button clicked")

            // TODO: Launch leaderboard screen
            // startActivity(Intent(this, LeaderboardActivity::class.java))
        }

        // Switch to game screen
        playButton.setOnClickListener {
            Log.d("MainActivity", "Play button clicked")

            startActivity(Intent(this, GameActivity::class.java))
        }

        // Display tutorial pop-up
        tutorialButton.setOnClickListener {
            Log.d("MainActivity", "Tutorial button clicked")

            // TODO: Implement tutorial
            // startActivity(Intent(this, GameActivity::class.java))
        }
    }

    private fun createUsername(prefs: SharedPreferences) {

        // create edit text for user to enter their username
        val input = EditText(this)
        input.hint = "Enter username (max $USERNAME_MAX_LEN chars)"

        // build the alert dialog
        val dialog = AlertDialog.Builder(this)
            .setTitle("Create Username")
            .setView(input)
            .setCancelable(false)
            .setPositiveButton("Save", null)
            .create()

        // set listener to our dialog
        dialog.setOnShowListener {

            // set listener to the button on the dialog
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {

                // format input given
                val name = input.text.toString().trim()

                // check the input string to make sure it is 1-15 chars
                if (name.isEmpty()) input.error = "Username cannot be empty"
                else if (name.length > USERNAME_MAX_LEN) input.error = "Must be $USERNAME_MAX_LEN characters or less"

                // handle valid strings
                else {
                    // update prefs
                    prefs.edit().putString(USERNAME_KEY, name).apply()

                    // update username text
                    usernameText.text = "Username: $name"

                    // remove dialog
                    dialog.dismiss()
                }
            }
        }

        // show dialog
        dialog.show()
    }
}