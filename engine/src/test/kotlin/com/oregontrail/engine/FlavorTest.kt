package com.oregontrail.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Flavor added on top of the core game: new events, sounds and rest tips. */
class FlavorTest {

    private fun travelGame(seed: Long = 1L): Game {
        val g = Game(DefaultRng(seed), InMemoryScoreStore())
        g.setViewport(48, 34)
        g.onTap("title:travel"); g.onTap("prof:0"); g.onTap("month:0"); g.onTap("names:go")
        g.onTap("store:inc:OXEN"); repeat(20) { g.onTap("store:inc:FOOD") }
        g.onTap("store:leave"); g.onTap("notice:continue")
        return g
    }

    @Test
    fun the_rainbow_event_unlocks_its_achievement_and_sound() {
        val g = travelGame()
        val msgs = g.debugFireEvent("rainbow")
        assertTrue(msgs.isNotEmpty(), "the rainbow should say something")
        assertTrue(Achievements.RAINBOW in g.achievements, "rainbow achievement should unlock")
        assertEquals(Sound.UNLOCK, g.pendingSound)
    }

    @Test
    fun abandoned_wagons_yield_a_spare_part() {
        val g = travelGame()
        val before = g.inventory.wheels + g.inventory.axles + g.inventory.tongues
        val msgs = g.debugFireEvent("abandoned_wagon")
        val after = g.inventory.wheels + g.inventory.axles + g.inventory.tongues
        assertTrue(msgs.isNotEmpty())
        assertEquals(before + 1, after)
    }

    @Test
    fun berries_and_springs_are_safe_and_kind() {
        val g = travelGame()
        val food = g.inventory.food
        g.debugFireEvent("berries")
        assertTrue(g.inventory.food > food, "berries should add food")

        g.party.forEach { it.health = 40 }
        g.debugFireEvent("hot_springs")
        assertTrue(g.party.any { it.health > 40 }, "the spring should heal someone")
    }

    @Test
    fun resting_offers_trail_wisdom_and_a_sound() {
        val g = travelGame()
        g.onTap("travel:rest")
        assertEquals(Phase.CHOICE, g.phase)
        g.onTap("rest:2")
        assertTrue(
            g.noticeLines.any { it.contains("Trail wisdom") },
            "resting should share a trail tip"
        )
        assertEquals(Sound.REST, g.pendingSound)
    }

    @Test
    fun travel_pages_and_pace_use_distinct_sounds() {
        val g = travelGame()
        g.onTap("travel:journal"); assertEquals(Sound.PAGE, g.pendingSound)
        g.onTap("journal:back")
        g.onTap("travel:map"); assertEquals(Sound.PAGE, g.pendingSound)
        g.onTap("map:back")
        g.onTap("travel:trade"); assertEquals(Sound.TRADE, g.pendingSound)
    }

    @Test
    fun reaching_a_landmark_plays_the_milestone_cue() {
        val g = travelGame(seed = 4L)
        var sawMilestone = false
        var guard = 0
        while (guard++ < 400) {
            if (g.pendingSound == Sound.MILESTONE) {
                sawMilestone = true
                break
            }
            when (g.phase) {
                Phase.TRAVEL -> g.onTap("travel:continue")
                Phase.NOTICE -> g.onTap("notice:continue")
                Phase.CHOICE -> g.onTap("choice:continue")
                Phase.RIVER -> g.onTap("river:caulk")
                Phase.HUNTING -> g.onTap("hunt:leave")
                else -> break
            }
        }
        assertTrue(sawMilestone, "reaching a landmark should play the milestone cue")
    }

    @Test
    fun there_is_plenty_of_talk_and_trail_wisdom() {
        assertTrue(Talk.lines.size >= 20, "more chatter on the trail")
        assertTrue(Talk.rumors.size >= 8, "more rumors")
        assertTrue(TrailTips.list.size >= 10, "more trail wisdom")
        assertTrue(Achievements.all.size >= 16)
    }
}
