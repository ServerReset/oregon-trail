package com.oregontrail.engine

/**
 * The complete, platform-independent Oregon Trail game.
 *
 * It exposes to a front-end exactly what it needs:
 *  - [render] returns a [Screen] (a grid of styled characters + tap targets)
 *  - [onTap] feeds a tapped hotspot id back in
 *  - [huntTick] advances the hunting minigame
 *
 * The mutable state lives in [GameState] / [GameSession]; the rules live in
 * the `GameX*.kt` files as extension functions; the presentation lives in the
 * `Render*.kt` files.
 */
class Game(
    internal val rng: Rng = DefaultRng(),
    internal val scores: ScoreStore = InMemoryScoreStore()
) : GameState() {

    init {
        topTen = scores.loadScores()
        lastGravestone = scores.loadGravestone()
        graves.addAll(scores.loadGraves())
        achievements.addAll(scores.loadAchievements())
        stats = scores.loadStats()
        newRun(Occupation.BANKER, TravelMonth.MARCH)
        phase = Phase.TITLE
    }

    companion object {
        /** Bumped when the engine or its content changes. */
        const val VERSION = "2.8.0"

        /** Caps to keep save files and memory bounded on very long runs. */
        const val JOURNAL_LIMIT = 400
        const val GRAVE_LIMIT = 50

        /** Frames over which a screen-change reveal plays. */
        const val TRANSITION_FRAMES = 4

        val INVALID_RESUME_PHASES = setOf(
            Phase.TITLE, Phase.ABOUT, Phase.MANAGEMENT, Phase.TOP_TEN,
            Phase.PROFESSION, Phase.MONTH, Phase.NAMES, Phase.DEATH,
            Phase.ARRIVED, Phase.CHOICE, Phase.HUNTING, Phase.RAFTING, Phase.BARLOW,
            Phase.JOURNAL, Phase.LOAD, Phase.ACHIEVEMENTS, Phase.STATS,
            Phase.EPILOGUE, Phase.PAUSE, Phase.NOTICE
        )

        /** Phases from which the pause menu can be opened. */
        val PAUSABLE_PHASES = setOf(
            Phase.TRAVEL, Phase.LANDMARK, Phase.RIVER, Phase.MAP,
            Phase.JOURNAL, Phase.STORE, Phase.HUNTING, Phase.RAFTING, Phase.BARLOW
        )

        val ABOUT_PAGES: List<String> get() = AboutPages.list
    }
}
