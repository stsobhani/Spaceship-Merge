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
    // The custom view that shows graphics
    private lateinit var gameView : GameView
    // The gesture detector that handles taps
    private lateinit var detector : GestureDetector
    // Logic controller
    private lateinit var spaceshipMerge : SpaceshipMerge
    // Timer which updates state of the game
    private lateinit var gameTimer: Timer
    // Flag which tracks if game is running
    private var gameInProgress : Boolean = true
    // Result launcher that shares score for things like email
    private val shareScoreLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) {
        // This code runs after the share window closes
        returnHome()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // reads the sticky mode option
        val stickyModeEnabled = intent.getBooleanExtra("sticky_mode_enabled", false)

        // hides the bars to allow full screen
        val windowInsetsController : WindowInsetsControllerCompat = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.hide( 	WindowInsetsCompat.Type.systemBars() )
        // Gets the dimensions for the game
        val width : Int = resources.displayMetrics.widthPixels
        val height : Int = resources.displayMetrics.heightPixels
        // initializes the game view with the dimensions
        gameView = GameView( this, width, height)
        setContentView(gameView)
        // Gets the reference to logic controller
        spaceshipMerge = gameView.getSpaceshipMerge()
        // Applies the sticky mode setting
        spaceshipMerge.useStickyMode(stickyModeEnabled)

        // Set up event handling
        var th : TouchHandler = TouchHandler()
        detector = GestureDetector(this, th)
        detector.setOnDoubleTapListener(th)
        // Starts the timer which updates game
        var task : GameTimerTask = GameTimerTask(this)
        gameTimer = Timer()
        gameTimer.schedule(task, 0L, GameView.DELTA_TIME)
    }
    // Handles the touch gestures like scrolling!!
    inner class TouchHandler : GestureDetector.SimpleOnGestureListener(){
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            return super.onSingleTapConfirmed(e)
        }
        // Deals with the scrolling to aim
        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            // Allows dragging if ship ready to launch only
            if(spaceshipMerge.readyToLaunch()){
                //Move the launch shift left and right
                spaceshipMerge.dragShip(-distanceX)
                return true
            }
            return false
        }
    }
    // Launches the ship when person lifts mouse
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (!gameInProgress) return true

        if (event != null) {
            //lets the gesture process event first
            detector.onTouchEvent(event)
            // if person lifts off mouse, the spaceship flies off
            if(event.action == MotionEvent.ACTION_UP){
                spaceshipMerge.launch()
            }
        }
        return true
    }
    // Called when the game ends
    fun endGame(){
        // Stops the timer
        gameTimer.cancel()
        gameTimer.purge()
        gameInProgress = false
        // Gets previous high schore from the shared preferences
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val oldHigh : Int = prefs.getInt(HIGH_SCORE_KEY, 0)
        // Update and gets new high score
        val newHigh : Int = spaceshipMerge.updateHighScore()

        Log.w("GameActivity", "oldHigh=$oldHigh newHigh=$newHigh")

        // Only push to leaderboard if newHigh is actually higher than the old one
        if (newHigh > oldHigh) {
            Log.w("GameActivity", "New high score detected so leaderboard updated")
            updateLeaderboardIfTop10(newHigh)
        } else {
            Log.w("GameActivity", "Not high score so no update to leaderboard")
        }
        runOnUiThread{showGameOverDialog()}
    }
    // Calls all frames to update the game state
    fun updateGameView(){
        // if the game has ended we do not update
        if(!gameInProgress) return
        // checks if the game is over condition is true
        if(gameView.getSpaceshipMerge().isGameOver()){
            endGame()
            return
        }
        // update the ship poisitions
        gameView.getSpaceshipMerge().moveShips()
        // checks to see if collisions between the spaceships
        gameView.getSpaceshipMerge().checkCollisions()
        // triggers the game view
        gameView.postInvalidate()
    }
    // Displays game over dialog
    private fun showGameOverDialog(){
        val dialog = AlertDialog.Builder(this)
            .setTitle("GAME OVER")
            .setMessage("Your ship couldn't fit into the anti-gravity zone.")
            .setCancelable(false)
            // This makes the user choose to play, return, or share score when busted!
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
    // Opens the dialog to let user share score
    private fun sendScoreByEmail() {
        Log.d("Email", "In the email function")
        // gets username and score
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val username = prefs.getString(USERNAME_KEY, "")
        val score = prefs.getInt(HIGH_SCORE_KEY, 0)

        val subject = "Spaceship Merge - My High Score!"
        val body = """
        Hello,
        I just scored $score points in Spaceship Merge!!
        Can you do better?
        
        From, $username""".trimIndent()
        // opens the dialog to let them share score with their app
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }
        // launches the chooser and afterwards, it returns home
        shareScoreLauncher.launch(Intent.createChooser(intent, "Share your score!!"))
    }
    // returns to home menu!
    private fun returnHome(){
        startActivity(Intent(this, MainActivity::class.java))
    }
    // Resets the game state and timer
    private fun restartGame(){
        // Resets the logic to the original state
        gameView.getSpaceshipMerge().reset()
        gameInProgress = true
        // Restarts timer of the game
        gameTimer.cancel()
        gameTimer.purge()
        var task : GameTimerTask = GameTimerTask(this)
        gameTimer = Timer()
        gameTimer.schedule(task, 0L, GameView.DELTA_TIME)
        // Remakes the view!
        gameView.postInvalidate()
    }
    // Updates the leaderboard if in top 10
    private fun updateLeaderboardIfTop10(score: Int) {
        Log.w("GameActivity", "In the leaderboard update fn, score = $score")
        // Gets the username from the preferences
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val username = prefs.getString(USERNAME_KEY, "Player") ?: "Player"
        // If username is blank, does not update
        if (username.isBlank()) {
            Log.w("GameActivity", "Username blank, skipping leaderboard")
            return
        }
        // Gets the database reference to the leaderboard
        val db = FirebaseDatabase.getInstance()
        val leaderboardRef = db.getReference("leaderboard")
        // Reads the current leaderboard data
        leaderboardRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                // Reads the ten usernames and scores
                val usernames = MutableList(10) { i ->
                    snapshot.child("username${i + 1}")
                        .getValue(String::class.java) ?: "***"
                }

                val scores = MutableList(10) { i ->
                    snapshot.child("score${i + 1}")
                        .getValue(Int::class.java) ?: 0
                }

                Log.w("GameActivity", "Current leaderboard usernames=$usernames scores=$scores")

                // Find insert position
                var insertPos = -1
                for (i in 0 until 10) {
                    if (score > scores[i]) {
                        insertPos = i
                        break
                    }
                }
                // The score did not make the top 10
                if (insertPos == -1) {
                    Log.d("GameActivity", "Score not in top 10 so no update")
                    return
                }
                // Shift down scores to make room
                for (i in 9 downTo insertPos + 1) {
                    usernames[i] = usernames[i - 1]
                    scores[i] = scores[i - 1]
                }
                // Put snew score at right place
                usernames[insertPos] = username
                scores[insertPos] = score

                Log.w("GameActivity", "New leaderboard usernames=$usernames scores=$scores")
                // Builds the update map
                val updates = HashMap<String, Any>()
                for (i in 0 until 10) {
                    val idx = i + 1
                    updates["username$idx"] = usernames[i]
                    updates["score$idx"] = scores[i]
                }
                // Writes the updates at once to the firebase
                leaderboardRef.updateChildren(updates)
                    .addOnSuccessListener {
                        Log.w("GameActivity", "Leaderboard update success!!")
                    }
                    .addOnFailureListener { e ->
                        Log.w("GameActivity", "Leaderboard update failed!!", e)
                    }
            }
            // Statement to see if cancelled
            override fun onCancelled(error: DatabaseError) {
                Log.w("GameActivity", "Leaderboard read cancelled: ${error.message}")
            }
        })
    }
}