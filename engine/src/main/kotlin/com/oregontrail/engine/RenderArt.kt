package com.oregontrail.engine


/** A flowing river: the wave pattern shifts each frame. */
internal fun riverArt(frame: Int): List<String> = AsciiScenery.river.mapIndexed { r, line ->
    val sb = StringBuilder()
    for ((c, ch) in line.withIndex()) {
        if (ch == '~') {
            sb.append(if (((c / 2 + r + frame / 2) % 2) == 0) '~' else ' ')
        } else {
            sb.append(ch)
        }
    }
    sb.toString()
}

/** A flickering campfire. */
internal fun campArt(frame: Int): List<String> =
    if (frame % 2 == 0) AsciiLandmarks.camp else AsciiLandmarks.campFlicker

/** Stars that twinkle. */
internal fun starsArt(frame: Int): List<String> = AsciiSky.stars.mapIndexed { r, line ->
    val sb = StringBuilder()
    for ((c, ch) in line.withIndex()) {
        if (ch == '*' && (c + r + frame) % 3 == 0) sb.append('.') else sb.append(ch)
    }
    sb.toString()
}
