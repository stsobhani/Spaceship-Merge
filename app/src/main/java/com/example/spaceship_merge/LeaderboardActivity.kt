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

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query


// The activity which displays the top ten highest scores using firebase!
class LeaderboardActivity : AppCompatActivity() {

    companion object {
        // the number of leaderboard positions
        private const val NUM_SLOTS = 10
        private const val TAG = "LeaderboardActivity"
    }
    // holds all the leaderboard rows
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

        // Firestore instance
        val db = FirebaseFirestore.getInstance()
        db.collection("leaderboard")
            .orderBy("score", Query.Direction.DESCENDING)
            .limit(NUM_SLOTS.toLong())
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    Log.w(TAG, "Failed to read leaderboard", error)
                    return@addSnapshotListener
                }

                // Clear rows first (handles empty leaderboard)
                for (i in 0 until NUM_SLOTS) {
                    rowViews[i].text = formatRow(i + 1, "***", 0)
                }

                if (snapshot == null) return@addSnapshotListener

                for ((index, doc) in snapshot.documents.withIndex()) {
                    val username = doc.getString("username") ?: "***"
                    val score = doc.getLong("score")?.toInt() ?: 0

                    rowViews[index].text = formatRow(index + 1, username, score)
                }
            }

    }
    // Creates the ten textviews to display leaderboard entries!
    private fun createRows() {
        // Sets the layout parameters
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
