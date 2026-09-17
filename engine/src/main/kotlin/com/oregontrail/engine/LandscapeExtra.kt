package com.oregontrail.engine

/**
 * Extra scenery pieces that slot into [Landscape.travelScene] as the wagon
 * train moves west: rocky badlands, sage desert, snowy hills and the steep
 * forested Cascades. Kept apart from [Landscape] so each file stays small.
 */
internal object LandscapeExtra {

    val bluffs: List<ArtRow> = listOf(
        ArtRow("", Palette.BROWN),
        ArtRow("""                  _____                   _______""", Palette.DIM),
        ArtRow("""                 /     \        _________/       \""", Palette.DIM),
        ArtRow("""       _________/       \______/                  \_______""", Palette.GRAY),
        ArtRow("""      /         \      /       \                 /        \""", Palette.GRAY),
        ArtRow("""_____/___________\____/_________\______________/__________\__""", Palette.BROWN),
        ArtRow("""  ,,   ,,   ,,   ,,    ,,    ,,   ,,    ,,   ,,    ,,   ,,   ,""", Palette.YELLOW),
        ArtRow("""    '      '       '       '       '       '       '       '""", Palette.YELLOW),
    )

    val desert: List<ArtRow> = listOf(
        ArtRow("", Palette.YELLOW),
        ArtRow("""                         ___                       ___""", Palette.DIM),
        ArtRow("""          __            /   \          __         /   \""", Palette.DIM),
        ArtRow("""         /  \          /     \        /  \       /     \""", Palette.DIM),
        ArtRow("""        /    \________/       \______/    \_____/       \""", Palette.YELLOW),
        ArtRow("""______/____________________________________________________\__""", Palette.BROWN),
        ArtRow("""   ,,      ,,        ,,       ,,      ,,      ,,      ,,  ,,""", Palette.YELLOW),
        ArtRow("""  .    .       .        .        .        .        .       .""", Palette.YELLOW),
    )

    val snow: List<ArtRow> = listOf(
        ArtRow("", Palette.BRIGHT_WHITE),
        ArtRow("""               _.-''''-.            _.-''''-.""", Palette.BRIGHT_WHITE),
        ArtRow("""         _.-'''       ''-.___    _.-''       ''-._""", Palette.BRIGHT_WHITE),
        ArtRow("""     _.-'                     '-./                '-._""", Palette.WHITE),
        ArtRow("""   /                                                \""", Palette.DIM),
        ArtRow("""__/__________________________________________________\__""", Palette.BRIGHT_WHITE),
        ArtRow("""    |        |         |         |         |        |""", Palette.BROWN),
        ArtRow("""   -+-      -+-       -+-       -+-       -+-      -+-""", Palette.BROWN),
    )

    val cascade: List<ArtRow> = listOf(
        ArtRow("", Palette.GRAY),
        ArtRow("""                    _/\_          _/\_""", Palette.BRIGHT_WHITE),
        ArtRow("""                   /    \        /    \""", Palette.BRIGHT_WHITE),
        ArtRow("""        _/\_     /      \      /      \     _/\_""", Palette.GRAY),
        ArtRow("""       /    \   /        \    /        \   /    \""", Palette.GRAY),
        ArtRow("""      /      \_/          \__/          \_/      \""", Palette.DIM),
        ArtRow("""     /                                              \""", Palette.DIM),
        ArtRow("""   ^^     ^^^^     ^^^^^^     ^^^^^^     ^^^^     ^^""", Palette.GREEN),
        ArtRow("""_____/________________________________________________\____""", Palette.BROWN),
    )
}
