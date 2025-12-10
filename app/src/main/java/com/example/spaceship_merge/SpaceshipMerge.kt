package com.example.spaceship_merge

import android.content.Context
import android.graphics.RectF
import android.util.Log
import android.widget.Space
import kotlin.collections.mutableListOf

//Lose condition idea: if you launch a ship that hits another ship before full entering anti-gravity zone, you lose

//Handles the logic of the Spaceship Merge Game
class SpaceshipMerge {

    private val width : Int
    private val height : Int

    private var shipSpeed = 50f

    private val spaceships = mutableListOf<Spaceship>()

    private var shipToLaunch : Spaceship? = null
    private var launchReady = false
    private val launchPosition : Pair<Float, Float>

    private val gridCellSize = 300f

    private val numGridCols : Int
    private val numGridRows : Int

    private val shipScale = 1.25

    val shipBaseSize : Float

    //If stickyMode is true, ships stick to the boundaries of the screen
    private var stickyMode : Boolean = false

    private val grid : Array<Array<GridCell>>

    constructor(width : Int, height : Int, context : Context){
        this.width = width
        this.height = height
        this.launchPosition = Pair<Float, Float>(width/2f, height - 200f)

        shipBaseSize = width * .1f

        //Define grid squares that fill the anti-gravity zone
        numGridCols = (width/gridCellSize).toInt() + 1
        numGridRows = ((height/2)/gridCellSize).toInt() + 1

        val cellWidth = width/numGridCols.toFloat()
        val cellHeight = height/numGridRows.toFloat()

        //2D array of grid cells that fill the anti-gravity zone for collision detection
        grid = Array(numGridRows){ row ->
            Array(numGridCols){ col ->
                val left = col * cellWidth
                val top = row * cellHeight
                val right = left + cellWidth
                val bottom = top + cellHeight
                GridCell(row, col, RectF(left, top, right, bottom))
            }
        }

        setUpCellAdjacencies()

        loadSpaceship(1)
    }

    //Turn on and of sticky mode
    fun useStickyMode(stickyMode : Boolean) {
        this.stickyMode = stickyMode
    }

    fun isStickyMode() : Boolean{
        return stickyMode
    }

    //Add references to all adjacent cells of every grid cell
    private fun setUpCellAdjacencies(){
        for(row in grid.indices){
            for(col in grid[row].indices){
                //Add upper adjacent cell
                if(row != 0){
                    grid[row][col].adjacentCells.add(grid[row - 1][col])

                    //Add upper left adjacent cell
                    if(col != 0){
                        grid[row][col].adjacentCells.add(grid[row - 1][col - 1])
                    }

                    //Add upper right adjacent cell
                    if(col != grid[row].size - 1){
                        grid[row][col].adjacentCells.add(grid[row - 1][col + 1])
                    }
                }



                //Add lower adjacent cell
                if(row != grid.size - 1){
                    grid[row][col].adjacentCells.add(grid[row + 1][col])

                    //Add lower left adjacent cell
                    if(col != 0){
                        grid[row][col].adjacentCells.add(grid[row + 1][col - 1])
                    }

                    //Add lower right adjacent cell
                    if(col != grid[row].size - 1){
                        grid[row][col].adjacentCells.add(grid[row + 1][col + 1])
                    }
                }

                //Add left adjacent cell
                if(col != 0){
                    grid[row][col].adjacentCells.add(grid[row][col - 1])
                }

                //Add right adjacent cell
                if(col != grid[row].size - 1){
                    grid[row][col].adjacentCells.add(grid[row][col + 1])
                }
            }
        }
    }

    fun loadSpaceship(tier : Int){
        //If there is already a ship ready to launch, don't load a new ship
        if(launchReady) return

        val scale = Math.pow(shipScale, (tier - 1.0)).toFloat()

        val shipWidth = (shipBaseSize * scale)
        val shipHeight = (shipBaseSize * scale)


        val ship = Spaceship(launchPosition.first, y = launchPosition.second, (shipSpeed * Math.cos(Math.toRadians(-90.0))).toFloat(),(shipSpeed * Math.sin(Math.toRadians(-90.0))).toFloat(),shipWidth, shipHeight, tier, 0f, true, false, false)

        spaceships.add(ship)

        //Set as the ship to launch
        shipToLaunch = ship

        //Spaceship is now ready to launch
        launchReady = true
    }

    private fun mergeShips(ship1 : Spaceship, ship2 : Spaceship, shipsToRemove : MutableList<Spaceship>, shipsToAdd : MutableList<Spaceship>){
        if(ship1.tier != ship2.tier) return

        val tier = ship1.tier + 1
        val mergedX = (ship1.x + ship2.x)/2f
        val mergedY = (ship1.y + ship2.y)/2f
        val mergedVelocityX = (ship1.velocityX + ship2.velocityX)/2f
        val mergedVelocityY = (ship1.velocityY + ship2.velocityY)/2f

        val scale = Math.pow(shipScale, (tier - 1.0)).toFloat()

        val shipWidth = (shipBaseSize * scale)
        val shipHeight = (shipBaseSize * scale)

        val mergedShip = Spaceship(mergedX, y = mergedY, mergedVelocityX, mergedVelocityY,shipWidth, shipHeight, tier, 0f, true, true, true)

        mergedShip.rect.set(
            mergedShip.x - mergedShip.width/2f,
            mergedShip.y - mergedShip.height/2f,
            mergedShip.x + mergedShip.width/2f,
            mergedShip.y + mergedShip.height/2f
        )

        ship1.visible = false
        ship2.visible = false

        shipsToRemove.add(ship1)
        shipsToRemove.add(ship2)

        shipsToAdd.add(mergedShip)
    }

