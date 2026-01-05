package com.example.spaceship_merge

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import android.widget.Space
import android.widget.Toast
import com.example.spaceship_merge.MainActivity.Companion.HIGH_SCORE_KEY
import com.example.spaceship_merge.MainActivity.Companion.PREFS_NAME
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.remove

// The game logic that manages the behaviors, scoring, and rules
class SpaceshipMerge {

    private val width : Int
    private val height : Int
    //Speed the ship moves
    private val shipSpeed = 50f
    // List of the ships in game
    private val spaceships = mutableListOf<Spaceship>()
    // Refers to the ship which is good to launch
    private var shipToLaunch : Spaceship? = null
    // Sees if ship is good to go
    private var launchReady = false
    // Starting position for new ships
    private val launchPosition : Pair<Float, Float>

    private var currentScore : Int = 0

    private var gameOver = false

    private val shipScale = 1.25

    val shipBaseSize : Float

    //If stickyMode is true the ships stick to the boundaries
    private var stickyMode : Boolean = false
    // Tracks highest tier ship which apeared
    private var currentMaxTier = 0
    // Grid to put into cells
    private var grid : Array<Array<GridCell>>
    // assigns id to each ship
    private var nextShipId : Int = 0
    // Acessing the shared preferences
    private var context : Context
    // the hieght of the top score bar
    private val topBarHeight : Int

    private var shipBitmaps = mutableMapOf<Int, Bitmap>()
    // Initialises the game with dimensions and makes the grid
    constructor(width : Int, height : Int, topBarHeight: Int, shipBitmaps: MutableMap<Int, Bitmap>, context : Context){
        this.width = width
        this.height = height
        this.topBarHeight = topBarHeight
        this.launchPosition = Pair<Float, Float>(width/2f, height - 200f)
        this.context = context
        this.shipBitmaps = shipBitmaps

        shipBaseSize = width * .1f

        // Build the grid based on the largest ship size possible
        grid = buildGrid(calculateGridCellSize())
        // loads the first ship
        loadSpaceship((0..2).random())
    }
    // Returns the current score
    fun getScore() : Int{
        return currentScore
    }
    // Resets the game to start new game
    fun reset(){
        spaceships.clear()
        currentMaxTier = 0
        grid = buildGrid(calculateGridCellSize())
        launchReady = false
        gameOver = false
        shipToLaunch = null
        currentScore = 0
        loadSpaceship((0..2).random())
    }

    // Turns on and of sticky mode
    fun useStickyMode(stickyMode : Boolean) {
        this.stickyMode = stickyMode
    }
    // returns when sticky is enabled!
    fun isStickyMode() : Boolean{
        return stickyMode
    }
    //
    fun loadSpaceship(tier : Int){
        // If there is already a ship ready to launch do not load new ship
        if(launchReady) return
        // Calculates the size on tier
        val scale = Math.pow(shipScale, (tier - 1.0)).toFloat()

        val bitmap = shipBitmaps[tier] ?: return

        val aspectRatio = bitmap.width.toFloat() / bitmap.height

        val targetSize = shipBaseSize * scale

        //Determine properly scaled ship width and height
        val (shipWidth, shipHeight) = if(aspectRatio >= 1f){
            //Image is wider than it is tall, match width to target size, scale down the height
            targetSize to (targetSize/aspectRatio)
        }else{
            //Image is taller than it is wide, match height to target size, scale down the width
            (targetSize * aspectRatio) to targetSize
        }

        // If largest ship, rebuilds the grid!
        if(tier > currentMaxTier){
            currentMaxTier = tier
            grid = buildGrid(calculateGridCellSize())
        }
        // Creates the new ship upwards
        val ship = Spaceship(nextShipId, launchPosition.first, y = launchPosition.second, (shipSpeed * Math.cos(Math.toRadians(-90.0))).toFloat(),(shipSpeed * Math.sin(Math.toRadians(-90.0))).toFloat(),shipWidth, shipHeight, tier, 0f, true, false, false)

        nextShipId += 1

        spaceships.add(ship)

        // Sets ship good to take off :))
        shipToLaunch = ship

        // Ready for takeoff!
        launchReady = true
    }

