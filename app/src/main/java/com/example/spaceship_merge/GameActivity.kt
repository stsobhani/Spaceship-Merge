package com.example.spaceship_merge

import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.util.Timer

class GameActivity : AppCompatActivity() {
    private lateinit var gameView : GameView

    private lateinit var detector : GestureDetector

    private lateinit var spaceshipMerge : SpaceshipMerge


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // hide top and bottom bars
        val windowInsetsController : WindowInsetsControllerCompat = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.hide( 	WindowInsetsCompat.Type.systemBars() )

        val width : Int = resources.displayMetrics.widthPixels
        val height : Int = resources.displayMetrics.heightPixels

        gameView = GameView( this, width, height)
        setContentView(gameView)



        spaceshipMerge = gameView.getSpaceshipMerge()


        // Set up event handling
        var th : TouchHandler = TouchHandler()
        detector = GestureDetector(this, th)
        detector.setOnDoubleTapListener(th)

        var task : GameTimerTask = GameTimerTask(this)
        var gameTimer : Timer = Timer()
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
                return super.onScroll(e1, e2, distanceX, distanceY)
            }

            return false

        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event != null) {
            detector.onTouchEvent(event)

            if(event.action == MotionEvent.ACTION_UP){
                spaceshipMerge.launch()
            }

        }
        return super.onTouchEvent(event)
    }

    fun updateGameView(){
        gameView.getSpaceshipMerge().moveShips()
        gameView.getSpaceshipMerge().checkCollisions()
        gameView.postInvalidate()
    }
}