    //Using researched physics formulas for reflecting a velocity vector across a collision normal,
    //implements bounce mechanics for the two colliding ships
    private fun bounceShips(ship1 : Spaceship, ship2 : Spaceship){
        if(ship1.tier == ship2.tier) return

        //Using the vector reflection formula R = V - 2 * (V • N) * N, we find the reflected velocities

        //First, calculate the unit normal vectors using the formula N = VectorAB/|VectorAB|,
        //Where VectorAB is the vector from ship1 to ship2 (difference) and |VectorAB| is the length of VectorAB
        val diffX = ship1.x - ship2.x
        val diffY = ship1.y - ship2.y
        val length = Math.max(Math.sqrt((diffX * diffX + diffY * diffY).toDouble()), .01) //protect against division by zero
        val unitNormalX = (diffX / length).toFloat()
        val unitNormalY = (diffY / length).toFloat()

        //Next, calculate the dot products of each ship's velocity vectors with the unit normal vectors: (V • N)
        val dotProduct1 = ship1.velocityX * unitNormalX + ship1.velocityY * unitNormalY
        val dotProduct2 = ship2.velocityX * unitNormalX + ship2.velocityY * unitNormalY

        //Finally, reflect each velocity using the vector reflection formula: R = V - 2 * (V • N) * N
        ship1.velocityX = (ship1.velocityX - 2 * dotProduct1 * unitNormalX)
        ship1.velocityY = (ship1.velocityY - 2 * dotProduct1 * unitNormalY)
        ship2.velocityX = (ship2.velocityX - 2 * dotProduct2 * unitNormalX)
        ship2.velocityY = (ship2.velocityY - 2 * dotProduct2 * unitNormalY)


        //Offset the rockets so they are no longer overlapping
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



    fun getShips() : List<Spaceship>{
        return spaceships
    }

    fun readyToLaunch() : Boolean{
        return launchReady
    }

    //Moves the ship to launch left or right when dragged
    fun dragShip(distanceX : Float){

        val ship = shipToLaunch ?: return

        ship.x += distanceX

        if(ship.x - ship.width/2f < 0f){
            ship.x = ship.width/2f
        }

        if(ship.x + ship.width/2f > width){
            ship.x = width - ship.width/2f
        }

        ship.rect.set(
            ship.x - ship.width / 2f,
            ship.y - ship.height / 2f,
            ship.x + ship.width / 2f,
            ship.y + ship.height / 2f
        )
    }

    //Initiate a spaceship launch
    fun launch(){
        val ship = shipToLaunch ?: return

        ship.launched = true
        shipToLaunch = null
        launchReady = false

        //Load next spaceship to launch
        loadSpaceship(1)
    }

    //Handle movement of any spaceship, every frame
    fun moveShips(){
        for (ship in spaceships.toList()) {

            //Move ship if it hasn't slowed enough yet
            if(ship.launched && (Math.abs(ship.velocityX) > 0.1f || Math.abs(ship.velocityY) > 0.1f)) {

                //Ship enters anti-gravity zone in upper half of screen
                if(!ship.antiGravity && ship.y < height/2f){
                    ship.antiGravity = true
                }

                ship.x += ship.velocityX
                ship.y += ship.velocityY

                ship.rect.set(
                    ship.x - ship.width/2f,
                    ship.y - ship.height/2f,
                    ship.x + ship.width/2f,
                    ship.y + ship.height/2f
                )

                //If ship hits left or right boundary, reverse direction
                if(ship.x - ship.width/2f < 0f || ship.x + ship.width/2f > width) {
                    ship.velocityX *= -1

//                    val newShipAngle = Math.toDegrees(Math.atan2(ship.velocityY.toDouble(), ship.velocityX.toDouble())).toFloat()
//                    ship.shipAngle = newShipAngle

                }

                //If ship hits top or bottom boundary of anti-gravity zone, reverse direction
                if(ship.antiGravity && (ship.y - ship.height/2f < 0f || ship.y + ship.height/2f > height/2f)) {
                    ship.velocityY *= -1

//                    val newShipAngle = Math.toDegrees(Math.atan2(ship.velocityY.toDouble(), ship.velocityX.toDouble())).toFloat()
//                    ship.shipAngle = newShipAngle
                }
            }else if(ship.launched){
                //Stop the movement of ships if under .1 velocity
                if(Math.abs(ship.velocityX) > 0f) ship.velocityX = 0f
                if(Math.abs(ship.velocityY) > 0f) ship.velocityY = 0f
            }

            if(ship.antiGravity) {

                //Slowly decrease the ships velocity until it comes to a stop (anti-gravity with friction)
                ship.velocityX *= 0.97f
                ship.velocityY *= 0.97f

                //val shipRect = ship.getRect()

                val newCells = mutableSetOf<GridCell>()


                //Update the grids the current ship belongs to
                for (row in grid) {
                    for (cell in row) {
                        if (RectF.intersects(cell.rect, ship.rect)) {
                            newCells.add(cell)
                        }
                    }
                }

                // Remove ship from previous cells
                for(cell in ship.currentCells){
                    if(!newCells.contains(cell)) {
                        cell.ships.remove(ship)
                    }
                }

                for(cell in newCells) {
                    cell.ships.add(ship)
                }

                ship.currentCells = newCells


                //After every move, check to make sure ships remain in bounds. If not, force them to
                val minY = ship.height/2f
                val maxY = height/2f - ship.height/2f
                val minX = ship.width/2f
                val maxX = width - ship.width/2f

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

                ship.rect.set(
                    ship.x - ship.width/2f,
                    ship.y - ship.height/2f,
                    ship.x + ship.width/2f,
                    ship.y + ship.height/2f
                )
            }
        }
    }

    //Checks for any ship collisions
    fun checkCollisions(){

        //Stores two pairs of <row, col> to mark two cells as already compared and avoid duplicate comparisons
        val comparedCells = mutableSetOf<Pair<Pair<Int, Int>, Pair<Int, Int>>>()

        val shipsToRemove = mutableListOf<Spaceship>()
        val shipsToAdd = mutableListOf<Spaceship>()

        for(row in grid){
            for(cell in row){

                //Only check visible and moving ships
                val visibleShipsInCell = cell.ships.filter{it.visible}

                if(visibleShipsInCell.size < 2) continue

                for(i in visibleShipsInCell.indices){
                    val ship1 = visibleShipsInCell[i]
                    for(j in i+1 until visibleShipsInCell.size){
                        val ship2 = visibleShipsInCell[j]

                        if(ship1.visible && ship2.visible && ship1 != ship2 && RectF.intersects(ship1.rect, ship2.rect)){
                            Log.w("MainActivity", "Collision detected")
                            if(ship1.tier == ship2.tier){
                                mergeShips(ship1, ship2, shipsToRemove, shipsToAdd)
                            }else{
                                bounceShips(ship1, ship2)
                            }

                        }
                    }
                }

//                for(adjacentCell in cell.adjacentCells){
//
//                    //If two cells have already been compared, skip duplicate comparison
//                    if(comparedCells.contains(Pair(Pair(cell.row, cell.col), Pair(adjacentCell.row, adjacentCell.col)))) continue
//
//                    val visibleShipsInAdjacentCell = adjacentCell.ships.filter{it.visible}
//
//                    for(ship1 in visibleShipsInCell){
//                        for(ship2 in visibleShipsInAdjacentCell){
//                            if(ship1.visible && ship2.visible && ship1 != ship2 && RectF.intersects(ship1.getRect(), ship2.getRect())){
//                                Log.w("MainActivity", "Collision detected")
//                                if(ship1.tier == ship2.tier){
//                                    mergeShips(ship1, ship2, shipsToRemove, shipsToAdd)
//                                }else{
//                                    bounceShips(ship1, ship2)
//                                }
//                            }
//                        }
//                    }
//
//                    //Add both orderings of the two cells to the comparedCells set
//                    comparedCells.add(Pair(Pair(cell.row, cell.col), Pair(adjacentCell.row, adjacentCell.col)))
//                    comparedCells.add(Pair(Pair(adjacentCell.row, adjacentCell.col), Pair(cell.row, cell.col)))
//                }
            }
        }

        //Remove old ships and update cells
        for(ship in shipsToRemove){
            spaceships.remove(ship)
            for(cell in ship.currentCells) {
                cell.ships.remove(ship)
            }
        }

        //Add merged ships and update cells
        for(ship in shipsToAdd){
            spaceships.add(ship)

            //val shipRect = ship.getRect()

            val newCells = mutableSetOf<GridCell>()

            for(row in grid){
                for(cell in row){
                    if(RectF.intersects(cell.rect, ship.rect)){
                        newCells.add(cell)
                        cell.ships.add(ship)
                    }
                }
            }
            ship.currentCells = newCells
        }
    }



    // Defines a spaceship
    data class Spaceship(
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
        )
    ){
//        fun getRect() : RectF{
//            return RectF(
//                x - width / 2f,
//                y - height / 2f,
//                x + width / 2f,
//                y + height / 2f
//            )
//        }
    }

    //Defines a grid cell, which is one portion of the anti-gravity zone where ships will exist
    class GridCell(
        val row: Int,
        val col: Int,

        //Stores the grid cell's position in the gameView
        val rect: RectF,

        //Stores references to all ships in the current grid cell
        val ships: MutableSet<Spaceship> = mutableSetOf(),

        //Stores all adjacent cells
        val adjacentCells: MutableSet<GridCell> = mutableSetOf()
    )
}