    // Defines a loss
    fun checkLoseCondition(){

    }
    //Determines which grid cells a ship based off the rectangle around the ship!
    private fun getCellsForShip(ship : Spaceship) : MutableSet<GridCell>{
        var cells = mutableSetOf<GridCell>()

        val cellWidth = grid[0][0].rect.width()
        val cellHeight = grid[0][0].rect.height()

        // Sees what rows and columns the spaceship spans
        val minCol = (ship.rect.left / cellWidth).toInt().coerceIn(0, grid[0].size - 1)
        val maxCol = (ship.rect.right / cellWidth).toInt().coerceIn(0, grid[0].size - 1)
        val minRow = (ship.rect.top / cellHeight).toInt().coerceIn(0, grid[0].size - 1)
        val maxRow = (ship.rect.bottom / cellHeight).toInt().coerceIn(0, grid[0].size - 1)
        //adds the cells in this range!
        for(row in minRow..maxRow){
            for(col in minCol..maxCol){
                cells.add(grid[row][col])
            }
        }
        return cells
    }
    fun canFitIntoAntiGravity(ship: Spaceship): Boolean {
        //Determine which cells the ship could be blocked by another ship in
        val blockingCells = getCellsForShip(ship)

        //If any of the blocking cells has visible ship, check if that ship blocks incoming ship
        for (cell in blockingCells) {
            for(blockingShip in cell.ships.filter{it.visible}){
                if (RectF.intersects(blockingShip.rect, ship.rect) && blockingShip.tier != ship.tier) {
                    if(ship.rect.bottom > height/2f){
                        //Return false if ship is sticking out of antigravity zone
                        return false
                    }
                }
            }
        }
        return true
    }

    //Returns whether game eneded
    fun isGameOver() : Boolean{
        return gameOver
    }
    // Updates high schore in shared preferences if current score is higher
    fun updateHighScore(): Int {
        if (!gameOver) return 0

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val oldHigh = prefs.getInt(HIGH_SCORE_KEY, 0)

        val newHigh = if (currentScore > oldHigh) {
            prefs.edit().putInt(HIGH_SCORE_KEY, currentScore).apply()
            currentScore
        } else {
            oldHigh
        }
        return newHigh
    }
    // Calculates the optimal size based on largest ship
    private fun calculateGridCellSize() : Float {
        // finds calculation of biggest ship
        val baseCellSize = shipBaseSize
        val scale = Math.pow(shipScale, (currentMaxTier - 1.0)).toFloat()
        val largestShipSize = shipBaseSize * scale

        // Return cell size that fits biggest ship
        return 100f + largestShipSize
    }
    // Builds the grid of cells which covers antigravity zone
    private fun buildGrid(cellSize : Float) : Array<Array<GridCell>>{
        // Calculates number of colums and rows fit in antigravity zone
        val numGridCols = (width/cellSize).toInt() + 1
        val numGridRows = ((height/2)/cellSize).toInt() + 1
        // calcualtes actuall cell dimensions to fill space
        val cellWidth = width/numGridCols.toFloat()
        val cellHeight = height/numGridRows.toFloat()

        // creates the 2d array of cells
        val generatedGrid = Array(numGridRows){ row ->
            Array(numGridCols){ col ->
                val left = col * cellWidth
                val top = row * cellHeight
                val right = left + cellWidth
                val bottom = top + cellHeight
                // makes grid cell with the position above!
                GridCell(row, col, RectF(left, top, right, bottom))
            }
        }
        return generatedGrid
    }
    // Merges two ships of same tier into the next tier
    private fun mergeShips(ship1 : Spaceship, ship2 : Spaceship, shipsToRemove : MutableList<Spaceship>, shipsToAdd : MutableList<Spaceship>){
        // Only merging if same
        if(ship1.tier != ship2.tier) return
        //creates one higher
        val tier = ship1.tier + 1
        // rebuild if this is largest ship
        if(tier > currentMaxTier){
            currentMaxTier = tier
            grid = buildGrid(calculateGridCellSize())
        }

        //score 500 points for a merge plus 100 times tier points for new ship
        currentScore += 500 + (100 * (tier + 1))

        val mergedX = (ship1.x + ship2.x)/2f
        val mergedY = (ship1.y + ship2.y)/2f
        val mergedVelocityX = (ship1.velocityX + ship2.velocityX)/2f
        val mergedVelocityY = (ship1.velocityY + ship2.velocityY)/2f

        val scale = Math.pow(shipScale, (tier - 1.0)).toFloat()

        val bitmap = shipBitmaps[tier] ?: return

        val aspectRatio = bitmap.width.toFloat() / bitmap.height

        val targetSize = shipBaseSize * scale

        //Determine properly scaled ship width and height
        val (shipWidth, shipHeight) = if(aspectRatio >= 1f){
            //Image is wider than it is tall, match width to target size, scale down the height
            targetSize to (targetSize/aspectRatio)
        }else{
            //Image is taller than it is wide, match height to target size, scale down the width
            (targetSize * aspectRatio) to targetSize
        }

        val mergedShip = Spaceship(nextShipId, mergedX, y = mergedY, mergedVelocityX, mergedVelocityY,shipWidth, shipHeight, tier, 0f, true, true, true)
        nextShipId += 1
        // Updates the rectanle of the ship
        mergedShip.rect.set(
            mergedShip.x - mergedShip.width/2f,
            mergedShip.y - mergedShip.height/2f,
            mergedShip.x + mergedShip.width/2f,
            mergedShip.y + mergedShip.height/2f
        )
        // will make ship invisible!
        ship1.visible = false
        ship2.visible = false

        shipsToRemove.add(ship1)
        shipsToRemove.add(ship2)

        shipsToAdd.add(mergedShip)
    }

