package com.oregontrail.engine

class Inventory(
    var cash: Double = 0.0,
    var oxen: Int = 0,          // single oxen; bought in yokes of two
    var food: Int = 0,          // pounds
    var clothing: Int = 0,      // sets
    var ammo: Int = 0,          // bullets
    var wheels: Int = 0,
    var axles: Int = 0,
    var tongues: Int = 0
) {
    fun spareCount(part: Part): Int = when (part) {
        Part.WHEEL -> wheels
        Part.AXLE -> axles
        Part.TONGUE -> tongues
    }

    fun useSpare(part: Part): Boolean {
        return when (part) {
            Part.WHEEL -> if (wheels > 0) { wheels--; true } else false
            Part.AXLE -> if (axles > 0) { axles--; true } else false
            Part.TONGUE -> if (tongues > 0) { tongues--; true } else false
        }
    }

    fun addSpare(part: Part) {
        when (part) {
            Part.WHEEL -> wheels++
            Part.AXLE -> axles++
            Part.TONGUE -> tongues++
        }
    }

    fun copy(): Inventory = Inventory(cash, oxen, food, clothing, ammo, wheels, axles, tongues)
}
