package com.oregontrail.engine

/** Camp and the named landmarks along the trail. */
internal object AsciiLandmarks {
    val camp: List<String> = listOf(
        "   *    .        *     .",
        "        .    *        .",
        "           ___",
        "  *      .'   '.   *",
        "        /  ___  \\",
        "       |  /   \\  |",
        "        \\ \\___/ /",
        "     ____'.___.'____",
        "    |_______________|",
        "   .    (@)   (@)    .",
        "       \\  |  /",
        "        \\ | /",
        "       .--*--.",
        "        \\___/"
    )

    /** The second campfire frame, for a flickering flame. */
    val campFlicker: List<String> = listOf(
        "   .    *        .     *",
        "        *    .        *",
        "           ___",
        "  .      .'   '.   .",
        "        /  ___  \\",
        "       |  /   \\  |",
        "        \\ \\___/ /",
        "     ____'.___.'____",
        "    |_______________|",
        "   *    (@)   (@)    *",
        "        \\ /|\\ /",
        "         \\ | /",
        "        .-*.*-.",
        "         \\_/_/"
    )

    /** Chimney Rock: a tall clay spire. */
    val chimneyRock: List<String> = listOf(
        "          ___",
        "         / _ \\",
        "        | | | |",
        "        | |_| |",
        "       /   |   \\",
        "      /    |    \\",
        "     /     |     \\",
        "    /______|______\\",
        "   /_______________\\"
    )

    /** South Pass: a broad gap in the Rockies. */
    val southPass: List<String> = listOf(
        "      /\\              /\\",
        "     /  \\    ____    /  \\",
        "    /    \\  /    \\  /    \\",
        "   /      \\/      \\/      \\",
        "  /________________________\\"
    )

    /** Independence, Missouri: a town of cabins. */
    val town: List<String> = listOf(
        "    _____      _____",
        "   |  _  |    |  _  |",
        "   | |_| |    | |_| |",
        "   |  |  |____|  |  |",
        "   |__|__|____|__|__|",
        "     |  |      |  |"
    )

    /** The Dalles: cliffs and a river gorge. */
    val dalles: List<String> = listOf(
        "   /|                  |\\",
        "  / |     ~~~~~~~~     | \\",
        " /  |   ~~~~~~~~~~~~   |  \\",
        "/___|__________________|___\\"
    )

    /** The Willamette Valley: pines, a cabin and a river. */
    val valley: List<String> = listOf(
        "     /\\        /\\        /\\",
        "    /  \\      /  \\      /  \\",
        "   /    \\    /    \\    /    \\",
        "  /______\\  /______\\  /______\\",
        "         ___________",
        "        |  [] [] [] |",
        "        |___________|",
        "   ~~~~~~~~~~~~~~~~~~~~~~~~"
    )

    /** A few twinkling stars for title/sky decoration. */
}
