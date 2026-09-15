package com.oregontrail.engine

internal fun Game.rollEvent(msgs: MutableList<String>) {
        val pool = eventPool(miles)
        val event = pool[rng.nextInt(pool.size)]
        lastEventArt = eventArtFor(event)
        applyEvent(event, msgs)
    }

internal fun Game.eventPool(m: Int): List<String> {
        val plains = listOf(
            "breakdown", "breakdown", "ox_lame", "ox_wander", "child_lost",
            "child_arm", "unsafe_water", "heavy_rain", "hail", "bandits",
            "wild_animals", "fire", "fog", "indians", "thief", "fruit", "riders",
            "stranded", "berries", "prairie_dogs", "rainbow", "abandoned_wagon"
        )
        val mountains = listOf(
            "breakdown", "ox_lame", "ox_wander", "unsafe_water", "heavy_rain",
            "hail", "bandits", "wild_animals", "fire", "fog", "snakebite",
            "cold", "blizzard", "indians", "riders", "stranded",
            "berries", "prairie_dogs", "rainbow", "hot_springs"
        )
        return if (m > 900) mountains else plains
    }

internal fun Game.applyEvent(event: String, msgs: MutableList<String>) {
        when (event) {
            "breakdown" -> evBreakdown(msgs)
            "ox_lame" -> evOxLame(msgs)
            "ox_wander" -> evOxWander(msgs)
            "child_lost" -> evChildLost(msgs)
            "child_arm" -> evChildArm(msgs)
            "unsafe_water" -> evUnsafeWater(msgs)
            "heavy_rain" -> evHeavyRain(msgs)
            "hail" -> evHail(msgs)
            "bandits" -> evBandits(msgs)
            "wild_animals" -> evWildAnimals(msgs)
            "fire" -> evFire(msgs)
            "fog" -> evFog(msgs)
            "snakebite" -> evSnakebite(msgs)
            "cold" -> evCold(msgs)
            "blizzard" -> evBlizzard(msgs)
            "indians" -> evIndians(msgs)
            "thief" -> evThief(msgs)
            "fruit" -> evFruit(msgs)
            "berries" -> evBerries(msgs)
            "prairie_dogs" -> evPrairieDogs(msgs)
            "rainbow" -> evRainbow(msgs)
            "hot_springs" -> evHotSprings(msgs)
            "abandoned_wagon" -> evAbandonedWagon(msgs)
            "stranded" -> startStrandedChoice()
            "riders" -> startRidersChoice()
        }
    }

    /** A moral encounter: help a family in need, at a cost. */
internal fun <T> List<T>.randomOrNull(rng: Rng): T? =
    if (isEmpty()) null else this[rng.nextInt(size)]
