package com.oregontrail.engine

/** Read-only views of a [HuntField] used by the renderer. */
fun HuntField.decorationAt(x: Int, y: Int): Char? {
    for (d in decorations) if (d.first == x && d.second == y) return d.third
    return null
}

fun HuntField.animalGlyphAt(x: Int, y: Int): Pair<Char, Palette>? =
    animalAt(x, y)?.let { it.kind.glyph to it.kind.color }

fun HuntField.bulletAt(x: Int, y: Int): Boolean = bullets.any { it.x == x && it.y == y }

val HuntField.isFull: Boolean get() = meat >= carryLimit
