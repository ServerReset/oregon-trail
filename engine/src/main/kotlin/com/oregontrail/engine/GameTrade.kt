package com.oregontrail.engine

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

internal fun Game.attemptTrade() {
        // A trader, mountain man or soldier offers a deal.
        val offers = listOf(
            Triple("80 pounds of food", "1 set of clothing", "food80"),
            Triple("1 spare wheel", "1 set of clothing", "wheel"),
            Triple("40 bullets", "30 pounds of food", "ammo"),
            Triple("1 yoke of oxen", "50 pounds of food", "oxen")
        )
        val offer = offers[rng.nextInt(offers.size)]
        val who = listOf("A trader", "A mountain man", "A soldier from the fort")[rng.nextInt(3)]
        choiceTitle = "Trading"
        choiceLines.clear()
        choiceLines.add("$who camped nearby offers you")
        choiceLines.add("${offer.first} in exchange for ${offer.second}.")
        choiceLines.add("")
        choiceLines.add("What do you do?")
        choiceOptions.clear()
        choiceOptions.add("Accept the trade" to "trade:yes:${offer.third}")
        choiceOptions.add("Haggle for more" to "trade:haggle:${offer.third}")
        choiceOptions.add("Decline" to "trade:no")
        choiceNext = Phase.TRAVEL
        phase = Phase.CHOICE
        pendingSound = Sound.TRADE
    }

    // ====================================================================
    //  Core travel loop
    // ====================================================================

internal fun Game.handleChoice(id: String) {
        when {
            id.startsWith("riders:") -> handleRiders(id.substringAfter("riders:"))
            id.startsWith("trade:") -> handleTradeResult(id)
            id.startsWith("rest:") -> rest(id.substringAfter("rest:").toIntOrNull() ?: 3)
            else -> phase = choiceNext
        }
    }

    /** Applies a trade, scaling what you receive by [bonus]. */
internal fun Game.applyTrade(kind: String, bonus: Double, msgs: MutableList<String>): Boolean {
        return when (kind) {
            "food80" -> {
                if (inventory.clothing >= 1) {
                    inventory.clothing -= 1
                    inventory.food += (80 * bonus).toInt()
                    msgs.add("You trade a set of clothing for")
                    msgs.add("${(80 * bonus).toInt()} pounds of food.")
                    true
                } else { msgs.add("You do not have the goods to trade."); false }
            }
            "wheel" -> {
                if (inventory.clothing >= 1) {
                    inventory.clothing -= 1
                    inventory.wheels += 1
                    if (bonus > 1.0) inventory.axles += 1
                    msgs.add("You trade a set of clothing for a spare wheel.")
                    if (bonus > 1.0) msgs.add("He throws in an axle as well.")
                    true
                } else { msgs.add("You do not have the goods to trade."); false }
            }
            "ammo" -> {
                if (inventory.food >= 30) {
                    inventory.food -= 30
                    inventory.ammo += (40 * bonus).toInt()
                    msgs.add("You trade 30 pounds of food for")
                    msgs.add("${(40 * bonus).toInt()} bullets.")
                    true
                } else { msgs.add("You do not have the goods to trade."); false }
            }
            "oxen" -> {
                if (inventory.food >= 50) {
                    inventory.food -= 50
                    inventory.oxen += 2
                    if (bonus > 1.0) inventory.food += 20
                    msgs.add("You trade 50 pounds of food for a yoke of oxen.")
                    if (bonus > 1.0) msgs.add("He throws in 20 pounds of food.")
                    true
                } else { msgs.add("You do not have the goods to trade."); false }
            }
            else -> false
        }
    }

internal fun Game.handleTradeResult(id: String) {
        val parts = id.split(":")
        val action = parts.getOrElse(1) { "" }
        if (action == "no") {
            showNotice("Trading", listOf("You decline the trade and move on."), Phase.TRAVEL)
            return
        }
        val kind = parts.getOrElse(2) { "" }
        val msgs = ArrayList<String>()
        if (action == "haggle") {
            // Bankers are practised at driving a bargain.
            val skill = if (occupation == Occupation.BANKER) 0.6 else 0.45
            if (rng.chance(skill)) {
                unlock(Achievements.BARGAINER)
                msgs.add("You haggle hard and the trader sweetens the deal.")
                applyTrade(kind, 1.25, msgs)
            } else {
                msgs.add("The trader scowls at your cheek and packs up.")
                msgs.add("No deal.")
            }
            showNotice("Bargaining", msgs, Phase.TRAVEL)
            return
        }
        applyTrade(kind, 1.0, msgs)
        showNotice("Trading", msgs, Phase.TRAVEL)
    }

    // ====================================================================
    //  Scoring
    // ====================================================================
