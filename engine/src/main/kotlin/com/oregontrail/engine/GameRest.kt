package com.oregontrail.engine

import kotlin.math.max
import kotlin.math.min
import kotlin.math.abs

    /** The original let you choose how long to rest. */
internal fun Game.startRest() {
        choiceTitle = "Rest"
        choiceLines.clear()
        choiceLines.add("How long should the party rest?")
        choiceLines.add("Longer rest heals more, but eats food and costs time.")
        choiceOptions.clear()
        choiceOptions.add("1 day" to "rest:1")
        choiceOptions.add("2 days" to "rest:2")
        choiceOptions.add("3 days" to "rest:3")
        choiceOptions.add("5 days" to "rest:5")
        choiceNext = Phase.TRAVEL
        phase = Phase.CHOICE
        pendingSound = Sound.SELECT
    }


internal fun Game.rest(days: Int) {
        var report = "You camp and rest for $days day(s)."
        repeat(days) {
            date.plusDays(1)
            rollWeather()
            consumeFood()
        }
        aliveMembers().forEach { it.heal(days * 4) }
        oxHealth = (oxHealth + days * 5).coerceAtMost(100)
        val healed = aliveMembers().joinToString(", ") { "${it.name} (${it.state.displayName})" }
        report += "\n\nRest helps. Your party's health: $healed."
        report += "\n\nTrail wisdom: ${TrailTips.list[rng.nextInt(TrailTips.list.size)]}"
        val storyteller = aliveMembers().takeIf { it.isNotEmpty() }
            ?.let { it[rng.nextInt(it.size)].name }
        if (storyteller != null) {
            report += "\n\nAround the fire: ${CampStories.forMember(storyteller, rng)}"
        }
        addJournal("Rested for $days day(s) to recover.")
        showNotice("Resting", listOf(report), Phase.TRAVEL)
        pendingSound = Sound.REST
    }
