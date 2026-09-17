package com.oregontrail.engine

/** A single row of scenery with its own colour. */
data class ArtRow(val text: String, val fg: Palette)

/**
 * Layered scenery for the travel and landmark screens. Each piece is drawn
 * with several colours (snow, rock, grass, pines, water) so the trail has some
 * depth instead of a single flat colour. Rows are raw strings so the many
 * backslashes and quotes in the art need no escaping.
 */
object Landscape {

    val plains: List<ArtRow> = listOf(
        ArtRow("", Palette.GREEN),
        ArtRow("""                 _.-''''-._             _.-''''-._""", Palette.DIM),
        ArtRow("""           _.-'''         '''-._     _.-''         ''-._""", Palette.DIM),
        ArtRow("""     _.-''                       '-.''                ''-._""", Palette.GREEN),
        ArtRow(""" _.-'                                                     '-._""", Palette.GREEN),
        ArtRow("""__/________________________________________________________\__""", Palette.BROWN),
        ArtRow(""" ,,  ,,  ,,  ,,  ,,  ,,  ,,  ,,  ,,  ,,  ,,  ,,  ,,  ,,  ,,""", Palette.YELLOW),
        ArtRow("""    '         '         '         '         '         '""", Palette.YELLOW),
    )

    val peaks: List<ArtRow> = listOf(
        ArtRow("", Palette.GRAY),
        ArtRow("""                          _/\_""", Palette.BRIGHT_WHITE),
        ArtRow("""            _/\_         /    \        _/\_""", Palette.BRIGHT_WHITE),
        ArtRow("""           /    \       /      \      /    \""", Palette.GRAY),
        ArtRow("""          /      \     /        \    /      \""", Palette.GRAY),
        ArtRow("""         /        \___/          \__/        \""", Palette.GRAY),
        ArtRow("""        /                                      \""", Palette.DIM),
        ArtRow("""_______/___________________________________________\_____""", Palette.GREEN),
        ArtRow("""   ^^^       ^^^^         ^^^^^^         ^^^^       ^^^""", Palette.GREEN),
    )

    val forest: List<ArtRow> = listOf(
        ArtRow("", Palette.GREEN),
        ArtRow("""        ^^      ^^^      ^^      ^^^      ^^      ^^^""", Palette.DIM),
        ArtRow("""       ^^^^    ^^^^^    ^^^^    ^^^^^    ^^^^    ^^^^^""", Palette.GREEN),
        ArtRow("""      ^^^^^^  ^^^^^^^  ^^^^^^  ^^^^^^^  ^^^^^^  ^^^^^^^""", Palette.GREEN),
        ArtRow("""       ||||    |||||    ||||    ||||||   ||||    |||||""", Palette.BROWN),
        ArtRow("""        ||      |||      ||      |||      ||      |||""", Palette.BROWN),
        ArtRow("""____,,______,,_______,,______,,_______,,______,,______,,____""", Palette.BROWN),
        ArtRow("""    ^^        ^^         ^^         ^^         ^^""", Palette.DIM),
    )

    val river: List<ArtRow> = listOf(
        ArtRow("", Palette.GREEN),
        ArtRow("""  \                                                  /""", Palette.GREEN),
        ArtRow("""   \    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~  /""", Palette.CYAN),
        ArtRow("""    \  ~~~ ~~~~ ~~~ ~~~~ ~~~ ~~~~ ~~~ ~~~~ ~~~ ~~~ /""", Palette.CYAN),
        ArtRow("""     \~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~/""", Palette.CYAN),
        ArtRow("""      \~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~/""", Palette.BLUE),
        ArtRow("""       \________________________________________/""", Palette.BROWN),
        ArtRow("""   ,,     ,,       ,,        ,,       ,,     ,,   ,,""", Palette.GREEN),
    )

    val town: List<ArtRow> = listOf(
        ArtRow("", Palette.GREEN),
        ArtRow("""              _.-'''-._        _.-'''-._""", Palette.DIM),
        ArtRow("""        _.-'''       '''-.__.-''       ''-._""", Palette.DIM),
        ArtRow("""             ______""", Palette.YELLOW),
        ArtRow("""     _______/      \     _____      _____""", Palette.YELLOW),
        ArtRow("""    |  []  |  []   |   |     |    |     |""", Palette.GRAY),
        ArtRow("""    |  []  |  []   |___|  [] |____|  [] |""", Palette.GRAY),
        ArtRow("""__/______________________________________\__""", Palette.BROWN),
    )

    val fort: List<ArtRow> = listOf(
        ArtRow("", Palette.GREEN),
        ArtRow("""    _|_    _|_    _|_    _|_    _|_    _|_""", Palette.BROWN),
        ArtRow("""   |   |  |   |  |   |  |   |  |   |  |   |""", Palette.BROWN),
        ArtRow(""" __|___|__|___|__|___|__|___|__|___|__|___|__""", Palette.BROWN),
        ArtRow("""|                                            |""", Palette.GRAY),
        ArtRow("""|___________[ FORT ]_________________________|""", Palette.GRAY),
        ArtRow("""|____________________________________________|""", Palette.GRAY),
        ArtRow("""___|______________________________________|___""", Palette.BROWN),
    )

    val bluffs: List<ArtRow> get() = LandscapeExtra.bluffs
    val desert: List<ArtRow> get() = LandscapeExtra.desert
    val snow: List<ArtRow> get() = LandscapeExtra.snow
    val cascade: List<ArtRow> get() = LandscapeExtra.cascade

    /** Scenery for the open trail, based on how far along you are. */
    fun travelScene(miles: Int): List<ArtRow> = when {
        miles < 300 -> plains
        miles < 650 -> bluffs
        miles < 1000 -> peaks
        miles < 1300 -> desert
        miles < 1550 -> forest
        miles < 1800 -> snow
        else -> cascade
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
