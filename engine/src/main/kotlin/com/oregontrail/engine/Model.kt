package com.oregontrail.engine

/** A simple 1840s calendar date that avoids java.time (unavailable below API 26). */
class GameDate(year: Int = 1848, month: Int = 3, day: Int = 1) {

    var year: Int = year
        private set
    var month: Int = month
        private set
    var day: Int = day
        private set

    private val monthLengths = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)

    private fun isLeap(y: Int): Boolean = (y % 4 == 0 && y % 100 != 0) || y % 400 == 0

    private fun lengthOfMonth(y: Int, m: Int): Int =
        if (m == 2 && isLeap(y)) 29 else monthLengths[m - 1]

    fun plusDays(n: Int) {
        var remaining = n
        while (remaining > 0) {
            val len = lengthOfMonth(year, month)
            if (day >= len) {
                day = 1
                month++
                if (month > 12) {
                    month = 1
                    year++
                }
            } else {
                day++
                remaining--
            }
        }
    }

    val monthName: String
        get() = MONTH_NAMES[month - 1]

    val dayName: String
        get() = DAY_NAMES[weekday]

    /** 0 = Sunday, 1 = Monday ... 6 = Saturday (Zeller's congruence). */
    private val weekday: Int
        get() {
            var m = month
            var y = year
            if (m < 3) {
                m += 12
                y -= 1
            }
            val k = y % 100
            val j = y / 100
            val h = (day + (13 * (m + 1)) / 5 + k + k / 4 + j / 4 + 5 * j) % 7
            // h: 0=Saturday ... convert to 0=Sunday
            return (h + 6) % 7
        }

    override fun toString(): String = "$dayName, $monthName $day, $year"

    fun copy(): GameDate = GameDate(year, month, day)

    companion object {
        private val MONTH_NAMES = arrayOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        private val DAY_NAMES = arrayOf(
            "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
        )
    }
}

enum class WeatherKind(val displayName: String) {
    CLEAR("clear"),
    CLOUDY("cloudy"),
    RAIN("rainy"),
    HEAVY_RAIN("heavy rain"),
    THUNDERSTORM("thunderstorms"),
    SNOW("snowy"),
    BLIZZARD("blizzard"),
    FOG("foggy"),
    HOT("very hot"),
    COLD("very cold"),
    WINDY("windy"),
    HAIL("hailstorms")
}

data class Weather(val kind: WeatherKind, val tempF: Int) {
    val description: String
        get() = "${kind.displayName}, ${tempF}\u00B0F"
}

/** One member of the traveling party. */
class PartyMember(
    var name: String,
    var health: Int = 100,
    var alive: Boolean = true,
    var condition: String? = null
) {
    val state: HealthState get() = if (!alive) HealthState.DEAD else HealthState.fromHealth(health)

    fun hurt(amount: Int) {
        if (!alive) return
        health = (health - amount).coerceIn(0, 100)
        if (health == 0) alive = false
    }

    fun heal(amount: Int) {
        if (!alive) return
        health = (health + amount).coerceAtMost(100)
        if (condition != null && health > 50) condition = null
    }
}

/** Everything the party owns. */
class Inventory(
    var cash: Double = 0.0,
    var oxen: Int = 0,          // single oxen; bought in yokes of two
    var food: Int = 0,          // pounds
    var clothing: Int = 0,      // sets
    var ammo: Int = 0,          // bullets
    var wheels: Int = 0,
    var axles: Int = 0,
    var tongues: Int = 0
) {
    fun spareCount(part: Part): Int = when (part) {
        Part.WHEEL -> wheels
        Part.AXLE -> axles
        Part.TONGUE -> tongues
    }

    fun useSpare(part: Part): Boolean {
        return when (part) {
            Part.WHEEL -> if (wheels > 0) { wheels--; true } else false
            Part.AXLE -> if (axles > 0) { axles--; true } else false
            Part.TONGUE -> if (tongues > 0) { tongues--; true } else false
        }
    }

    fun addSpare(part: Part) {
        when (part) {
            Part.WHEEL -> wheels++
            Part.AXLE -> axles++
            Part.TONGUE -> tongues++
        }
    }

    fun copy(): Inventory = Inventory(cash, oxen, food, clothing, ammo, wheels, axles, tongues)
}
