package com.oregontrail.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The pause menu, quick save/load and return paths. */
class PauseTest {

    private fun newGame(seed: Long = 1L): Game {
        val g = Game(DefaultRng(seed), InMemoryScoreStore())
        g.setViewport(48, 34)
        return g
    }

    private fun Game.intro() {
        onTap("title:travel"); onTap("prof:0"); onTap("month:0"); onTap("names:go")
        onTap("store:inc:OXEN"); repeat(20) { onTap("store:inc:FOOD") }
        onTap("store:leave"); onTap("notice:continue")
    }

    private fun dallesSave(): String = buildString {
        append("v=1\nocc=BANKER\nmonth=MAY\ndate=1848,8,1\nweather=CLEAR,72\n")
        append("miles=2040\nlandmark=16\npace=STEADY\nrations=FILLING\n")
        append("storeAtFort=false\nsound=true\ncash=100.0\noxen=6\nfood=600\n")
        append("clothing=5\nammo=120\nwheels=1\naxles=1\ntongues=1\nphase=LANDMARK\n")
        for (i in 0..4) append("p$i=Person$i,100,true,\n")
    }

    @Test
    fun pause_opens_and_resumes() {
        val g = newGame()
        assertFalse(g.canPause(), "the title screen cannot be paused")
        g.intro()
        assertEquals(Phase.TRAVEL, g.phase)
        assertTrue(g.canPause())
        g.onTap("pause:open")
        assertEquals(Phase.PAUSE, g.phase)
        g.onTap("pause:resume")
        assertEquals(Phase.TRAVEL, g.phase)
    }

    @Test
    fun pause_cannot_open_from_the_title() {
        val g = newGame()
        g.onTap("pause:open")
        assertEquals(Phase.TITLE, g.phase)
    }

    @Test
    fun pause_exposes_save_and_quick_save_flags() {
        val g = newGame()
        g.intro()
        g.onTap("pause:open")
        g.onTap("pause:save")
        assertTrue(g.requestedSave)
        g.clearSaveRequest()
        assertFalse(g.requestedSave)

        g.onTap("pause:quicksave")
        assertTrue(g.requestedQuickSave)
        g.clearQuickSaveRequest()
        assertFalse(g.requestedQuickSave)

        g.onTap("pause:quickload")
        assertTrue(g.requestedQuickLoad)
        g.clearQuickLoadRequest()
        assertFalse(g.requestedQuickLoad)
    }

    @Test
    fun pause_load_and_management_return_to_the_pause_menu() {
        val g = newGame()
        g.intro()
        g.onTap("pause:open")

        g.onTap("pause:load")
        assertEquals(Phase.LOAD, g.phase)
        g.onTap("slots:back")
        assertEquals(Phase.PAUSE, g.phase)

        g.onTap("pause:manage")
        assertEquals(Phase.MANAGEMENT, g.phase)
        g.onTap("manage:back")
        assertEquals(Phase.PAUSE, g.phase)

        g.onTap("pause:resume")
        assertEquals(Phase.TRAVEL, g.phase)
    }

    @Test
    fun title_management_still_returns_to_the_title() {
        val g = newGame()
        g.onTap("title:manage")
        assertEquals(Phase.MANAGEMENT, g.phase)
        g.onTap("manage:back")
        assertEquals(Phase.TITLE, g.phase)
    }

    @Test
    fun saving_while_paused_keeps_the_underlying_phase() {
        val g = newGame()
        assertTrue(g.load(dallesSave()))
        assertEquals(Phase.LANDMARK, g.phase)
        g.onTap("pause:open")
        assertEquals(Phase.PAUSE, g.phase)

        val fresh = newGame(9L)
        assertTrue(fresh.load(g.save()))
        assertEquals(Phase.LANDMARK, fresh.phase, "a paused save should resume at the landmark")
        assertEquals(16, fresh.landmarkIndex)
    }

    @Test
    fun a_pause_button_is_shown_only_on_play_screens() {
        val title = newGame()
        assertTrue(title.render().hotspots.none { it.id == "pause:open" })
        title.intro()
        assertTrue(title.render().hotspots.any { it.id == "pause:open" })
    }

    @Test
    fun the_pause_screen_renders_on_every_viewport() {
        val widths = listOf(16, 20, 28, 36, 48, 60, 76, 100)
        for (w in widths) {
            for (h in listOf(10, 16, 22, 30, 44)) {
                val g = newGame()
                g.setViewport(w, h)
                g.intro()
                g.onTap("pause:open")
                if (g.phase != Phase.PAUSE) continue
                val s = g.render()
                for (hot in s.hotspots) {
                    assertTrue(
                        hot.x0 in 0 until w && hot.x1 in 0 until w &&
                            hot.y0 in 0 until h && hot.y1 in 0 until h,
                        "pause hotspot ${hot.id} outside ${w}x$h"
                    )
                }
                assertTrue(s.hotspots.any { it.id == "pause:resume" }, "resume missing at ${w}x$h")
            }
        }
    }
}
