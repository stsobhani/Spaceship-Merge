package com.example.spaceship_merge

import android.content.Intent
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.util.Timer
import android.net.Uri
import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.example.spaceship_merge.MainActivity.Companion.PREFS_NAME
import com.example.spaceship_merge.MainActivity.Companion.USERNAME_KEY
import com.example.spaceship_merge.MainActivity.Companion.HIGH_SCORE_KEY

class GameActivity : AppCompatActivity() {
    private lateinit var gameView : GameView

    private lateinit var detector : GestureDetector

    private lateinit var spaceshipMerge : SpaceshipMerge

    private lateinit var gameTimer: Timer

    private var gameInProgress : Boolean = true

    private val shareScoreLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) {
        // This code runs AFTER the email/share window closes
        returnHome()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // read sticky mode option
        val stickyModeEnabled = intent.getBooleanExtra("sticky_mode_enabled", false)

        // hide top and bottom bars
        val windowInsetsController : WindowInsetsControllerCompat = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.hide( 	WindowInsetsCompat.Type.systemBars() )

        val width : Int = resources.displayMetrics.widthPixels
        val height : Int = resources.displayMetrics.heightPixels

        gameView = GameView( this, width, height)
        setContentView(gameView)

        spaceshipMerge = gameView.getSpaceshipMerge()

        spaceshipMerge.useStickyMode(stickyModeEnabled)

        // Set up event handling
        var th : TouchHandler = TouchHandler()
        detector = GestureDetector(this, th)
        detector.setOnDoubleTapListener(th)

        var task : GameTimerTask = GameTimerTask(this)
        gameTimer = Timer()
        gameTimer.schedule(task, 0L, GameView.DELTA_TIME)
    }

    inner class TouchHandler : GestureDetector.SimpleOnGestureListener(){
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            return super.onSingleTapConfirmed(e)
        }

        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {

            if(spaceshipMerge.readyToLaunch()){
                //Move the launch shift left and right (to aim)
                spaceshipMerge.dragShip(-distanceX)
                return true
            }

            return false

        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (!gameInProgress) return true

        if (event != null) {
            detector.onTouchEvent(event)

            if(event.action == MotionEvent.ACTION_UP){
                spaceshipMerge.launch()
            }

        }
        return true
    }

    fun endGame(){
        gameTimer.cancel()
        gameTimer.purge()
        gameInProgress = false

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val oldHigh : Int = prefs.getInt(HIGH_SCORE_KEY, 0)
        val newHigh : Int = spaceshipMerge.updateHighScore()  // <- we will modify this function too

        Log.d("GameActivity", "oldHigh=$oldHigh newHigh=$newHigh")

        // Only push to leaderboard if the newHigh is actually higher than the old one
        if (newHigh > oldHigh) {
            Log.d("GameActivity", "New real high score detected → updating leaderboard")
            updateLeaderboardIfTop10(newHigh)
        } else {
            Log.d("GameActivity", "Not a real high score → skipping leaderboard update")
        }


        runOnUiThread{showGameOverDialog()}
    }

    fun updateGameView(){
        if(!gameInProgress) return

        if(gameView.getSpaceshipMerge().isGameOver()){
            endGame()
            return
        }

        gameView.getSpaceshipMerge().moveShips()
        gameView.getSpaceshipMerge().checkCollisions()

        gameView.postInvalidate()
    }

    private fun showGameOverDialog(){
        val dialog = AlertDialog.Builder(this)
            .setTitle("GAME OVER")
            .setMessage("Your ship couldn't fit into the anti-gravity zone.")
            .setCancelable(false)
            .setPositiveButton("Play Again"){_, _ ->
                restartGame()
            }
            .setNegativeButton("Return Home"){_, _ ->
                returnHome()
            }
            .setNeutralButton("Share Score") { _, _ -> sendScoreByEmail()
            }
            .create()

        dialog.show()
    }

    private fun sendScoreByEmail() {
        Log.d("Email", "In the email function")

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val username = prefs.getString(USERNAME_KEY, "")
        val score = prefs.getInt(HIGH_SCORE_KEY, 0)

        val subject = "Spaceship Merge - My High Score!"
        val body = """
        Hey,

        I just scored $score points in Spaceship Merge 🚀
        Username: $username

        Can you beat my score?

        - $username
    """.trimIndent()

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"  // <-- allows ANY share app (SMS, Discord, Gmail, Notes, etc.)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }

        shareScoreLauncher.launch(Intent.createChooser(intent, "Share your score"))
    }

    private fun returnHome(){
        startActivity(Intent(this, MainActivity::class.java))
    }

    private fun restartGame(){
        gameView.getSpaceshipMerge().reset()
        gameInProgress = true

        gameTimer.cancel()
        gameTimer.purge()
        var task : GameTimerTask = GameTimerTask(this)
        gameTimer = Timer()
        gameTimer.schedule(task, 0L, GameView.DELTA_TIME)

        gameView.postInvalidate()
    }

    private fun updateLeaderboardIfTop10(score: Int) {
        Log.d("GameActivity", "In the leaderboard update fn, score = $score")

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val username = prefs.getString(USERNAME_KEY, "Player") ?: "Player"

        if (username.isBlank()) {
            Log.d("GameActivity", "Username blank, skipping leaderboard")
            return
        }

        val db = FirebaseDatabase.getInstance()
        val leaderboardRef = db.getReference("leaderboard")

        leaderboardRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                // Read username1..username10 and score1..score10
                val usernames = MutableList(10) { i ->
                    snapshot.child("username${i + 1}")
                        .getValue(String::class.java) ?: "---"
                }

                val scores = MutableList(10) { i ->
                    snapshot.child("score${i + 1}")
                        .getValue(Int::class.java) ?: 0
                }

                Log.d("GameActivity", "Current leaderboard usernames=$usernames scores=$scores")

                // Find insert position (higher scores are better)
                var insertPos = -1
                for (i in 0 until 10) {
                    if (score > scores[i]) {
                        insertPos = i
                        break
                    }
                }

                if (insertPos == -1) {
                    Log.d("GameActivity", "Score not in top 10, not updating")
                    return
                }

                // Shift down entries to make room for new entry
                for (i in 9 downTo insertPos + 1) {
                    usernames[i] = usernames[i - 1]
                    scores[i] = scores[i - 1]
                }

                usernames[insertPos] = username
                scores[insertPos] = score

                Log.d("GameActivity", "New leaderboard usernames=$usernames scores=$scores")

                val updates = HashMap<String, Any>()
                for (i in 0 until 10) {
                    val idx = i + 1
                    updates["username$idx"] = usernames[i]
                    updates["score$idx"] = scores[i]
                }

                leaderboardRef.updateChildren(updates)
                    .addOnSuccessListener {
                        Log.d("GameActivity", "Leaderboard update success")
                    }
                    .addOnFailureListener { e ->
                        Log.e("GameActivity", "Leaderboard update FAILED", e)
                    }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("GameActivity", "Leaderboard read cancelled: ${error.message}")
            }
        })
    }


}