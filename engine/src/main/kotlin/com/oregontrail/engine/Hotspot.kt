package com.oregontrail.engine

/** A rectangular, tappable region on the screen. Coordinates are inclusive. */
data class Hotspot(
    val id: String,
    val x0: Int,
    val y0: Int,
    val x1: Int,
    val y1: Int
) {
    fun contains(x: Int, y: Int): Boolean = x in x0..x1 && y in y0..y1
}