    // Using researched physics formulas when two ships of different tiers hit each other
    // Was actually fun researching on this topic :) Hope these notes are interesting
    private fun bounceShips(ship1 : Spaceship, ship2 : Spaceship){
        if(ship1.tier == ship2.tier) return

        // Uses the vector reflection formula R = V - 2 * (V • N) * N

        // First, calculate the unit normal vectors using formula N = VectorAB/|VectorAB|,
        // where VectorAB is the vector from ship1 to ship2 and |VectorAB| is the length of VectorAB
        val diffX = ship1.x - ship2.x
        val diffY = ship1.y - ship2.y
        val length = Math.max(Math.sqrt((diffX * diffX + diffY * diffY).toDouble()), .01) //protect against division by zero
        val unitNormalX = (diffX / length).toFloat()
        val unitNormalY = (diffY / length).toFloat()
        // obtains the velocities
        var velX1 = ship1.velocityX
        var velY1 = ship1.velocityY
        var velX2 = ship2.velocityX
        var velY2 = ship2.velocityY
        // if ship1 is stationary, gets opposite velocity of ship2!
        if(ship1.velocityX == 0f && ship1.velocityY == 0f){
            velX1 = -ship2.velocityX * (0.80f)
            velY1 = -ship2.velocityY * (0.80f)
        }
        // vise versa!
        if(ship2.velocityX == 0f && ship2.velocityY == 0f){
            velX2 = -ship1.velocityX * (0.80f)
            velY2 = -ship1.velocityY * (0.80f)
        }

        // Calculate the dot products of each ship's velocity vectors with (V • N)
        var dotProduct1 = velX1 * unitNormalX + velY1 * unitNormalY
        var dotProduct2 = velX2 * unitNormalX + velY2 * unitNormalY

        // Reflect each velocity using the vector reflection formula: R = V - 2 * (V • N) * N
        ship1.velocityX = (velX1 - 2 * dotProduct1 * unitNormalX)
        ship1.velocityY = (velY1 - 2 * dotProduct1 * unitNormalY)
        ship2.velocityX = (velX2 - 2 * dotProduct2 * unitNormalX)
        ship2.velocityY = (velY2 - 2 * dotProduct2 * unitNormalY)

        // Offset the rockets so they are no longer overlapping
        val offset = 5f
        ship1.x += (unitNormalX * offset)
        ship1.y += (unitNormalY * offset)
        ship2.x -= (unitNormalX * offset)
        ship2.y -= (unitNormalY * offset)

        // Update rectangles
        val ship1Left = ship1.x - ship1.width/2
        val ship1Top = ship1.y - ship1.height/2
        val ship1Right = ship1.x + ship1.width/2
        val ship1Bottom = ship1.y + ship1.height/2

        val ship2Left = ship2.x - ship2.width/2
        val ship2Top = ship2.y - ship2.height/2
        val ship2Right = ship2.x + ship2.width/2
        val ship2Bottom = ship2.y + ship2.height/2

        ship1.rect.set(ship1Left, ship1Top, ship1Right, ship1Bottom)
        ship2.rect.set(ship2Left, ship2Top, ship2Right, ship2Bottom)

    }
    // returns list of all ships in game
    fun getShips() : List<Spaceship>{
        return spaceships
    }
    // returns if ship is good to launch
    fun readyToLaunch() : Boolean{
        return launchReady
    }

