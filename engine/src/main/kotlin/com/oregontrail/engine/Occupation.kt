package com.oregontrail.engine

/** Occupations the player can choose. Higher difficulty = higher score multiplier. */
enum class Occupation(
    val displayName: String,
    val startingMoney: Int,
    val multiplier: Int,
    val blurb: String,
    val perk: String
) {
    BANKER(
        "Banker", 1400, 1,
        "You start with the most money but earn the fewest points.",
        "Perk: merchants give you a 10% discount at forts."
    ),
    CARPENTER(
        "Carpenter", 800, 2,
        "A skilled trade earns a middle income and middle points.",
        "Perk: you can often repair a broken wagon without a spare part."
    ),
    FARMER(
        "Farmer", 700, 3,
        "You start with little money but earn the most points.",
        "Perk: you know game, and bring home 50% more meat from a hunt."
    )
}

/** Overall difficulty, which scales how often trouble finds you. */
enum class Difficulty(val displayName: String, val eventChance: Double, val illnessScale: Double) {
    EASY("Easy", 0.05, 0.5),
    NORMAL("Normal", 0.16, 2.2),
    HARD("Hard", 0.26, 3.0)
}

/** Presentation settings owned by the front-end. */
interface UiSettings {
    var textScaleIndex: Int      // 0 = small, 1 = medium, 2 = large
    var highContrast: Boolean
    var scanlines: Boolean
    var haptics: Boolean
        get() = true
        set(_) {}

    /** Whether action sounds play. Persisted by the front-end. */
    var soundEnabled: Boolean
        get() = true
        set(_) {}

    /** CRT filter strength: 0 = off, 1 = low, 2 = high. Persisted. */
    var crtMode: Int
        get() = 1
        set(_) {}

    /** 0 = Terminal (green on black), 1 = Classic (colour on black), 2 = Material You. */
    var themeIndex: Int
        get() = 1
        set(_) {}
}

/** One dated line in the traveler's journal. */
