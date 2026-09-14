package com.oregontrail.engine

import kotlin.math.abs

/** An animal that can be hunted, with a distinct roguelike glyph and color. */
enum class AnimalKind(
    val glyph: Char,
    val displayName: String,
    val meat: Int,
    val color: Palette
) {
    SQUIRREL('s', "squirrel", 2, Palette.BROWN),
    RABBIT('r', "rabbit", 2, Palette.WHITE),
    FOX('f', "fox", 5, Palette.YELLOW),
    DEER('d', "deer", 45, Palette.BROWN),
    ANTELOPE('a', "antelope", 40, Palette.YELLOW),
    BEAR('B', "bear", 80, Palette.RED),
    WOLF('w', "wolf", 30, Palette.GRAY),
    BUFFALO('b', "buffalo", 100, Palette.WHITE)
}

private data class HuntAnimal(var x: Int, var y: Int, val kind: AnimalKind, var alive: Boolean = true)

private data class HuntBullet(var x: Int, var y: Int, val dx: Int, val dy: Int)

/**
 * A self-contained roguelike hunting minigame. The field is plain ASCII:
 * `@` is the hunter, letters are animals, `^` and `f` are trees, `o` is rock.
 * It has no Android dependency and can be unit tested.
 */
class HuntField(
    val width: Int,
    val height: Int,
    private val rng: Rng,
    animalsPool: List<AnimalKind>,
    val carryLimit: Int = 100
) {

    private val blocked = BooleanArray(width * height)
    private val decorations = ArrayList<Triple<Int, Int, Char>>()
    private val animals = ArrayList<HuntAnimal>()
    private val bullets = ArrayList<HuntBullet>()

    var hunterX: Int = width / 2
        private set
    var hunterY: Int = height / 2
        private set
    var aimX: Int = 1
        private set
    var aimY: Int = 0
        private set

    var meat: Int = 0
        private set
    var shotsFired: Int = 0
        private set
    var kills: Int = 0
        private set

    private val pool = animalsPool
    private val maxAnimals = 5

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

    fun decorationAt(x: Int, y: Int): Char? {
        for (d in decorations) if (d.first == x && d.second == y) return d.third
        return null
    }

    private fun animalAt(x: Int, y: Int): HuntAnimal? = animals.firstOrNull { it.alive && it.x == x && it.y == y }

    fun animalGlyphAt(x: Int, y: Int): Pair<Char, Palette>? =
        animalAt(x, y)?.let { it.kind.glyph to it.kind.color }

    fun bulletAt(x: Int, y: Int): Boolean = bullets.any { it.x == x && it.y == y }

    val isFull: Boolean get() = meat >= carryLimit

    private fun isBlocked(x: Int, y: Int): Boolean {
        if (x < 0 || y < 0 || x >= width || y >= height) return true
        return blocked[y * width + x]
    }

    private fun spawnAnimal() {
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
        private set
    var lastShotHit: Boolean = false
        private set

    /** Advances the simulation one step. */
    fun tick() {
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

    /** Ends the hunt, returning the meat actually carried back. */
    fun finish(): Int = meat.coerceAtMost(carryLimit)

    // ----- test hooks (visible to the same module) ----------------------

    internal fun debugPlaceAnimal(x: Int, y: Int, kind: AnimalKind) {
        animals.add(HuntAnimal(x, y, kind))
    }

    internal fun debugAnimalCount(): Int = animals.size
}
