package com.oregontrail.engine

import kotlin.math.min

internal fun Game.handleRiver(action: String) {
        val lm = Data.landmarkAt(landmarkIndex)
        val river = lm.river ?: return
        val msgs = ArrayList<String>()
        when (action) {
            "ford" -> {
                unlock(Achievements.RIVERBANK)
                val risk = 0.25 + if (inventory.oxen < 4) 0.2 else 0.0 + weatherRisk()
                if (rng.chance(1 - risk)) {
                    msgs.add("You ford the ${lm.name.split(" ").first()} safely.")
                    pendingSound = Sound.RIVER
                } else {
                    supplyLoss(msgs, 30, 1)
                    msgs.add("The wagon is nearly swept away!")
                    msgs.add("You lose supplies but make the far bank.")
                    pendingSound = Sound.BAD
                }
            }
            "caulk" -> {
                unlock(Achievements.RIVERBANK)
                val days = rng.nextInt(1, 3)
                date.plusDays(days)
                val risk = 0.15 + if (inventory.oxen < 4) 0.15 else 0.0
                if (rng.chance(1 - risk)) {
                    msgs.add("You caulk the wagon and float across.")
                    msgs.add("It takes $days day(s) but works perfectly.")
                    pendingSound = Sound.RIVER
                } else {
                    supplyLoss(msgs, 40, 1)
                    msgs.add("The wagon tips and water pours in!")
                    msgs.add("You lose supplies but reach the far side.")
                    pendingSound = Sound.BAD
                }
            }
            "ferry" -> {
                val cost = river.ferryCost ?: return
                if (inventory.cash < cost) {
                    showNotice("River Crossing", listOf("You cannot afford the ferry."), Phase.RIVER)
                    pendingSound = Sound.BAD
                    return
                }
                inventory.cash -= cost
                date.plusDays(1)
                unlock(Achievements.FERRYMAN)
                msgs.add("You pay $${"%.2f".format(cost)} for the ferry and")
                msgs.add("cross the river without trouble.")
                pendingSound = Sound.RIVER
            }
            "guide" -> {
                val cost = river.guideCost ?: return
                if (inventory.cash < cost) {
                    showNotice("River Crossing", listOf("You cannot afford a guide."), Phase.RIVER)
                    pendingSound = Sound.BAD
                    return
                }
                inventory.cash -= cost
                date.plusDays(1)
                msgs.add("A local guide leads your wagon across")
                msgs.add("a safe ford for $${"%.2f".format(cost)}.")
                pendingSound = Sound.RIVER
            }
            "wait" -> {
                date.plusDays(1)
                rollWeather()
                consumeFood()
                msgs.add("You wait a day and watch the river.")
                msgs.add("The water is now: ${riverState(river)}.")
                showNotice("River Crossing", msgs, Phase.RIVER)
                return
            }
        }
        addJournal("Crossed the ${lm.name}.")
        showNotice("River Crossing", msgs, Phase.TRAVEL)
    }

internal fun Game.riverState(river: River): String =
        if (river.widthYards > 500) "wide and swift" else "deep and strong"

internal fun Game.weatherRisk(): Double = when (weather.kind) {
        WeatherKind.HEAVY_RAIN, WeatherKind.THUNDERSTORM -> 0.15
        WeatherKind.RAIN, WeatherKind.HAIL -> 0.08
        else -> 0.0
    }

internal fun Game.supplyLoss(msgs: MutableList<String>, food: Int, clothes: Int) {
        val lostFood = min(inventory.food, food)
        val lostClothes = min(inventory.clothing, clothes)
        inventory.food -= lostFood
        inventory.clothing -= lostClothes
        if (lostFood > 0) msgs.add("You lose $lostFood pounds of food.")
        if (lostClothes > 0) msgs.add("You lose $lostClothes set(s) of clothing.")
    }

    // ====================================================================
    //  The Dalles / end of trail
    // ====================================================================
