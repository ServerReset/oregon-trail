package com.oregontrail.engine

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

    /** True when the Back button should open the pause menu. */
fun Game.canPause(): Boolean = phase in Game.PAUSABLE_PHASES

internal fun Game.openPause() {
        if (canPause()) {
            pauseReturn = phase
            phase = Phase.PAUSE
        }
    }

    /** A short, shareable summary of the journey. */
fun Game.summaryText(): String {
        val leader = party.firstOrNull()?.name ?: "Traveler"
        val sb = StringBuilder()
        sb.append("The Oregon Trail - $leader\n")
        sb.append("${occupation.displayName}, departed ${travelMonth.displayName} 1848\n")
        sb.append("${date}: $miles of ${Data.TOTAL_MILES} miles\n")
        if (phase == Phase.DEATH) {
            sb.append("Died of $deathCause on the trail.\n")
        } else {
            sb.append("Final score: $lastScore\n")
        }
        sb.append("Party:\n")
        for (m in party) {
            val fate = if (m.alive) "reached Oregon" else "died of ${m.condition ?: "the trail"}"
            sb.append("  ${m.name}: $fate\n")
        }
        return sb.toString()
    }
