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

        // Instantiate switch
        stickyModeSwitch = findViewById(R.id.sticky_mode_switch)

        // Make sticky mode off by default
        stickyModeSwitch.isChecked = false

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
        val adUnitId: String = "ca-app-pub-3940256099942544/6300978111"

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

            val stickyMode = stickyModeSwitch.isChecked
            val intent = Intent(this, GameActivity::class.java)
            intent.putExtra("sticky_mode_enabled", stickyMode)
            startActivity(intent)
        }

        // Display tutorial pop-up
        tutorialButton.setOnClickListener {
            Log.d("MainActivity", "Tutorial button clicked")

            // Build content view for the popup
            val context = this

            // Outer container (for red border)
            val outerLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(8, 8, 8, 8) // border thickness

                background = GradientDrawable().apply {
                    // Red border container
                    setColor(ContextCompat.getColor(context, R.color.red))
                    cornerRadius = 40f
                }
            }

            // Inner layout (turquoise background)
            val innerLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(32, 32, 32, 32)

                background = GradientDrawable().apply {
                    setColor(ContextCompat.getColor(context, R.color.turquoise))
                    cornerRadius = 32f
                }
            }

            // Tutorial text
            val tutorialTextView = TextView(context).apply {
                text = "HOW TO PLAY\n\n" +
                        "• Tap to shoot a ship\n" +
                        "• When two same-level ships touch, they merge\n" +
                        "• Merging creates a higher-level ship\n" +
                        "• Try to build the biggest ship\n" +
                        "• Don’t let the board get too full!"
                setTextColor(ContextCompat.getColor(context, R.color.white))
                textSize = 14f
                // If you want your retro font:
                // typeface = ResourcesCompat.getFont(context, R.font.press_start_2p)
            }

            innerLayout.addView(
                tutorialTextView,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )

            outerLayout.addView(
                innerLayout,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )

            // Create the pop-up
            val popupWidthDp = 300
            val popupWidthPx = (popupWidthDp * resources.displayMetrics.density).toInt()

            val popupWindow = PopupWindow(
                outerLayout,
                popupWidthPx,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true  // focusable: allows back button / outside touch to dismiss
            )

            // Needed so outside touches dismiss the popup
            popupWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            popupWindow.isOutsideTouchable = true

            // Log the pop-up
            popupWindow.setOnDismissListener {
                Log.d("MainActivity", "Tutorial popup dismissed")
            }

            // Position the pop-up
            val rootView = findViewById<View>(R.id.home_root)
            popupWindow.showAtLocation(rootView, Gravity.CENTER, 0, -510)

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