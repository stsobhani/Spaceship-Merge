package com.example.spaceship_merge

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import android.graphics.drawable.GradientDrawable

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
// The activity which displays the top ten highest scores using firebase!
class LeaderboardActivity : AppCompatActivity() {

    companion object {
        // the number of leaderboard positions
        private const val NUM_SLOTS = 10
        private const val TAG = "LeaderboardActivity"
    }
    // holds all the leadarboard rows
    private lateinit var leaderboardContainer: LinearLayout
    // Lists the TextViews for each leaderboard position
    private val rowViews = mutableListOf<TextView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leaderboard)

        // The back button logic
        val backButton =
            findViewById<com.google.android.material.button.MaterialButton>(R.id.back_button)
        backButton.setOnClickListener {
            finish()
        }

        leaderboardContainer = findViewById(R.id.leaderboard_container)

        // Creates the ten boxes
        createRows()

        // Learned in class, how to use the firebase!
        val db = FirebaseDatabase.getInstance()
        val leaderboardRef = db.getReference("leaderboard")
        // Adds the listner which updates the UI whenever data changes
        leaderboardRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Updates the leaderboard position
                for (i in 1..NUM_SLOTS) {
                    // Reads the username and score
                    val username = snapshot.child("username$i")
                        .getValue(String::class.java) ?: "***"
                    val score = snapshot.child("score$i")
                        .getValue(Int::class.java) ?: 0
                    // This updates the TextView with the formatted text!
                    rowViews[i - 1].text = formatRow(i, username, score)
                }
            }
            // Used to help debug:)
            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Failed to read leaderboard: ${error.message}")
            }
        })
    }
    // Creates the ten textviews to display leaderboard entries!
    private fun createRows() {
        // Sets the layour parameters
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = dpToPx(8)
            leftMargin = dpToPx(24)
            rightMargin = dpToPx(24)
        }

        // Creates the ten rows
        for (i in 1..NUM_SLOTS) {
            val rowTextView = TextView(this).apply {
                // This initializes the placeholder
                text = formatRow(i, "***", 0)
                textSize = 16f
                setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12))
                gravity = Gravity.CENTER_VERTICAL
                setTextColor(ContextCompat.getColor(this@LeaderboardActivity, R.color.white))

                // Uses earlier font
                typeface = ResourcesCompat.getFont(this@LeaderboardActivity, R.font.space_grotesk_medium)

                // Creates the rectangle background
                background = GradientDrawable().apply {
                    setColor(ContextCompat.getColor(this@LeaderboardActivity, R.color.turquoise))

                    // Makes the corners rounded
                    cornerRadius = dpToPx(16).toFloat()
                    // Makes red border to look nice!!
                    setStroke(dpToPx(2), ContextCompat.getColor(this@LeaderboardActivity, R.color.red))
                }
            }
            // Adds row to container
            leaderboardContainer.addView(rowTextView, layoutParams)
            rowViews.add(rowTextView)
        }
    }
    // Formats the leaderboard entry
    private fun formatRow(rank: Int, username: String, score: Int): String {
        return "$rank. $username - $score"
    }
    // Converts the dp to pixels
    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
