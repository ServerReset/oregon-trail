package com.oregontrail.engine

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

internal fun Game.advanceToLandmark(msgs: MutableList<String>) {
        val idx = if (cutoffTarget > landmarkIndex) cutoffTarget else landmarkIndex + 1
        landmarkIndex = min(idx, Data.landmarks.lastIndex)
        cutoffTarget = -1
        val lm = Data.landmarkAt(landmarkIndex)
        if (lm.kind == LandmarkKind.END) { arriveOregon(); return }
        val lines = ArrayList<String>()
        lines.add("You have reached ${lm.name}.")
        lines.add("")
        lines.addAll(lm.blurb)
        val here = graves.filter { it.landmarkId == lm.id }
        if (here.isNotEmpty()) {
            lines.add("")
            lines.add("You pass the graves of earlier travelers:")
            here.take(3).forEach { lines.add(it.text) }
        }
        msgs.clear()
        lines.forEach { msgs.add(it) }
        addJournal("Reached ${lm.name}.")
        val next = if (lm.kind == LandmarkKind.RIVER) Phase.RIVER else Phase.LANDMARK
        showNotice("Landmark", lines, next)
        pendingSound = Sound.MILESTONE
    }

internal fun Game.handleLandmarkMenu(action: String) {
        when (action) {
            "continue" -> phase = Phase.TRAVEL
            "supplies" -> showNotice("Your Supplies", suppliesLines(), Phase.LANDMARK)
            "map" -> phase = Phase.MAP
            "rest" -> startRest()
            "buy" -> { storeAtFort = true; phase = Phase.STORE }
            "talk" -> talkToPeople()
            "fact" -> showHistory()
            "hunt" -> startHunt(Phase.LANDMARK)
            "cutoff" -> takeCutoff()
        }
    }

    /** Shows a historical note about the current landmark (the educational bit). */
internal fun Game.showHistory() {
        val lm = Data.landmarkAt(landmarkIndex)
        val fact = Facts.forLandmark(lm.id)
            ?: "This stretch of the trail is remembered by the families who crossed it."
        factsRead++
        pendingSound = Sound.PAGE
        if (factsRead >= 5) unlock(Achievements.HISTORIAN)
        addJournal("Read about ${lm.name}.")
        showNotice("History of ${shortLandmarkTitle(lm)}", listOf(fact), Phase.LANDMARK)
    }

internal fun Game.shortLandmarkTitle(lm: Landmark): String =
        if (lm.name.length <= cols - 12) lm.name else lm.name.substringBefore(",")

    /** Approximate river depth in feet, deepened by rain. */
fun Game.riverDepth(river: River): Double {
        val base = river.widthYards / 130.0 + 1.5
        val rain = when (weather.kind) {
            WeatherKind.HEAVY_RAIN, WeatherKind.THUNDERSTORM -> 2.0
            WeatherKind.RAIN, WeatherKind.HAIL -> 1.0
            else -> 0.0
        }
        return (base + rain)
    }

internal fun Game.takeCutoff() {
        val lm = Data.landmarkAt(landmarkIndex)
        val targetId = lm.cutoffId ?: return
        val targetIdx = Data.indexOf(targetId)
        if (targetIdx <= landmarkIndex + 1) return
        cutoffTarget = targetIdx
        val targetMile = Data.landmarkAt(targetIdx).mile
        miles = min(targetMile - 1, miles + lm.cutoffMiles)
        pendingSound = Sound.GOOD
        addJournal("Took the ${lm.cutoffLabel?.removePrefix("Take the ")} to save ${lm.cutoffMiles} miles.")
        showNotice(
            "Taking the Cutoff",
            listOf(
                lm.cutoffLabel ?: "You take a cutoff.",
                "You leave the main trail and save ${lm.cutoffMiles} miles,",
                "but you will miss the next fort and its supplies."
            ),
            Phase.TRAVEL
        )
    }

internal fun Game.talkToPeople() {
        val lm = Data.landmarkAt(landmarkIndex)
        val lines = ArrayList<String>()
        lines.add(Talk.lines[rng.nextInt(Talk.lines.size)])
        // Sometimes you also pick up a useful rumor.
        if (rng.chance(0.4)) {
            lines.add("")
            lines.add(Talk.rumors[rng.nextInt(Talk.rumors.size)])
        }
        showNotice(lm.name, lines, Phase.LANDMARK)
        pendingSound = Sound.SELECT
    }

    // ====================================================================
    //  Rivers
    // ====================================================================
