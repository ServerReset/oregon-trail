package com.oregontrail.engine

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

internal fun Game.handleDalles(action: String) {
        when (action) {
            "barlow" -> {
                val toll = 5.0
                if (inventory.cash < toll) {
                    showNotice("The Dalles", listOf("You cannot afford the Barlow Road toll."), Phase.LANDMARK)
                    return
                }
                inventory.cash -= toll
                startBarlow()
                return
            }
            "raft" -> {
                startRaft()
                return
            }
            "portage" -> {
                val days = rng.nextInt(4, 8)
                date.plusDays(days)
                arriveOregon(
                    listOf(
                        "You portage around the rapids, hauling the",
                        "wagons overland. It is slow and exhausting,",
                        "taking $days days, but nothing is lost.",
                        "At last, the Willamette Valley lies below."
                    )
                )
                return
            }
            "wait" -> {
                date.plusDays(1)
                showNotice("The Dalles", listOf("You wait for better conditions."), Phase.LANDMARK)
                return
            }
        }
    }

internal fun Game.startBarlow() {
        val (w, h) = barlowSize()
        barlowField = BarlowField(w, h, rng)
        phase = Phase.BARLOW
    }

internal fun Game.barlowSize(): Pair<Int, Int> =
        min(contentW, 40).coerceIn(10, 40) to min(rows - 5, 16).coerceIn(4, 16)

internal fun Game.finishBarlow() {
        val field = barlowField ?: return
        val msgs = ArrayList<String>()
        if (field.success) {
            msgs.add("After a long climb over the Cascades, the")
            msgs.add("Barlow Road brings you down into the valley.")
            if (field.damage > 0) {
                supplyLoss(msgs, field.damage * 8, 0)
                msgs.add("The rocks and ruts cost you some supplies.")
            }
        } else {
            msgs.add("A wheel shatters on the rocks and the")
            msgs.add("wagon is dragged down the slope.")
            supplyLoss(msgs, 40, 1)
        }
        addJournal("Took the Barlow Road over the Cascades.")
        barlowField = null
        arriveOregon(msgs)
    }

internal fun Game.startRaft() {
        val (w, h) = raftSize()
        raftField = RaftField(w, h, rng)
        phase = Phase.RAFTING
    }

internal fun Game.finishRaft() {
        val field = raftField ?: return
        val msgs = ArrayList<String>()
        if (field.success) {
            msgs.add("You pilot the raft through the crashing")
            msgs.add("rapids of the Columbia and reach the shore.")
            if (field.damage() > 0) {
                supplyLoss(msgs, field.damage() * 12, 0)
                msgs.add("The rocks cost you some supplies.")
            }
        } else {
            msgs.add("Your raft is smashed on the rocks!")
            msgs.add("You struggle ashore, losing supplies.")
            supplyLoss(msgs, 60, 1)
        }
        raftField = null
        arriveOregon(msgs)
    }

internal fun Game.arriveOregon(extra: List<String> = emptyList()) {
        lastScore = computeScore()
        arrivalFrame = frame
        addJournal("Arrived safely in the Willamette Valley!")
        topTen.add(ScoreEntry(party.firstOrNull()?.name ?: "Traveler", lastScore, occupation.displayName))
        topTen = topTen.sortedByDescending { it.points }.take(10).toMutableList()
        scores.saveScores(topTen)
        recordStats {
            it.copy(
                arrivals = it.arrivals + 1,
                bestScore = maxOf(it.bestScore, lastScore),
                totalMiles = it.totalMiles + miles
            )
        }
        unlock(Achievements.REACHED_OREGON)
        if (aliveCount == 5) unlock(Achievements.ALL_FIVE_ALIVE)
        if (aliveCount == 1) unlock(Achievements.SURVIVOR)
        if (inventory.cash >= 500) unlock(Achievements.FRUGAL)
        if (inventory.cash >= 1000) unlock(Achievements.WEALTHY)
        if (topTen.any { it.points == lastScore }) unlock(Achievements.TOP_TEN)
        pendingSound = Sound.ARRIVAL
        noticeTitle = "Oregon!"
        noticeLines.clear()
        noticeLines.addAll(extra)
        if (noticeLines.isNotEmpty()) noticeLines.add("")
        noticeLines.add("You have reached the Willamette Valley!")
        noticeLines.add("")
        noticeLines.add("You traveled ${miles} miles in ${daysOnTrail()} days.")
        noticeLines.add("Your final score is $lastScore points.")
        noticeNext = Phase.ARRIVED
        phase = Phase.NOTICE
    }

internal fun Game.daysOnTrail(): Int {
        // Rough day count from March 1 to current date.
        var count = 0
        val start = GameDate(1848, travelMonth.monthIndex, 1)
        while (start.month != date.month || start.day != date.day || start.year != date.year) {
            start.plusDays(1)
            count++
            if (count > 400) break
        }
        return count
    }

    // ====================================================================
    //  Death
    // ====================================================================
