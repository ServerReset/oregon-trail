package com.oregontrail.engine

import kotlin.math.max
import kotlin.math.min
import kotlin.math.abs

    /** Miles traveled can never fall behind the last landmark reached. */
internal fun Game.clampMiles() {
        val floor = Data.landmarkAt(landmarkIndex).mile
        if (miles < floor) miles = floor
        if (miles < 0) miles = 0
    }



internal fun Game.stopOrDeath(): DayResult {
        return if (aliveCount == 0) {
            if (deathCause.isEmpty()) deathCause = "disease"
            DayResult.DEATH
        } else DayResult.EVENT
    }



internal fun Game.consumeFood(): Boolean {
        val needed = rations.poundsPerPersonPerDay * aliveCount
        if (inventory.food >= needed) {
            inventory.food -= needed
            return true
        }
        inventory.food = 0
        return false
    }
