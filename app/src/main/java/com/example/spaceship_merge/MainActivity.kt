package com.example.spaceship_merge

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
import android.widget.Switch
import androidx.appcompat.app.AlertDialog
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat

class MainActivity : AppCompatActivity() {

    // late init vars for our buttons and ad view
    private lateinit var leaderboardButton: MaterialButton
    private lateinit var playButton: MaterialButton
    private lateinit var tutorialButton : MaterialButton
    private lateinit var usernameText: TextView
    private lateinit var highScoreText: TextView
    private lateinit var stickyModeSwitch: Switch
    private lateinit var adView: AdView

    companion object {
        // Shared preferences
        const val PREFS_NAME = "prefs"
        const val USERNAME_KEY = "username"
        const val HIGH_SCORE_KEY = "high_score"
        // the max length for username
        private const val USERNAME_MAX_LEN = 15
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize google mobile ads!!
        MobileAds.initialize(this) {}

        // Instantiate buttons
        leaderboardButton = findViewById(R.id.leaderboard_button)
        playButton = findViewById(R.id.play_button)
        tutorialButton = findViewById(R.id.tutorial_button)

        // Instantiate text views
        usernameText = findViewById(R.id.username_text)
        highScoreText = findViewById(R.id.high_score_text)

        // Instantiate switch
        stickyModeSwitch = findViewById(R.id.sticky_mode_switch)

        // Makes sticky mode off by default
        stickyModeSwitch.isChecked = false
        var stickyMode = stickyModeSwitch.isChecked

        // Get shared prefs
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        // Set the high score text
        val savedHighScore = prefs.getInt(HIGH_SCORE_KEY, 0)
        val savedHighScoreString = savedHighScore.toString()
        highScoreText.text = "Current High Score: $savedHighScoreString"

        // create username if needed, set username text
        val savedUsername = prefs.getString("username", null)
        if (savedUsername.isNullOrBlank()) {
            // Forces user to make!
            createUsername(prefs)
        } else {
            usernameText.text = "Username: $savedUsername"
        }

        // Setsup the banner ad
        val adContainer = findViewById<FrameLayout>(R.id.ad_container)

        // The test ad!!
        val adUnitId: String = "ca-app-pub-3940256099942544/6300978111"

        // Create adView
        adView = AdView(this)
        adView.setAdSize(AdSize.BANNER)
        adView.adUnitId = adUnitId
        adContainer.addView(adView)

        // Loads the advertisement
        val adRequest: AdRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)

        // Switches to leaderboard screen
        leaderboardButton.setOnClickListener {
            Log.w("MainActivity", "Leaderboard button clicked")

            val intent = Intent(this, LeaderboardActivity::class.java)
            startActivity(intent)
        }
        // Switches to the game
        playButton.setOnClickListener {
            Log.w("MainActivity", "Play button clicked")

            val intent = Intent(this, GameActivity::class.java)
            intent.putExtra("sticky_mode_enabled", stickyMode)
            startActivity(intent)
        }

        // sticky mode listener
        stickyModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            stickyMode = isChecked
        }

        // display tutorial popup
        tutorialButton.setOnClickListener {
            Log.w("MainActivity", "Tutorial button clicked")

            // Build content view for the popup
            val context = this

            // makes the red broder
            val outerLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(8, 8, 8, 8)

                background = GradientDrawable().apply {
                    // sets the container!
                    setColor(ContextCompat.getColor(context, R.color.red))
                    cornerRadius = 40f
                }
            }

            // creates the inner layout for the content area
            val innerLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(32, 32, 32, 32)

                background = GradientDrawable().apply {
                    setColor(ContextCompat.getColor(context, R.color.turquoise))
                    cornerRadius = 32f
                }
            }

            // The tutorial text
            val tutorialTextView = TextView(context).apply {
                text = "HOW TO PLAY\n\n" +
                        "• Tap to shoot a ship\n" +
                        "• When two same-level ships touch, they merge\n" +
                        "• Merging creates a higher-level ship\n" +
                        "• Try to build the biggest ship\n" +
                        "• Don’t let the board get too full!"
                setTextColor(ContextCompat.getColor(context, R.color.white))
                textSize = 14f
            }
            innerLayout.addView(
                tutorialTextView,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )
            // Adds the inner to outer layout!!
            outerLayout.addView(
                innerLayout,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )
            // Create the popup window
            val popupWidthDp = 300
            val popupWidthPx = (popupWidthDp * resources.displayMetrics.density).toInt()

            val popupWindow = PopupWindow(
                outerLayout,
                popupWidthPx,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
            )

            // Makes the touches work
            popupWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            popupWindow.isOutsideTouchable = true

            // Logs the popup
            popupWindow.setOnDismissListener {
                Log.w("MainActivity", "Tutorial popup dismissed")
            }
            // centers the popup
            val rootView = findViewById<View>(R.id.home_root)
            popupWindow.showAtLocation(rootView, Gravity.CENTER, 0, -510)
        }
    }
    // Shows the prompt to create the username!
    private fun createUsername(prefs: SharedPreferences) {

        // create edit text to enter username
        val input = EditText(this)
        input.hint = "Enter username (max $USERNAME_MAX_LEN characters!!)"

        // builds alert dialog
        val dialog = AlertDialog.Builder(this)
            .setTitle("Create Username")
            .setView(input)
            .setCancelable(false)
            .setPositiveButton("Save", null)
            .create()

        // set listener to our dialog
        dialog.setOnShowListener {
            // Sets listener to button on dialog
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {

                // Trims the entered name
                val name = input.text.toString().trim()

                // Checks if valid length
                if (name.isEmpty()) input.error = "Username cannot be empty"
                else if (name.length > USERNAME_MAX_LEN) input.error = "Must be $USERNAME_MAX_LEN characters or less"

                // Ssername is good!
                else {
                    // Saves the username to shared preferences
                    prefs.edit().putString(USERNAME_KEY, name).apply()
                    //Updates the text
                    usernameText.text = "Username: $name"
                    // Closes the dialog
                    dialog.dismiss()
                }
            }
        }
        // Shows the dialog
        dialog.show()
    }
}