    // Moves the ship to launch left or right when dragged
    fun dragShip(distanceX : Float){

        val ship = shipToLaunch ?: return
        // horizontal
        ship.x += distanceX
        // left boundary!
        if(ship.x - ship.width/2f < 0f){
            ship.x = ship.width/2f
        }
        // right boundary!
        if(ship.x + ship.width/2f > width){
            ship.x = width - ship.width/2f
        }
        // updating the bounding rectangle
        ship.rect.set(
            ship.x - ship.width / 2f,
            ship.y - ship.height / 2f,
            ship.x + ship.width / 2f,
            ship.y + ship.height / 2f
        )
    }

    fun getShipToLaunch() : Spaceship?{
        return shipToLaunch
    }

    fun moveShipToLaunch(x : Float){
        val ship = shipToLaunch ?: return

        ship.x = x

        // left boundary!
        if(ship.x - ship.width/2f < 0f){
            ship.x = ship.width/2f
        }
        // right boundary!
        if(ship.x + ship.width/2f > width){
            ship.x = width - ship.width/2f
        }
        // updating the bounding rectangle
        ship.rect.set(
            ship.x - ship.width / 2f,
            ship.y - ship.height / 2f,
            ship.x + ship.width / 2f,
            ship.y + ship.height / 2f
        )
    }

    // Launches the current spaceship
    fun launch(){
        val ship = shipToLaunch ?: return

        ship.launched = true
        shipToLaunch = null
        launchReady = false

        // Load next spaceship to launch
        loadSpaceship((0..2).random())
    }

    // Updates the position of all the moving ships
    fun moveShips(){
        for (ship in spaceships.toList()) {

            // Moves the ship launched and has velocity
            if(ship.launched && (Math.abs(ship.velocityX) > 0.1f || Math.abs(ship.velocityY) > 0.1f)) {

//                if(!ship.antiGravity && ship.y < height/2f){
//                    ship.antiGravity = true
//                }

                // Checks if ship in antigravity area
                if (!ship.antiGravity && ship.rect.top < height/2f){
                    if (!canFitIntoAntiGravity(ship)){
                        gameOver = true
                        return
                    }

                    //If ship is fully inside antigravity zone makr true
                    if(ship.rect.bottom <= height/2f){
                        ship.antiGravity = true

                        // award points for good entry
                        currentScore += 100 * (ship.tier + 1)
                    }
                }
                // Applies velocity to position
                ship.x += ship.velocityX
                ship.y += ship.velocityY
                // updates the rectangle
                ship.rect.set(
                    ship.x - ship.width/2f,
                    ship.y - ship.height/2f,
                    ship.x + ship.width/2f,
                    ship.y + ship.height/2f
                )

                // handles collision with left and right boundaries
                if(ship.x - ship.width/2f < 0f || ship.x + ship.width/2f > width) {
                    ship.velocityX *= -1
                }

                // handles colision with top bottom boundaries
                if(ship.antiGravity && (ship.y - ship.height/2f < topBarHeight || ship.y + ship.height/2f > height/2f )) {
                    ship.velocityY *= -1
                }
            }else if(ship.launched){
                // Stop the movement of ships if under .1
                if(Math.abs(ship.velocityX) > 0f) ship.velocityX = 0f
                if(Math.abs(ship.velocityY) > 0f) ship.velocityY = 0f
            }

            if(ship.antiGravity) {

                // Decrease the ships velocity until it stops
                ship.velocityX *= 0.97f
                ship.velocityY *= 0.97f

                //val shipRect = ship.getRect()

                val newCells = getCellsForShip(ship)

                // Remove ship from previous cells
                for(cell in ship.currentCells){
                    if(!newCells.contains(cell)) {
                        cell.ships.remove(ship)
                    }
                }
                // adds the ship to the new cell
                for(cell in newCells) {
                    cell.ships.add(ship)
                }

                ship.currentCells = newCells


                // Keep the ships within the bounds
                val minY = ship.height/2f + topBarHeight
                val maxY = height/2f - ship.height/2f
                val minX = ship.width/2f
                val maxX = width - ship.width/2f
                // this stops y position and velocity for stickyy
                if(ship.y < minY){
                    ship.y = minY
                    if(stickyMode) ship.velocityY = 0f
                }
                if(ship.y > maxY){
                    ship.y = maxY
                    if(stickyMode) ship.velocityY = 0f
                }
                if(ship.x < minX){
                    ship.x = minX
                    if(stickyMode) ship.velocityX = 0f
                }
                if(ship.x > maxX){
                    ship.x = maxX
                    if(stickyMode) ship.velocityX = 0f
                }
                // updates rectangle after the clamp
                ship.rect.set(
                    ship.x - ship.width/2f,
                    ship.y - ship.height/2f,
                    ship.x + ship.width/2f,
                    ship.y + ship.height/2f
                )
            }
        }
    }

