package com.oregontrail.engine

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

    /** Builds the current screen. Rendering itself lives in GameRender.kt. */
fun Game.render(): Screen {
        val screen = Screen(cols, rows)
        screen.fillBackground(Palette.BLACK)
        when (phase) {
            Phase.TITLE -> renderTitle(screen)
            Phase.ABOUT -> renderAbout(screen)
            Phase.MANAGEMENT -> renderManagement(screen)
            Phase.TOP_TEN -> renderTopTen(screen)
            Phase.PROFESSION -> renderProfession(screen)
            Phase.MONTH -> renderMonth(screen)
            Phase.NAMES -> renderNames(screen)
            Phase.STORE -> renderStore(screen)
            Phase.TRAVEL -> renderTravel(screen)
            Phase.LANDMARK -> renderLandmark(screen)
            Phase.RIVER -> renderRiver(screen)
            Phase.MAP -> renderMap(screen)
            Phase.JOURNAL -> renderJournal(screen)
            Phase.LOAD -> renderLoad(screen)
            Phase.ACHIEVEMENTS -> renderAchievements(screen)
            Phase.STATS -> renderStats(screen)
            Phase.EPILOGUE -> renderEpilogue(screen)
            Phase.PAUSE -> renderPause(screen)
            Phase.CHOICE -> renderChoice(screen)
            Phase.HUNTING -> renderHunting(screen)
            Phase.RAFTING -> renderRafting(screen)
            Phase.BARLOW -> renderBarlow(screen)
            Phase.NOTICE -> renderNotice(screen)
            Phase.DEATH -> renderDeath(screen)
            Phase.ARRIVED -> renderArrived(screen)
        }
        screen.ambient = ambientFor()
        applyTransition(screen)
        return screen
    }

    /**
     * A top-to-bottom reveal when the screen changes. Opt-in so headless tests
     * always see the complete screen.
     */
internal fun Game.applyTransition(screen: Screen) {
        if (!transitions) return
        if (phase != lastRenderedPhase) {
            lastRenderedPhase = phase
            transitionStart = frame
        }
        val elapsed = frame - transitionStart
        if (elapsed >= Game.TRANSITION_FRAMES) return
        val revealed = rows * elapsed / Game.TRANSITION_FRAMES
        for (y in revealed until rows) screen.blankRow(y)
    }

    /** Chooses a background mood for the current situation. */
internal fun Game.ambientFor(): Palette = when (phase) {
        Phase.RAFTING -> Palette.BLUE
        Phase.BARLOW -> Palette.BROWN
        Phase.HUNTING -> Palette.GREEN
        Phase.TRAVEL, Phase.LANDMARK, Phase.RIVER -> when (weather.kind) {
            WeatherKind.THUNDERSTORM ->
                if (frame % 9 == 0 || frame % 9 == 1) Palette.BRIGHT_WHITE else Palette.BLUE
            WeatherKind.SNOW, WeatherKind.BLIZZARD, WeatherKind.COLD,
            WeatherKind.HEAVY_RAIN, WeatherKind.RAIN, WeatherKind.HAIL -> Palette.BLUE
            WeatherKind.HOT -> Palette.BROWN
            WeatherKind.CLEAR -> Palette.GREEN
            else -> Palette.BLACK
        }
        else -> Palette.BLACK
    }

    // ====================================================================
    //  Save / restore
    // ====================================================================
