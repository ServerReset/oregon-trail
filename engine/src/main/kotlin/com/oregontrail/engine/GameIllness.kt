package com.oregontrail.engine

import kotlin.math.max
import kotlin.math.min
import kotlin.math.abs

internal fun Game.checkIllness(msgs: MutableList<String>): Boolean {
        var any = false
        for (m in aliveMembers()) {
            var chance = 0.012
            if (rations != Rations.FILLING) chance += 0.012
            if (rations == Rations.BARE_BONES) chance += 0.02
            if (pace == Pace.GRUELING) chance += 0.01
            if (inventory.food <= 20) chance += 0.02
            if (weather.tempF < 40 && inventory.clothing < aliveCount) chance += 0.02
            if (m.health < 50) chance += 0.015
            chance *= difficulty.illnessScale
            if (!rng.chance(chance)) continue
            any = true
            val illness = Data.illnesses[rng.nextInt(Data.illnesses.size)]
            val severity = rng.nextInt(8, 22)
            m.hurt(severity)
            m.condition = illness
            if (!m.alive) {
                msgs.add("${m.name} has died of $illness.")
                addJournal("${m.name} died of $illness.")
                deathCause = illness
            } else {
                msgs.add("${m.name} has come down with $illness.")
                addJournal("${m.name} came down with $illness.")
            }
        }
        return any
    }

    // ====================================================================
    //  Events
    // ====================================================================
