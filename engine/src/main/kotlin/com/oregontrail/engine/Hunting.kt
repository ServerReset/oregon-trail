package com.oregontrail.engine

import kotlin.math.abs

/** An animal that can be hunted, with a distinct roguelike glyph and color. */
internal data class HuntAnimal(var x: Int, var y: Int, val kind: AnimalKind, var alive: Boolean = true)

internal data class HuntBullet(var x: Int, var y: Int, val dx: Int, val dy: Int)

/**
 * A self-contained roguelike hunting minigame. The field is plain ASCII:
 * `@` is the hunter, letters are animals, `^` and `f` are trees, `o` is rock.
 * It has no Android dependency and can be unit tested.
 */
class HuntField(
    val width: Int,
    val height: Int,
    internal val rng: Rng,
    animalsPool: List<AnimalKind>,
    val carryLimit: Int = 100,
    initialMeat: Int = 0,
    initialKills: Int = 0,
    initialShots: Int = 0
) {

    internal val blocked = BooleanArray(width * height)
    internal val decorations = ArrayList<Triple<Int, Int, Char>>()
    internal val animals = ArrayList<HuntAnimal>()
    internal val bullets = ArrayList<HuntBullet>()

    var hunterX: Int = width / 2
        private set
    var hunterY: Int = height / 2
        private set
    var aimX: Int = 1
        private set
    var aimY: Int = 0
        private set

    var meat: Int = initialMeat
        internal set
    var shotsFired: Int = initialShots
        private set
    var kills: Int = initialKills
        internal set

    internal val pool = animalsPool
    internal val maxAnimals = 5

    init {
        // Scatter trees and rocks, but keep an open area around the hunter.
        val decorCount = (width * height) / 18
        var attempts = 0
        while (decorations.size < decorCount && attempts < decorCount * 20) {
            attempts++
            val x = rng.nextInt(width)
            val y = rng.nextInt(height)
            if (abs(x - hunterX) <= 3 && abs(y - hunterY) <= 2) continue
            if (blocked[y * width + x]) continue
            val ch = if (rng.chance(0.7)) Ascii.TREE_A else Ascii.TREE_B
            blocked[y * width + x] = true
            decorations.add(Triple(x, y, ch))
        }
        repeat(if (pool.isEmpty()) 0 else 3) { spawnAnimal() }
    }

    internal fun animalAt(x: Int, y: Int): HuntAnimal? = animals.firstOrNull { it.alive && it.x == x && it.y == y }

    internal fun isBlocked(x: Int, y: Int): Boolean {
        if (x < 0 || y < 0 || x >= width || y >= height) return true
        return blocked[y * width + x]
    }

    internal fun spawnAnimal() {
        if (pool.isEmpty()) return
        val kind = pool[rng.nextInt(pool.size)]
        val y = rng.nextInt(height)
        val fromLeft = rng.chance(0.5)
        val x = if (fromLeft) 0 else width - 1
        if (isBlocked(x, y) || animalAt(x, y) != null) return
        animals.add(HuntAnimal(x, y, kind))
    }

    fun move(dx: Int, dy: Int) {
        aimX = dx
        aimY = dy
        val nx = hunterX + dx
        val ny = hunterY + dy
        if (!isBlocked(nx, ny)) {
            hunterX = nx
            hunterY = ny
        }
    }

    /** Fires a bullet in the current aim direction. Returns true if a shot was fired. */
    fun shoot(): Boolean {
        val sx = hunterX + aimX
        val sy = hunterY + aimY
        if (isBlocked(sx, sy)) return false
        bullets.add(HuntBullet(sx, sy, aimX, aimY))
        shotsFired++
        return true
    }

    var lastKill: AnimalKind? = null
        internal set
    var lastShotHit: Boolean = false
        internal set

    /** Ends the hunt, returning the meat actually carried back. */
    fun finish(): Int = meat.coerceAtMost(carryLimit)

    // ----- test hooks (visible to the same module) ----------------------

    internal fun debugPlaceAnimal(x: Int, y: Int, kind: AnimalKind) {
        animals.add(HuntAnimal(x, y, kind))
    }

    internal fun debugAnimalCount(): Int = animals.size
}
