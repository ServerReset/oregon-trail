package com.oregontrail.engine

import kotlin.math.max

internal fun Game.startStrandedChoice() {
        choiceTitle = "Stranded Family"
        choiceLines.clear()
        choiceLines.add("A family's wagon has thrown a wheel and")
        choiceLines.add("they are nearly out of food. They ask you")
        choiceLines.add("for help. What do you do?")
        choiceOptions.clear()
        choiceOptions.add("Share 50 lb of food" to "stranded:food")
        choiceOptions.add("Give them a spare wheel" to "stranded:wheel")
        choiceOptions.add("Wish them luck" to "stranded:no")
        choiceNext = Phase.TRAVEL
        phase = Phase.CHOICE
    }

internal fun Game.handleStranded(action: String) {
        val msgs = ArrayList<String>()
        when (action) {
            "food" -> if (inventory.food >= 50) {
                inventory.food -= 50
                msgs.add("You share 50 pounds of food with the")
                msgs.add("stranded family. They thank you warmly.")
                unlock(Achievements.GOOD_SAMARITAN)
            } else {
                msgs.add("You have no food to spare and roll on,")
                msgs.add("heavy of heart.")
            }
            "wheel" -> if (inventory.wheels > 0) {
                inventory.wheels -= 1
                msgs.add("You give them a spare wheel. They vow")
                msgs.add("to repay the kindness someday.")
                unlock(Achievements.GOOD_SAMARITAN)
            } else {
                msgs.add("You have no spare wheel to give.")
            }
            else -> msgs.add("You wish them luck and move on. The trail is hard.")
        }
        addJournal("Stranded family on the trail: chose '$action'.")
        showNotice("Stranded Family", msgs, Phase.TRAVEL)
    }

internal fun Game.startRidersChoice() {        choiceTitle = "Riders Ahead"
        choiceLines.clear()
        choiceLines.add("Riders appear on the horizon.")
        choiceLines.add(if (rng.chance(0.6)) "They look hostile." else "They look friendly.")
        choiceLines.add("")
        choiceLines.add("What do you do?")
        choiceOptions.clear()
        choiceOptions.add("1. Run" to "riders:run")
        choiceOptions.add("2. Attack" to "riders:attack")
        choiceOptions.add("3. Continue" to "riders:continue")
        choiceOptions.add("4. Circle the wagons" to "riders:circle")
        choiceNext = Phase.TRAVEL
        phase = Phase.CHOICE
    }

internal fun Game.handleRiders(action: String) {
        val hostile = rng.chance(0.6)
        val msgs = ArrayList<String>()
        if (action == "attack") {
            if (inventory.ammo >= 40) {
                inventory.ammo -= 40
                msgs.add("You attack. A sharp fight follows, but")
                msgs.add("the riders withdraw. You spent 40 bullets.")
            } else {
                inventory.ammo = 0
                val stolen = (inventory.cash / 3).coerceAtLeast(0.0)
                inventory.cash -= stolen
                msgs.add("You ran low on bullets. The riders")
                msgs.add("took what they wanted and left.")
            }
        } else if (action == "run") {
            inventory.oxen = max(0, inventory.oxen - 2)
            miles += 15
            msgs.add("You run for it, gaining time but")
            msgs.add("wearing out an ox. The riders give up.")
        } else if (action == "circle") {
            miles -= 20
            if (hostile) {
                msgs.add("You circle the wagons. The riders")
                msgs.add("circle twice and ride off.")
            } else {
                msgs.add("You circle the wagons. The friendly")
                msgs.add("riders trade news and move on.")
            }
        } else {
            if (hostile && inventory.ammo < 20) {
                inventory.food = max(0, inventory.food - 30)
                msgs.add("The hostile riders raid your stores")
                msgs.add("and take 30 pounds of food.")
            } else {
                msgs.add("You keep moving. The riders pass by")
                msgs.add("without incident.")
            }
        }
        addJournal("Riders on the trail; we chose to $action.")
        showNotice(choiceTitle, msgs, Phase.TRAVEL)
    }

    // ====================================================================
    //  Landmarks
    // ====================================================================


/** A simple, dependency-free helper for picking a random element. */
