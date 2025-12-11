package com.example.spaceship_merge

import java.util.TimerTask
// The timer task which runs constantly to update game state
class GameTimerTask : TimerTask {
    // References to the GameActivity which will be updated
    private lateinit var activity : GameActivity
    // Stores the reference to GameActivity
    constructor(activity : GameActivity){
        this.activity = activity
    }
    // Called by the Timer at a fixed interval
    override fun run(){
        // Updates the game view
        activity.updateGameView()
    }
}