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

class LeaderboardActivity : AppCompatActivity() {

    companion object {
        private const val NUM_SLOTS = 10
        private const val TAG = "LeaderboardActivity"
    }

    private lateinit var leaderboardContainer: LinearLayout
    private val rowViews = mutableListOf<TextView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leaderboard)

        // Back button logic
        val backButton = findViewById<com.google.android.material.button.MaterialButton>(R.id.back_button)
        backButton.setOnClickListener {
            finish()
        }

        leaderboardContainer = findViewById(R.id.leaderboard_container)

        // Create 10 boxes (turquoise background, white text, optional red border)
        createRows()

        // Firebase: /leaderboard/rank1, ..., /leaderboard/rank10
        val db = FirebaseDatabase.getInstance()
        val leaderboardRef = db.getReference("leaderboard")

        leaderboardRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // For each rank1..rank10, get username + score
                for (i in 1..NUM_SLOTS) {
                    val slotSnap = snapshot.child("rank$i")
                    val username = slotSnap.child("username")
                        .getValue(String::class.java) ?: "---"
                    val score = slotSnap.child("score")
                        .getValue(Int::class.java) ?: 0

                    rowViews[i - 1].text = formatRow(i, username, score)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to read leaderboard: ${error.message}")
            }
        })
    }

    private fun createRows() {
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = dpToPx(8)
            leftMargin = dpToPx(24)
            rightMargin = dpToPx(24)
        }

        for (i in 1..NUM_SLOTS) {
            val rowTextView = TextView(this).apply {
                text = formatRow(i, "---", 0)
                textSize = 16f
                setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12))
                gravity = Gravity.CENTER_VERTICAL

                // white text
                setTextColor(
                    ContextCompat.getColor(
                        this@LeaderboardActivity,
                        R.color.white
                    )
                )

                // Use your font if you want (you already use this on home)
                typeface = ResourcesCompat.getFont(
                    this@LeaderboardActivity,
                    R.font.space_grotesk_medium
                )

                // Turquoise background, red border
                background = GradientDrawable().apply {
                    setColor(
                        ContextCompat.getColor(
                            this@LeaderboardActivity,
                            R.color.turquoise
                        )
                    )
                    cornerRadius = dpToPx(16).toFloat()
                    // Optional red border
                    setStroke(
                        dpToPx(2),
                        ContextCompat.getColor(
                            this@LeaderboardActivity,
                            R.color.red
                        )
                    )
                }
            }

            leaderboardContainer.addView(rowTextView, layoutParams)
            rowViews.add(rowTextView)
        }
    }

    private fun formatRow(rank: Int, username: String, score: Int): String {
        // Example: "1. Shreyas - 12345"
        return "$rank. $username - $score"
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
