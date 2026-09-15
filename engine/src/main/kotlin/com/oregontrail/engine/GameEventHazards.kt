package com.oregontrail.engine

import kotlin.math.max
import kotlin.math.min

internal fun Game.evBreakdown(msgs: MutableList<String>) {
        val part = Part.entries[rng.nextInt(Part.entries.size)]
        if (inventory.useSpare(part)) {
            msgs.add("A wagon ${part.name.lowercase()} breaks, but you")
            msgs.add("have a spare and replace it on the spot.")
        } else if (occupation == Occupation.CARPENTER && rng.chance(0.5)) {
            msgs.add("A wagon ${part.name.lowercase()} breaks, but your")
            msgs.add("carpentry skills repair it without a spare part.")
            unlock(Achievements.CARPENTER)
        } else {
            val delay = rng.nextInt(10, 20)
            inventory.food = max(0, inventory.food - 8)
            miles -= delay
            msgs.add("A wagon ${part.name.lowercase()} breaks down.")
            msgs.add("You lose $delay miles making repairs.")
        }
}
internal fun Game.evOxLame(msgs: MutableList<String>) {
        val lost = rng.nextInt(15, 26)
        miles -= lost
        oxHealth = (oxHealth - rng.nextInt(10, 25)).coerceIn(0, 100)
        msgs.add("An ox goes lame. You slow down and")
        msgs.add("lose $lost miles resting the animal.")
        if (oxHealth <= 0 && inventory.oxen >= 2) {
            inventory.oxen -= 2
            oxHealth = 45
            msgs.add("The lame ox has to be cut from the team.")
        }
}
internal fun Game.evOxWander(msgs: MutableList<String>) {
        val lost = rng.nextInt(10, 18)
        miles -= lost
        msgs.add("An ox wanders off in the night.")
        msgs.add("You lose $lost miles searching for it.")
}
internal fun Game.evUnsafeWater(msgs: MutableList<String>) {
        val lost = rng.nextInt(2, 12)
        miles -= lost
        msgs.add("The water is unsafe. You lose time")
        msgs.add("looking for a clean spring.")
}
internal fun Game.evHeavyRain(msgs: MutableList<String>) {
        inventory.food = max(0, inventory.food - 10)
        inventory.ammo = max(0, inventory.ammo - 20)
        val lost = rng.nextInt(3, 12)
        miles -= lost
        msgs.add("Heavy rains soak the wagon. Food and")
        msgs.add("ammunition are damaged.")
}
internal fun Game.evHail(msgs: MutableList<String>) {
        inventory.ammo = max(0, inventory.ammo - 10)
        inventory.food = max(0, inventory.food - 4)
        val lost = rng.nextInt(4, 14)
        miles -= lost
        msgs.add("A hailstorm batters the wagon and")
        msgs.add("damages your supplies.")
}
internal fun Game.evFire(msgs: MutableList<String>) {
        inventory.food = max(0, inventory.food - 40)
        inventory.ammo = max(0, inventory.ammo - 20)
        msgs.add("A fire breaks out in the wagon.")
        msgs.add("Food and ammunition are damaged.")
}
internal fun Game.evFog(msgs: MutableList<String>) {
        val lost = rng.nextInt(8, 16)
        miles -= lost
        msgs.add("Heavy fog rolls in and you lose the")
        msgs.add("trail for $lost miles.")
}
internal fun Game.evSnakebite(msgs: MutableList<String>) {
        val victim = aliveMembers().randomOrNull(rng) ?: return
        victim.hurt(30)
        victim.condition = "a snakebite"
        msgs.add("${victim.name} is bitten by a poisonous")
        if (victim.alive) msgs.add("snake and grows very ill.") else {
            msgs.add("snake and does not survive.")
            deathCause = "a snakebite"
        }
}
internal fun Game.evCold(msgs: MutableList<String>) {
        if (inventory.clothing >= aliveCount * 2) {
            msgs.add("Cold weather bites, but your warm")
            msgs.add("clothing keeps the party comfortable.")
        } else {
            msgs.add("Cold weather! You do not have enough")
            msgs.add("clothing to keep everyone warm.")
            aliveMembers().forEach { it.hurt(rng.nextInt(4, 10)) }
        }
}
internal fun Game.evBlizzard(msgs: MutableList<String>) {
        inventory.food = max(0, inventory.food - 25)
        val lost = rng.nextInt(30, 70)
        miles -= lost
        msgs.add("A blizzard strikes the mountain pass!")
        msgs.add("You lose $lost miles and much food.")
        if (inventory.clothing < aliveCount * 2) {
            aliveMembers().forEach { it.hurt(rng.nextInt(6, 14)) }
        }
}
