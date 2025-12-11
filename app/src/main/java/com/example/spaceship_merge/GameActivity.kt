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
import com.example.spaceship_merge.MainActivity.Companion.PREFS_NAME
import com.example.spaceship_merge.MainActivity.Companion.USERNAME_KEY
import com.example.spaceship_merge.MainActivity.Companion.HIGH_SCORE_KEY

class GameActivity : AppCompatActivity() {
    private lateinit var gameView : GameView

    private lateinit var detector : GestureDetector

    private lateinit var spaceshipMerge : SpaceshipMerge

    private lateinit var gameTimer: Timer

    private var gameInProgress : Boolean = true


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

        spaceshipMerge.updateHighScore()

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

        startActivity(Intent.createChooser(intent, "Share your score"))
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

}