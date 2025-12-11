package com.example.spaceship_merge

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat

class GameView : View {
    // Paint object for drawing
    private var paint : Paint = Paint()
    private var width : Int = 0
    private var height : Int = 0
    // the total number of tiers of the spaceships
    private val numTiers : Int = 12
    // The main game logic controller
    private val spaceshipMerge : SpaceshipMerge

    // Maps spaceship tier numbers to corrresponding bitmaps
    private val shipBitmaps = mutableMapOf<Int, Bitmap>()
    // The original background image
    private val background = BitmapFactory.decodeResource(resources, R.drawable.space_background)
    // The background image to fit screen
    private val scaledBackground : Bitmap
    private var screenHeight : Int = 0
    private var screenWidth : Int = 0
    // Hieght of top score bar
    val topBarHeight : Int = 150
    // This initialises the game view with the dimensions
    constructor(context : Context, width : Int, height : Int) : super(context){
        this.screenWidth = width
        this.screenHeight = height
        // Playable areas but not the top bar!!
        this.width = this.screenWidth
        this.height = this.screenHeight - this.topBarHeight

        //Scales the background image to fill the screen
        scaledBackground = Bitmap.createScaledBitmap(background, width, height - topBarHeight, false)
        // Enables the anti aliasing to help graphics
        paint.isAntiAlias = true
        // loads the ship tiers
        loadShipImages()
        // initializes the game logic with dimensions
        spaceshipMerge = SpaceshipMerge(this.width, this.height, this.topBarHeight, context)
    }
    // Loads the ship bitmap images from the drawable resources!
    private fun loadShipImages() {

        for(i in 0..numTiers) {
            val resourceId = resources.getIdentifier("ship$i", "drawable", context.packageName)
            shipBitmaps[i] = BitmapFactory.decodeResource(resources, resourceId)
        }
    }
    // Called whenever the view needs to be redrawn
    override fun onDraw(canvas : Canvas) {
        super.onDraw(canvas)
        // Draws the space background
        canvas.drawBitmap(scaledBackground, 0f, topBarHeight.toFloat(), null)
        // Draws the score bar
        val barPaint = Paint()
        barPaint.color = ContextCompat.getColor(context, R.color.turquoise)

        canvas.drawRect(0f, 0f, canvas.width.toFloat(), topBarHeight.toFloat(), barPaint)
        // The red border aroud the score
        barPaint.color = ContextCompat.getColor(context, R.color.red)
        barPaint.style = Paint.Style.STROKE
        barPaint.strokeWidth = 5f
        canvas.drawRect(0f, 0f, canvas.width.toFloat(), topBarHeight.toFloat(), barPaint)
        // Score text in top bar
        val textPaint = Paint()
        textPaint.color = Color.WHITE
        textPaint.textSize = 100f
        textPaint.isAntiAlias = true

        canvas.drawText("Score: ${spaceshipMerge.getScore()}", 20f, topBarHeight * .7f, textPaint)
        // Draws the horizontal line which is the anti gravity area
        val linePaint = Paint()
        linePaint.color = ContextCompat.getColor(context, R.color.red)
        linePaint.strokeWidth = 3f
        linePaint.style = Paint.Style.STROKE
        linePaint.isAntiAlias = true

        canvas.drawLine(0f, height/2f, width.toFloat(), height/2f, linePaint)

        //Retrieve all ships in the current state of the game
        val ships = spaceshipMerge.getShips()

        //Draw all spaceships at current game state
        for(ship in ships){
            //Do not draw ship if not visible
            if(!ship.visible) continue
            val bitmap = shipBitmaps[ship.tier] ?: continue
            canvas.drawBitmap(bitmap, null, ship.rect, paint)
        }
    }
    //Provides access to logic controller
    fun getSpaceshipMerge() : SpaceshipMerge{
        return spaceshipMerge
    }
    // States the time between updates
    companion object{
        const val DELTA_TIME : Long = 30
    }
}