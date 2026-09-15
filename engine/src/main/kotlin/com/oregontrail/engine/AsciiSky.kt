package com.oregontrail.engine

/** Sun, clouds and stars. */
internal object AsciiSky {
    val stars: List<String> = listOf(
        "  *      .        *      .     *",
        "      .       *        ."
    )

    // ------------------------------------------------------------------
    // Sky props (animated)
    // ------------------------------------------------------------------
    val sun: List<String> = listOf(
        "\\ | /",
        "-(o)-",
        "/ | \\"
    )

    /** One frame of a small cloud. */
    val cloud: List<String> = listOf("( .. )")

    // ------------------------------------------------------------------
    // Event illustrations
    // ------------------------------------------------------------------
}
