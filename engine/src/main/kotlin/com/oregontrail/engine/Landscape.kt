package com.oregontrail.engine

/** A single row of scenery with its own colour. */
data class ArtRow(val text: String, val fg: Palette)

/**
 * Layered scenery for the travel and landmark screens. Each piece is drawn
 * with several colours (snow, rock, pines, grass, water) so the trail has some
 * depth instead of a single flat colour. Rows are raw strings so the many
 * backslashes and quotes in the art need no escaping.
 */
object Landscape {

    val plains: List<ArtRow> = listOf(
        ArtRow("", Palette.GREEN),
        ArtRow("", Palette.GREEN),
        ArtRow("""                 _.-'''-.            _.-'''-.""", Palette.DIM),
        ArtRow("""           _.-'''       '''-._  _.-''        ''-._""", Palette.DIM),
        ArtRow("""     _.-'''                 '''-''              ''-._""", Palette.GREEN),
        ArtRow(""" _.-'                                                 ''-._""", Palette.GREEN),
        ArtRow("""__/__________________________________________________________\__""", Palette.BROWN),
        ArtRow("""  ~ ~~ ~ ~~~ ~ ~~ ~ ~~~ ~ ~~ ~ ~~~ ~ ~~ ~ ~~~ ~ ~~ ~ ~~~ ~ ~""", Palette.YELLOW),
        ArtRow("""        ''                '' """, Palette.YELLOW)
    )

    val peaks: List<ArtRow> = listOf(
        ArtRow("""                          _..--.._""", Palette.BRIGHT_WHITE),
        ArtRow("""            _..--.._   .-'        '-.     _..--.._""", Palette.BRIGHT_WHITE),
        ArtRow("""          .'        '. /              \  /        \""", Palette.GRAY),
        ArtRow("""         /            Y                V            \""", Palette.GRAY),
        ArtRow("""        /              \                            \""", Palette.GRAY),
        ArtRow("""       /                \                            \""", Palette.DIM),
        ArtRow("""      /                  \                            \""", Palette.DIM),
        ArtRow("""_____/________________________________________________\_____""", Palette.GREEN),
        ArtRow("""         ^^          ^^^^          ^^          ^^^^ """, Palette.GREEN)
    )

    val forest: List<ArtRow> = listOf(
        ArtRow("", Palette.GREEN),
        ArtRow("""       ^^      ^^^      ^^      ^^^      ^^""", Palette.GREEN),
        ArtRow("""      ^^^^    ^^^^^    ^^^^    ^^^^^    ^^^^""", Palette.GREEN),
        ArtRow("""     ^^^^^^  ^^^^^^^  ^^^^^^  ^^^^^^^  ^^^^^^""", Palette.GREEN),
        ArtRow("""      ||||    |||||    ||||    ||||||   ||||""", Palette.BROWN),
        ArtRow("""       ||      |||      ||      |||      ||""", Palette.BROWN),
        ArtRow("""___,,______,,_______,,______,,_______,,______,,___""", Palette.BROWN),
        ArtRow("""   ^^        ^^         ^^         ^^        ^^""", Palette.DIM)
    )

    val river: List<ArtRow> = listOf(
        ArtRow("", Palette.GREEN),
        ArtRow("""  \                                              /""", Palette.GREEN),
        ArtRow("""   \  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~  /""", Palette.CYAN),
        ArtRow("""    \ ~~~ ~~~~ ~~~ ~~~~ ~~~ ~~~~ ~~~ ~~~~ ~~~ /""", Palette.CYAN),
        ArtRow("""     \~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~/""", Palette.BLUE),
        ArtRow("""      \____________________________________/""", Palette.BROWN)
    )

    val town: List<ArtRow> = listOf(
        ArtRow("", Palette.GREEN),
        ArtRow("""            ______""", Palette.YELLOW),
        ArtRow("""           /      \     _____""", Palette.YELLOW),
        ArtRow("""    _____ |  []  |    |     |    _____""", Palette.GRAY),
        ArtRow("""   |     ||  []  |    |  [] |   |     |""", Palette.GRAY),
        ArtRow("""   |_[]__||_[]__|____|_[]__|___|_[]__|""", Palette.GRAY),
        ArtRow("""__/____________________________________\__""", Palette.BROWN)
    )

    val fort: List<ArtRow> = listOf(
        ArtRow("", Palette.GREEN),
        ArtRow("""     _|_    _|_    _|_    _|_    _|_""", Palette.BROWN),
        ArtRow("""    |   |  |   |  |   |  |   |  |   |""", Palette.BROWN),
        ArtRow("""  __|___|__|___|__|___|__|___|__|___|__""", Palette.BROWN),
        ArtRow(""" |                                    |""", Palette.GRAY),
        ArtRow(""" |___________[ FORT ]_________________|""", Palette.GRAY),
        ArtRow("""___|________________________________|___""", Palette.BROWN)
    )

    /** Scenery for the open trail, based on how far along you are. */
    fun travelScene(miles: Int): List<ArtRow> = when {
        miles < 700 -> plains
        miles < 1200 -> peaks
        miles < 1600 -> forest
        else -> peaks
    }

    /** Picks scenery for the current stretch of trail. */
    fun forKind(kind: LandmarkKind, miles: Int): List<ArtRow> = when (kind) {
        LandmarkKind.MOUNTAINS -> peaks
        LandmarkKind.RIVER -> river
        LandmarkKind.FORT -> fort
        LandmarkKind.START -> town
        LandmarkKind.END -> town
        else -> if (miles > 1250) forest else plains
    }
}

/** Draws coloured scenery rows starting at (x, y). Returns the next row. */
internal fun Screen.drawScene(rows: List<ArtRow>, x: Int, y: Int): Int {
    rows.forEachIndexed { i, row ->
        if (row.text.isNotEmpty()) text(x, y + i, row.text, row.fg)
    }
    return y + rows.size
}

/** The width of the widest scenery row. */
internal fun sceneWidth(rows: List<ArtRow>): Int = rows.maxOfOrNull { it.text.length } ?: 0