    // Checks for any ship collisions
    fun checkCollisions(){

        // tracks the pairs of ships already compared
        val comparedShips = mutableSetOf<Pair<Int, Int>>()
        val shipsToRemove = mutableListOf<Spaceship>()
        val shipsToAdd = mutableListOf<Spaceship>()
        // checks each ship for any collision
        for(ship in spaceships.toList()) {
            val cells = getCellsForShip(ship)
            // Collect all other visbile ships in those cells
            val visibleShipsInCells = mutableSetOf<Spaceship>()

            for (cell in cells) {
                for (otherShip in cell.ships.filter { it.visible && it != ship}) {
                    visibleShipsInCells.add(otherShip)
                }
            }
            // Checks with each nearby ship
            for (otherShip in visibleShipsInCells) {
                // skips if we already did this one
                if(comparedShips.contains(Pair(ship.id, otherShip.id))) continue

                comparedShips.add(Pair(ship.id, otherShip.id))
                comparedShips.add(Pair(otherShip.id, ship.id))
                // checks if the rectangles intersect
                if (ship != otherShip && RectF.intersects(ship.rect, otherShip.rect)) {
                    Log.w("MainActivity", "Collision detected")
                    // if same tier we merge
                    if (ship.tier == otherShip.tier) {
                        mergeShips(ship, otherShip, shipsToRemove, shipsToAdd)
                    } else {
                        // we bounce around!!
                        if(ship.rect.bottom <= height/2f){
                            bounceShips(ship, otherShip)
                        }
                    }
                }
            }
        }
        // removes the merge ships and update cells
        for(ship in shipsToRemove){
            spaceships.remove(ship)
            for(cell in ship.currentCells) {
                cell.ships.remove(ship)
            }
        }

        // adds new merged ships and add to grid cell
        for(ship in shipsToAdd){
            spaceships.add(ship)

            //val shipRect = ship.getRect()
            // finds which cell the new ship has
            val newCells = getCellsForShip(ship)
            // adds ship to these cells
            for(cell in newCells){
                cell.ships.add(ship)
            }
            ship.currentCells = newCells
        }
    }

    // This class represents a spaceship!!
    data class Spaceship(
        val id : Int,
        var x : Float,
        var y : Float,
        var velocityX : Float,
        var velocityY : Float,
        var width : Float,
        var height : Float,
        val tier : Int,
        var shipAngle : Float,
        var visible : Boolean = true,
        var launched : Boolean = false,
        var antiGravity : Boolean = false,
        var currentCells: MutableSet<GridCell> = mutableSetOf(),
        val rect : RectF = RectF(
            x - width / 2f,
            y - height / 2f,
            x + width / 2f,
            y + height / 2f
        )) {
            fun getHitbox(): RectF {
//                val bodyWidth = (rect.width() * 0.45f).toInt()
//                val bodyHeight = (rect.height() * 0.75f).toInt()
//
//                val centerX = rect.centerX()
//                val top = rect.top + (rect.height() * 0.1f).toInt()
//
//                return RectF(
//                    centerX - bodyWidth / 2,
//                    top,
//                    centerX + bodyWidth / 2,
//                    top + bodyHeight
//                )
                return rect
            }
        }


    // Defines one cell in collision.
    class GridCell(
        val row: Int,
        val col: Int,

        // Stores the grid cell position
        val rect: RectF,

        //Stores references to all ships in current cell
        val ships: MutableSet<Spaceship> = mutableSetOf(),

        //Stores adjacent cells
        val adjacentCells: MutableSet<GridCell> = mutableSetOf()
    )
}