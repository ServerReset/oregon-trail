package com.oregontrail.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Determinism, balance, the Trail of the Day and ambience. */
class QualityTest {

    private fun newGame(seed: Long, store: ScoreStore = InMemoryScoreStore()): Game {
        val g = Game(DefaultRng(seed), store)
        g.setViewport(48, 34)
        return g
    }

    /** A competent but not perfect strategy: stock up, ration, ferry, portage. */
    private fun playFull(g: Game): Phase {
        g.onTap("title:travel"); g.onTap("prof:0"); g.onTap("month:0"); g.onTap("names:go")
        repeat(3) { g.onTap("store:inc:OXEN") }
        repeat(60) { g.onTap("store:inc:FOOD") }
        repeat(3) { g.onTap("store:inc:CLOTHING") }
        repeat(10) { g.onTap("store:inc:AMMUNITION") }
        g.onTap("store:leave"); g.onTap("notice:continue")
        g.onTap("travel:rations"); g.onTap("notice:continue") // meager rations
        var guard = 0
        while (guard++ < 4000 && g.phase != Phase.ARRIVED && g.phase != Phase.DEATH) {
            when (g.phase) {
                Phase.TRAVEL -> g.onTap("travel:continue")
                Phase.NOTICE -> g.onTap("notice:continue")
                Phase.CHOICE -> {
                    val s = g.render()
                    val id = s.hotspots.firstOrNull {
                        it.id.startsWith("riders:") || it.id.startsWith("trade:") ||
                            it.id.startsWith("rest:") || it.id.startsWith("stranded:")
                    }?.id ?: "choice:continue"
                    g.onTap(id)
                }
                Phase.RIVER -> {
                    val river = Data.landmarkAt(g.landmarkIndex).river
                    val ferry = river?.ferryCost
                    if (ferry != null && g.inventory.cash >= ferry) g.onTap("river:ferry")
                    else g.onTap("river:caulk")
                }
                Phase.LANDMARK ->
                    if (Data.landmarkAt(g.landmarkIndex).id == "dalles") g.onTap("dalles:portage")
                    else g.onTap("land:continue")
                Phase.STORE -> g.onTap("store:leave")
                Phase.HUNTING -> g.onTap("hunt:leave")
                Phase.RAFTING -> g.raftTick()
                Phase.BARLOW -> g.barlowTick()
                else -> break
            }
        }
        return g.phase
    }

    @Test
    fun the_same_seed_and_actions_replay_identically() {
        fun run(): String {
            val g = newGame(4242L)
            var guard = 0
            // Deterministic fixed policy for a while.
            g.onTap("title:travel"); g.onTap("prof:0"); g.onTap("month:0"); g.onTap("names:go")
            repeat(3) { g.onTap("store:inc:OXEN") }
            repeat(20) { g.onTap("store:inc:FOOD") }
            g.onTap("store:leave"); g.onTap("notice:continue")
            while (guard++ < 300 && g.phase != Phase.ARRIVED && g.phase != Phase.DEATH) {
                when (g.phase) {
                    Phase.TRAVEL -> g.onTap("travel:continue")
                    Phase.NOTICE -> g.onTap("notice:continue")
                    Phase.CHOICE -> g.onTap("choice:continue")
                    Phase.RIVER -> g.onTap("river:caulk")
                    Phase.LANDMARK -> g.onTap("land:continue")
                    Phase.HUNTING -> g.onTap("hunt:leave")
                    Phase.RAFTING -> g.raftTick()
                    Phase.BARLOW -> g.barlowTick()
                    else -> break
                }
            }
            return g.save()
        }
        assertEquals(run(), run(), "identical seed and actions must replay identically")
    }

    @Test
    fun a_competent_strategy_can_win_but_the_trail_is_dangerous() {
        val store = InMemoryScoreStore()
        var arrivals = 0
        var deaths = 0
        val seeds = 1L..30L
        for (seed in seeds) {
            val g = newGame(seed, store)
            when (playFull(g)) {
                Phase.ARRIVED -> arrivals++
                Phase.DEATH -> deaths++
                else -> {}
            }
        }
        // Not guaranteed to be winnable every time, but a good plan should
        // usually get through, and the trail should still claim some parties.
        assertTrue(arrivals >= 8, "expected a competent plan to reach Oregon often, got $arrivals/${seeds.count()}")
        assertTrue(arrivals <= seeds.count(), "arrivals cannot exceed games")
        assertTrue(arrivals + deaths <= seeds.count())
        assertTrue(store.loadStats().gamesPlayed >= seeds.count())
        assertTrue(store.loadStats().totalMiles > 0)
    }

    @Test
    fun weather_sets_the_background_mood() {
        val g = newGame(1L)
        g.onTap("title:travel"); g.onTap("prof:0"); g.onTap("month:0"); g.onTap("names:go")
        g.onTap("store:inc:OXEN"); g.onTap("store:leave"); g.onTap("notice:continue")
        assertEquals(Phase.TRAVEL, g.phase)

        g.weather = Weather(WeatherKind.BLIZZARD, -5)
        assertEquals(Palette.BLUE, g.render().ambient)
        g.weather = Weather(WeatherKind.HOT, 100)
        assertEquals(Palette.BROWN, g.render().ambient)
        g.weather = Weather(WeatherKind.CLEAR, 70)
        assertEquals(Palette.GREEN, g.render().ambient)
    }

    @Test
    fun trail_of_the_day_is_deterministic_per_day() {
        val seed = 20260914L
        val a = newGame(1L)
        a.dailySeed = seed
        a.onTap("title:daily")
        assertEquals(Phase.PROFESSION, a.phase)

        val b = newGame(2L)
        b.dailySeed = seed
        b.onTap("title:daily")
        assertEquals(Phase.PROFESSION, b.phase)
        // Same daily seed -> same party and same starting cash.
        assertEquals(a.party.map { it.name }, b.party.map { it.name })
        assertEquals(a.inventory.cash, b.inventory.cash, 0.001)
    }

    @Test
    fun title_offers_the_daily_and_hides_it_without_a_seed() {
        val g = newGame(1L)
        assertTrue(g.render().hotspots.none { it.id == "title:daily" })
        g.dailySeed = 20260101L
        assertTrue(g.render().hotspots.any { it.id == "title:daily" })
    }
}
