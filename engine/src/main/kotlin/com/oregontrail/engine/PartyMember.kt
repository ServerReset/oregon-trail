package com.oregontrail.engine

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
