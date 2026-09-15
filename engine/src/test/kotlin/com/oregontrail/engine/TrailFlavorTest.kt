package com.oregontrail.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The postcard quotes, affordability hints and landmark flourish. */
class TrailFlavorTest {

    private fun travelGame(seed: Long = 4L, cash: Double = 200.0): Game {
        val g = Game(DefaultRng(seed), InMemoryScoreStore())
        g.setViewport(70, 44)
        g.onTap("title:travel"); g.onTap("prof:0"); g.onTap("month:0"); g.onTap("names:go")
        g.onTap("store:inc:OXEN"); repeat(10) { g.onTap("store:inc:FOOD") }
        g.onTap("store:leave"); g.onTap("notice:continue")
        g.inventory.cash = cash
        return g
    }

    private fun walkTo(g: Game, phase: Phase, limit: Int = 200) {
        var guard = 0
        while (guard++ < limit && g.phase != phase) {
            when (g.phase) {
                Phase.TRAVEL -> g.onTap("travel:continue")
                Phase.NOTICE -> g.onTap("notice:continue")
                Phase.CHOICE -> g.onTap("choice:continue")
                Phase.RIVER -> g.onTap("river:caulk")
                Phase.HUNTING -> g.onTap("hunt:leave")
                else -> break
            }
        }
    }

    @Test
    fun every_landmark_has_a_postcard_quote() {
        for (lm in Data.landmarks) {
            assertTrue(
                TrailQuotes.byLandmark.containsKey(lm.id),
                "no postcard quote for ${lm.id}"
            )
        }
        assertEquals(Data.landmarks.size, TrailQuotes.byLandmark.size)
    }

    @Test
    fun the_landmark_screen_shows_its_quote_when_there_is_room() {
        val g = travelGame()
        walkTo(g, Phase.LANDMARK)
        if (g.phase != Phase.LANDMARK) return
        val lm = Data.landmarkAt(g.landmarkIndex)
        val text = g.render().toText().replace(Regex("\\s+"), " ")
        val quote = TrailQuotes.byLandmark.getValue(lm.id).replace(Regex("\\s+"), " ")
        assertTrue(text.contains(quote), "the postcard should appear for ${lm.id}")
    }

    @Test
    fun river_options_warn_when_you_cannot_afford_them() {
        val g = travelGame(cash = 0.0)
        walkTo(g, Phase.RIVER)
        if (g.phase != Phase.RIVER) return
        val river = Data.landmarkAt(g.landmarkIndex).river ?: return
        val text = g.render().toText()
        if (river.ferryCost != null || river.guideCost != null) {
            assertTrue(text.contains("can't afford"), "broke travellers should see a warning")
        }
    }

    @Test
    fun the_dalles_marks_the_barlow_toll_for_the_broke() {
        val g = Game(DefaultRng(1L), InMemoryScoreStore())
        g.setViewport(70, 44)
        val save = buildString {
            append("v=1\nocc=BANKER\nmonth=MAY\ndate=1848,8,1\nweather=CLEAR,72\n")
            append("miles=2040\nlandmark=16\npace=STEADY\nrations=FILLING\n")
            append("storeAtFort=false\nsound=true\ncash=1.0\noxen=6\nfood=600\n")
            append("clothing=5\nammo=120\nwheels=1\naxles=1\ntongues=1\nphase=LANDMARK\n")
            for (i in 0..4) append("p$i=Person$i,100,true,\n")
        }
        assertTrue(g.load(save))
        assertTrue(g.render().toText().contains("can't afford"), "Barlow should be flagged")
    }

    @Test
    fun events_play_a_fitting_sound_cue() {
        val g = travelGame()
        g.debugFireEvent("wild_horses")
        assertEquals(Sound.GOOD, g.pendingSound)
        g.debugFireEvent("prairie_fire")
        assertEquals(Sound.BAD, g.pendingSound)
        g.debugFireEvent("snakebite")
        assertTrue(g.pendingSound == Sound.INJURY || g.pendingSound == Sound.DEATH)
    }

    @Test
    fun resting_tells_a_campfire_story() {
        val g = travelGame()
        g.onTap("travel:rest")
        g.onTap("rest:2")
        assertTrue(
            g.noticeLines.any { it.contains("Around the fire:") },
            "a rest should include a campfire story"
        )
        assertTrue(CampStories.templates.size >= 6)
    }

    @Test
    fun a_landmark_arrival_title_flourishes() {
        val g = travelGame()
        walkTo(g, Phase.NOTICE)
        // Make sure we are on the landmark notice, then find a blinking frame.
        if (g.phase != Phase.NOTICE || g.noticeTitle != "Landmark") return
        var sawFlourish = false
        repeat(4) {
            if (g.noticeHeading().startsWith("* ")) sawFlourish = true
            g.animate()
        }
        assertTrue(sawFlourish, "the landmark title should flourish on some frames")
    }
}
