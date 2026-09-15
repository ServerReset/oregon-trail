package com.oregontrail.engine

/** The hunt's per-frame simulation, kept out of the data class. */
/** Advances the simulation one step. */
fun HuntField.tick() {
    lastKill = null
    lastShotHit = false

    // Advance bullets.
    val bulletsToRemove = ArrayList<HuntBullet>()
    for (b in bullets) {
        repeat(2) {
            b.x += b.dx
            b.y += b.dy
            val target = animals.firstOrNull { it.alive && it.x == b.x && it.y == b.y }
            if (target != null) {
                target.alive = false
                lastKill = target.kind
                lastShotHit = true
                kills++
                meat = (meat + target.kind.meat).coerceAtMost(carryLimit)
                bulletsToRemove.add(b)
                return@repeat
            }
            if (isBlocked(b.x, b.y)) {
                bulletsToRemove.add(b)
                return@repeat
            }
        }
    }
    bullets.removeAll(bulletsToRemove.toSet())

    animals.removeAll { !it.alive }

    // Animals wander.
    for (a in animals) {
        if (!rng.chance(0.55)) continue
        val dirs = listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)
        val (dx, dy) = dirs[rng.nextInt(4)]
        val nx = (a.x + dx).coerceIn(0, width - 1)
        val ny = (a.y + dy).coerceIn(0, height - 1)
        if (!isBlocked(nx, ny) && animalAt(nx, ny) == null) {
            a.x = nx
            a.y = ny
        }
    }

    while (animals.size < maxAnimals && rng.chance(0.4)) {
        val before = animals.size
        spawnAnimal()
        if (animals.size == before) break
    }
}
