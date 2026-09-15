package com.oregontrail.engine


internal fun Game.showNotice(title: String, lines: List<String>, next: Phase, art: List<String>? = null) {
        noticeTitle = title
        noticeLines.clear()
        noticeLines.addAll(lines)
        noticeNext = next
        noticeArt = art
        phase = Phase.NOTICE
    }

    /** ASCII illustration for an event, shown on the notice screen. */
fun Game.eventArtFor(eventId: String): List<String>? = when (eventId) {
        "breakdown" -> AsciiEvents.wheel
        "bandits" -> AsciiEvents.bandit
        "snakebite" -> AsciiEvents.snake
        "wild_animals" -> AsciiEvents.wolf
        "indians" -> AsciiEvents.teepee
        "fruit" -> AsciiEvents.bush
        "fire" -> AsciiEvents.fireArt
        "riders" -> AsciiEvents.horses
        "stranded" -> AsciiEvents.brokenWagon
        "heavy_rain", "hail", "thunderstorm" -> AsciiSky.cloud
        "berries", "fruit" -> AsciiEvents.bush
        "prairie_dogs" -> AsciiEvents.prairieDog
        "rainbow" -> AsciiEvents.rainbow
        "hot_springs" -> AsciiEvents.spring
        "abandoned_wagon" -> AsciiEvents.brokenWagon
        "wild_horses" -> AsciiEvents.horses
        "prairie_fire" -> AsciiEvents.fireArt
        "mirage" -> AsciiEvents.spring
        "buffalo_herd" -> AsciiEvents.buffalo
        "fiddle_night" -> AsciiEvents.fiddle
        else -> null
    }

internal fun Game.wrapString(s: String, width: Int): List<String> {
        if (width < 4) return listOf(s)
        val out = ArrayList<String>()
        var line = StringBuilder()
        for (word in s.split(' ')) {
            when {
                line.isEmpty() && word.length > width -> {
                    var w = word
                    while (w.length > width) {
                        out.add(w.substring(0, width))
                        w = w.substring(width)
                    }
                    line.append(w)
                }
                line.isEmpty() -> line.append(word)
                line.length + 1 + word.length <= width -> line.append(' ').append(word)
                else -> {
                    out.add(line.toString())
                    line = StringBuilder(word)
                }
            }
        }
        if (line.isNotEmpty()) out.add(line.toString())
        if (out.isEmpty()) out.add("")
        return out
    }

internal fun Game.suppliesLines(): List<String> {
        val yokes = inventory.oxen / 2
        return listOf(
            "Cash: $${"%.2f".format(inventory.cash)}",
            "Oxen: ${inventory.oxen} ($yokes yoke, ${oxCondition()})",
            "Food: ${inventory.food} pounds",
            "Clothing: ${inventory.clothing} sets",
            "Ammunition: ${inventory.ammo / 20} boxes (${inventory.ammo} bullets)",
            "Spare parts: ${inventory.wheels} wheels, ${inventory.axles} axles, ${inventory.tongues} tongues",
            "",
            "Party health:",
            *party.map { "  ${it.name}: ${it.state.displayName}" }.toTypedArray()
        )
    }

    // ====================================================================
    //  Rendering (see GameRender.kt)
    // ====================================================================
