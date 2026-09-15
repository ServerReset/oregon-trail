package com.oregontrail.engine

enum class HealthState(val displayName: String) {
    GOOD("good"),
    FAIR("fair"),
    POOR("poor"),
    VERY_POOR("very poor"),
    DEAD("dead");

    companion object {
        fun fromHealth(health: Int): HealthState = when {
            health <= 0 -> DEAD
            health < 25 -> VERY_POOR
            health < 50 -> POOR
            health < 75 -> FAIR
            else -> GOOD
        }
    }
}

/** Wagon spare parts that can break. */
enum class Part { WHEEL, AXLE, TONGUE }

/** Everything that can be bought or used. */
enum class Item(
    val label: String,
    val unit: String,
    val basePrice: Double,
    val fortPrice: Double,
    val step: Int,
    val max: Int
) {
    OXEN("Oxen", "yoke", 20.0, 25.0, 1, 12),
    FOOD("Food", "pound", 0.18, 0.22, 50, 3000),
    CLOTHING("Clothing", "set", 10.0, 12.5, 1, 99),
    AMMUNITION("Ammunition", "box", 2.0, 2.5, 1, 99),
    WHEEL("Spare wheel", "wheel", 10.0, 12.5, 1, 9),
    AXLE("Spare axle", "axle", 10.0, 12.5, 1, 9),
    TONGUE("Spare tongue", "tongue", 10.0, 12.5, 1, 9)
}
