package com.oregontrail.engine

enum class AnimalKind(
    val glyph: Char,
    val displayName: String,
    val meat: Int,
    val color: Palette
) {
    SQUIRREL('s', "squirrel", 2, Palette.BROWN),
    RABBIT('r', "rabbit", 2, Palette.WHITE),
    FOX('f', "fox", 5, Palette.YELLOW),
    DEER('d', "deer", 45, Palette.BROWN),
    ANTELOPE('a', "antelope", 40, Palette.YELLOW),
    BEAR('B', "bear", 80, Palette.RED),
    WOLF('w', "wolf", 30, Palette.GRAY),
    BUFFALO('b', "buffalo", 100, Palette.WHITE)
}
