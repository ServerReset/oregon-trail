package com.oregontrail.engine

import kotlin.math.max
import kotlin.math.min

internal fun Game.evChildLost(msgs: MutableList<String>) {
        val lost = rng.nextInt(5, 11)
        miles -= lost
        msgs.add("A child gets lost in the tall grass.")
        msgs.add("You spend half a day searching.")
}
internal fun Game.evChildArm(msgs: MutableList<String>) {
        val lost = rng.nextInt(4, 9)
        inventory.clothing = max(0, inventory.clothing - 1)
        miles -= lost
        msgs.add("A child breaks an arm and needs a sling.")
        msgs.add("You stop and tend the injury.")
}
internal fun Game.evBandits(msgs: MutableList<String>) {
        if (inventory.ammo >= 40) {
            inventory.ammo -= 20
            msgs.add("Bandits attack! You fire back and")
            msgs.add("drive them off, spending 20 bullets.")
        } else {
            val stolen = rng.nextInt(10, 60).toDouble().coerceAtMost(inventory.cash)
            inventory.cash -= stolen
            msgs.add("Bandits attack and you are low on")
            msgs.add("bullets. They steal $${"%.0f".format(stolen)}.")
        }
}
internal fun Game.evWildAnimals(msgs: MutableList<String>) {
        if (inventory.ammo >= 20) {
            inventory.ammo -= 20
            inventory.food += 40
            msgs.add("Wild animals attack the oxen. You")
            msgs.add("shoot one and gain 40 pounds of meat.")
        } else {
            inventory.food = max(0, inventory.food - 20)
            inventory.clothing = max(0, inventory.clothing - 1)
            msgs.add("Wild animals raid the camp. They")
            msgs.add("carry off food and clothing.")
        }
}
internal fun Game.evIndians(msgs: MutableList<String>) {
        inventory.food += 14
        msgs.add("Helpful Shoshone show you where to")
        msgs.add("find camas roots. You gain 14 pounds.")
}
internal fun Game.evThief(msgs: MutableList<String>) {
        val stolenFood = min(inventory.food, rng.nextInt(20, 60))
        val stolenClothes = min(inventory.clothing, rng.nextInt(0, 2))
        inventory.food -= stolenFood
        inventory.clothing -= stolenClothes
        msgs.add("A thief creeps into camp at night.")
        msgs.add("You lose $stolenFood pounds of food.")
}
internal fun Game.evFruit(msgs: MutableList<String>) {
        val gained = rng.nextInt(10, 31)
        inventory.food += gained
        msgs.add("You find bushes heavy with wild fruit")
        msgs.add("and gather $gained pounds of food.")
}
internal fun Game.evBerries(msgs: MutableList<String>) {
        val gained = rng.nextInt(6, 18)
        inventory.food += gained
        msgs.add("The children find wild berries along")
        msgs.add("the creek and gather $gained pounds.")
}
internal fun Game.evPrairieDogs(msgs: MutableList<String>) {
        msgs.add("A colony of prairie dogs whistles from")
        msgs.add("their burrows. The children laugh.")
}
internal fun Game.evRainbow(msgs: MutableList<String>) {
        unlock(Achievements.RAINBOW)
        msgs.add("A rainbow arcs across the sky after the")
        msgs.add("rain. The whole train takes heart.")
}
internal fun Game.evHotSprings(msgs: MutableList<String>) {
        aliveMembers().forEach { it.heal(rng.nextInt(3, 7)) }
        msgs.add("You find a warm mineral spring and let")
        msgs.add("the party soak. Everyone feels better.")
}
internal fun Game.evAbandonedWagon(msgs: MutableList<String>) {
        val part = Part.entries[rng.nextInt(Part.entries.size)]
        when (part) {
            Part.WHEEL -> inventory.wheels += 1
            Part.AXLE -> inventory.axles += 1
            Part.TONGUE -> inventory.tongues += 1
        }
        msgs.add("You come upon an abandoned wagon and")
        msgs.add("salvage a spare ${part.name.lowercase()}.")
}

/** A band of wild horses races the wagon — a good omen. */
internal fun Game.evWildHorses(msgs: MutableList<String>) {
    val gain = rng.nextInt(3, 9)
    miles += gain
    msgs.add("A band of wild horses races alongside the")
    msgs.add("wagon, spooking the oxen on for $gain miles.")
}

/** A false lake on the plains wastes a little time. */
internal fun Game.evMirage(msgs: MutableList<String>) {
    val lost = rng.nextInt(3, 7)
    miles -= lost
    msgs.add("A shining lake shimmers ahead — only a")
    msgs.add("mirage. You waste $lost miles chasing water.")
}

/** A buffalo herd: a chance for a great haul, if you have ammunition. */
internal fun Game.evBuffaloHerd(msgs: MutableList<String>) {
    if (inventory.ammo >= 10) {
        inventory.ammo -= 10
        inventory.food += 60
        msgs.add("A buffalo herd blocks the trail. You")
        msgs.add("shoot one and dress 60 pounds of meat.")
    } else {
        msgs.add("A buffalo herd thunders past the wagon,")
        msgs.add("too far to hunt without ammunition.")
    }
}

/** An evening of music lifts the party's spirits. */
internal fun Game.evFiddleNight(msgs: MutableList<String>) {
    aliveMembers().forEach { it.heal(rng.nextInt(2, 5)) }
    msgs.add("A fiddler plays in camp tonight. Heavy")
    msgs.add("hearts lighten and feet start to tap.")
}
