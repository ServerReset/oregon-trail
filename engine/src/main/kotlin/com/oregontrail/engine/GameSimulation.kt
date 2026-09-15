package com.oregontrail.engine

import kotlin.math.max
import kotlin.math.min

internal fun Game.continueOnTrail() {
        if (aliveCount == 0) { dieOf("the trail"); return }
        val msgs = ArrayList<String>()
        var result = DayResult.OK
        var guard = 0
        while (guard++ < 90) {
            result = simulateDay(msgs)
            if (result != DayResult.OK) break
        }
        when (result) {
            DayResult.LANDMARK -> advanceToLandmark(msgs)
            DayResult.DEATH -> dieOf(deathCause.ifEmpty { "disease" })
            else -> {
                if (msgs.isEmpty()) msgs.add("The trail is long and quiet.")
                showNotice("On the Trail", msgs, Phase.TRAVEL, lastEventArt)
                lastEventArt = null
            }
        }
    }

internal fun Game.nextLandmark(): Landmark? {
        if (cutoffTarget > landmarkIndex) return Data.landmarkAt(cutoffTarget)
        val idx = landmarkIndex + 1
        return if (idx <= Data.landmarks.lastIndex) Data.landmarkAt(idx) else null
    }

internal fun Game.simulateDay(msgs: MutableList<String>): DayResult {
        if (aliveCount == 0) { deathCause = "disease"; return DayResult.DEATH }
        date.plusDays(1)
        rollWeather()

        // Eat.
        if (!consumeFood()) {
            msgs.add("There is no food left. The party is weak and hungry.")
            aliveMembers().forEach { it.hurt(rng.nextInt(4, 10)) }
            if (aliveCount == 0) { deathCause = "starvation"; return DayResult.DEATH }
        }

        // Make miles.
        val weatherFactor = weatherSpeedFactor(weather.kind)
        val oxenYokes = inventory.oxen / 2
        val oxenFactor = if (oxenYokes <= 0) 0.0 else min(1.2, 0.6 + 0.2 * oxenYokes)

        // Hard driving and harsh weather wear the oxen down (a cut feature the
        // original designer wished he could have tracked).
        when (pace) {
            Pace.STRENUOUS -> oxHealth -= rng.nextInt(0, 2)
            Pace.GRUELING -> oxHealth -= rng.nextInt(1, 3)
            else -> {}
        }
        if (weather.tempF >= 90 || weather.tempF <= 25) oxHealth -= 1
        if (inventory.food <= 0) oxHealth -= 1
        oxHealth = oxHealth.coerceIn(0, 100)
        val oxHealthFactor = 0.55 + 0.45 * oxHealth / 100.0

        val terrain = terrainFactor(Data.landmarkAt(landmarkIndex).kind)
        var gained = (pace.milesPerDay * weatherFactor * oxenFactor * terrain * oxHealthFactor).toInt()
        if (inventory.oxen <= 0) gained = 0
        if (oxHealth <= 15 && inventory.oxen >= 2 && rng.chance(0.12)) {
            inventory.oxen -= 2
            oxHealth = 45
            msgs.add("An exhausted ox collapses. You are down a yoke.")
        }
        miles += max(0, gained)

        // Ill health from hard travel.
        if (pace == Pace.GRUELING) aliveMembers().forEach { it.hurt(rng.nextInt(0, 3)) }

        // Illness.
        if (checkIllness(msgs)) { clampMiles(); return stopOrDeath() }

        // Random event.
        if (rng.chance(difficulty.eventChance)) {
            val before = msgs.size
            rollEvent(msgs)
            msgs.getOrNull(before)?.let { addJournal(it) }
            clampMiles()
            return stopOrDeath()
        }

        clampMiles()

        // Reached next landmark?
        val next = nextLandmark()
        if (next != null && miles >= next.mile) {
            // Do not overshoot badly.
            miles = next.mile
            return DayResult.LANDMARK
        }

        return DayResult.OK
    }

