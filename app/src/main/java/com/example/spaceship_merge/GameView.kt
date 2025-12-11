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

    private var paint : Paint = Paint()
    private var width : Int = 0
    private var height : Int = 0

    private val numTiers : Int = 12

    private val spaceshipMerge : SpaceshipMerge

    //Maps spaceship type to spaceship image to be loaded
    private val shipBitmaps = mutableMapOf<Int, Bitmap>()
    private var shipRect : Rect? = null

    private val background = BitmapFactory.decodeResource(resources, R.drawable.space_background)
    private val scaledBackground : Bitmap

    private var screenHeight : Int = 0

    private var screenWidth : Int = 0

    val topBarHeight : Int = 150

    constructor(context : Context, width : Int, height : Int) : super(context){
        this.screenWidth = width
        this.screenHeight = height

        this.width = this.screenWidth
        this.height = this.screenHeight - this.topBarHeight



        //Scales the background image to fill the screen
        scaledBackground = Bitmap.createScaledBitmap(background, width, height - topBarHeight, false)

        paint.isAntiAlias = true

        loadShipImages()

        spaceshipMerge = SpaceshipMerge(this.width, this.height, this.topBarHeight, context)
    }

    private fun loadShipImages() {

        for(i in 0..numTiers) {
            val resourceId = resources.getIdentifier("ship$i", "drawable", context.packageName)
            shipBitmaps[i] = BitmapFactory.decodeResource(resources, resourceId)
        }

//        shipBitmaps[1] = BitmapFactory.decodeResource(resources, R.drawable.ship1)
//        shipBitmaps[2] = BitmapFactory.decodeResource(resources, R.drawable.ship2)
    }

    override fun onDraw( canvas : Canvas ) {
        super.onDraw( canvas )

        //Log.w("MainActivity", "Inside onDraw")

//        //Draw background color
//        canvas.drawColor(Color.BLACK)

        //Draw space image bitmap for background
//        val background = BitmapFactory.decodeResource(resources, R.drawable.space_background)
//        canvas.drawBitmap(background, 0f, 0f, null)

//        val scaledBackground = Bitmap.createScaledBitmap(background, canvas.width, canvas.height, true)
        canvas.drawBitmap(scaledBackground, 0f, topBarHeight.toFloat(), null)

        val barPaint = Paint()
        barPaint.color = ContextCompat.getColor(context, R.color.turquoise)

        canvas.drawRect(0f, 0f, canvas.width.toFloat(), topBarHeight.toFloat(), barPaint)

        barPaint.color = ContextCompat.getColor(context, R.color.red)
        barPaint.style = Paint.Style.STROKE
        barPaint.strokeWidth = 5f
        canvas.drawRect(0f, 0f, canvas.width.toFloat(), topBarHeight.toFloat(), barPaint)


        val textPaint = Paint()
        textPaint.color = Color.WHITE
        textPaint.textSize = 100f
        textPaint.isAntiAlias = true

        canvas.drawText("Score: ${spaceshipMerge.getScore()}", 20f, topBarHeight * .7f, textPaint)

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
            //Don't draw ship if not visible
            if(!ship.visible) continue
            val bitmap = shipBitmaps[ship.tier] ?: continue

//            canvas.rotate(ship.shipAngle, ship.x, ship.y)
            canvas.drawBitmap(bitmap, null, ship.rect, paint)
        }

    }

    fun getSpaceshipMerge() : SpaceshipMerge{
        return spaceshipMerge
    }

    companion object{
        const val DELTA_TIME : Long = 30
    }
}