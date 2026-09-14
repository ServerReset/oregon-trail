package com.oregontrail.engine

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GameTest {

    private fun newGame(seed: Long = 1L): Game {
        val rng = DefaultRng(seed)
        val game = Game(rng, InMemoryScoreStore())
        game.setViewport(48, 34)
        return game
    }

    private fun Game.doIntro() {
        onTap("title:travel")
        onTap("prof:0")
        onTap("month:0")
        onTap("names:go")
    }

    private fun Game.buyBasics() {
        repeat(3) { onTap("store:inc:OXEN") }
        repeat(40) { onTap("store:inc:FOOD") }
        repeat(5) { onTap("store:inc:CLOTHING") }
        repeat(20) { onTap("store:inc:AMMUNITION") }
        repeat(2) { onTap("store:inc:WHEEL") }
        repeat(2) { onTap("store:inc:AXLE") }
        repeat(2) { onTap("store:inc:TONGUE") }
        onTap("store:leave")
    }

    @Test
    fun title_renders_with_menu() {
        val game = newGame()
        val screen = game.render()
        assertTrue(screen.toText().isNotBlank())
        assertTrue(screen.hotspots.any { it.id == "title:travel" })
    }

    @Test
    fun store_purchases_deduct_cash_and_add_goods() {
        val game = newGame()
        game.doIntro()
        val start = game.inventory.cash
        game.onTap("store:inc:OXEN")
        assertEquals(2, game.inventory.oxen)
        assertEquals(start - 20.0, game.inventory.cash, 0.001)
        game.onTap("store:inc:FOOD")
        assertEquals(50, game.inventory.food)
        game.onTap("store:dec:FOOD")
        assertEquals(0, game.inventory.food)
    }

    @Test
    fun cannot_spend_more_than_available_cash() {
        val game = newGame()
        game.doIntro()
        // Farmer has $400; ensure we cannot overspend.
        game.onTap("prof:2")
        game.onTap("month:0")
        var guard = 0
        while (game.inventory.cash >= 20 && guard++ < 100) game.onTap("store:inc:OXEN")
        assertTrue(game.inventory.cash >= 0.0)
        assertTrue(game.inventory.cash < 20.0)
    }

    @Test
    fun leaving_without_oxen_is_refused() {
        val game = newGame()
        game.doIntro()
        game.onTap("store:leave")
        assertEquals(Phase.NOTICE, game.phase)
        game.onTap("notice:continue")
        assertEquals(Phase.STORE, game.phase)
    }

    @Test
    fun travel_progresses_and_can_reach_oregon() {
        var arrived = false
        var seed = 1L
        while (!arrived && seed <= 8L) {
            val game = newGame(seed)
            game.doIntro()
            game.buyBasics()
            assertEquals(Phase.NOTICE, game.phase)
            game.onTap("notice:continue")
            var guard = 0
            while (guard++ < 4000) {
                if (game.phase == Phase.TRAVEL && game.inventory.food < 200) {
                    game.inventory.food = 800 // keep the state-machine test focused
                }
                when (game.phase) {
                    Phase.TRAVEL -> game.onTap("travel:continue")
                    Phase.NOTICE -> game.onTap("notice:continue")
                    Phase.CHOICE -> game.onTap("choice:continue")
                    Phase.RIVER -> {
                        val lm = Data.landmarkAt(game.landmarkIndex)
                        val ferry = lm.river?.ferryCost
                        if (ferry != null && game.inventory.cash >= ferry) game.onTap("river:ferry")
                        else game.onTap("river:caulk")
                    }
                    Phase.LANDMARK -> {
                        if (Data.landmarkAt(game.landmarkIndex).id == "dalles") {
                            if (game.inventory.cash >= 5) game.onTap("dalles:barlow")
                            else game.onTap("dalles:raft")
                        } else game.onTap("land:continue")
                    }
                    Phase.ARRIVED -> { arrived = true; break }
                    Phase.DEATH -> break
                    Phase.STORE -> game.onTap("store:leave")
                    Phase.HUNTING -> game.onTap("hunt:leave")
                    Phase.RAFTING -> game.raftTick()
                    Phase.BARLOW -> game.barlowTick()
                    else -> break
                }
            }
            if (game.phase == Phase.ARRIVED) arrived = true
            seed++
        }
        assertTrue(arrived, "Expected at least one seeded playthrough to reach Oregon")
    }

    @Test
    fun hunting_field_runs() {
        val rng = ScriptedRng.of(*DoubleArray(400) { 0.42 })
        val field = HuntField(30, 12, rng, listOf(AnimalKind.RABBIT, AnimalKind.DEER))
        repeat(50) {
            field.move(1, 0)
            field.shoot()
            field.tick()
        }
        assertTrue(field.shotsFired > 0)
        assertTrue(field.meat >= 0)
    }

    @Test
    fun map_and_supplies_screens_render() {
        val game = newGame()
        game.doIntro()
        game.buyBasics()
        game.onTap("notice:continue")
        game.onTap("travel:map")
        assertEquals(Phase.MAP, game.phase)
        assertTrue(game.render().toText().contains("MAP"))
        game.onTap("map:back")
        game.onTap("travel:supplies")
        assertTrue(game.render().toText().contains("Food"))
    }

    @Test
    fun render_dump_for_visual_review() {
        val game = newGame(7L)
        val out = File("/tmp/ot-screens.txt")
        val sb = StringBuilder()
        fun dump(name: String) {
            sb.append("===== $name =====\n")
            sb.append(game.render().toText())
            sb.append("\n\n")
        }
        dump("TITLE")
        game.onTap("title:travel")
        dump("PROFESSION")
        game.onTap("prof:0")
        dump("MONTH")
        game.onTap("month:0")
        dump("NAMES")
        game.onTap("names:go")
        dump("STORE")
        game.buyBasics()
        dump("NOTICE-LEAVE")
        game.onTap("notice:continue")
        dump("TRAVEL")
        game.onTap("travel:map")
        dump("MAP")
        game.onTap("map:back")
        game.onTap("travel:supplies")
        dump("SUPPLIES")
        game.onTap("notice:continue")
        game.onTap("travel:pace")
        dump("PACE")
        game.onTap("notice:continue")
        game.onTap("travel:hunt")
        dump("HUNTING")
        game.onTap("hunt:leave")
        dump("AFTER-HUNT")
        // Force a river and a landmark screen.
        game.inventory.food = 5000
        var guard = 0
        while (game.phase != Phase.RIVER && guard++ < 500) {
            when (game.phase) {
                Phase.TRAVEL -> game.onTap("travel:continue")
                Phase.NOTICE -> game.onTap("notice:continue")
                Phase.CHOICE -> game.onTap("choice:continue")
                Phase.LANDMARK -> game.onTap("land:continue")
                Phase.HUNTING -> game.onTap("hunt:leave")
                else -> break
            }
        }
        dump("RIVER")
        game.onTap("river:caulk")
        dump("RIVER-RESULT")
        // Run to the end.
        guard = 0
        while (game.phase != Phase.ARRIVED && game.phase != Phase.DEATH && guard++ < 800) {
            game.inventory.food = 5000
            when (game.phase) {
                Phase.TRAVEL -> game.onTap("travel:continue")
                Phase.NOTICE -> game.onTap("notice:continue")
                Phase.CHOICE -> game.onTap("choice:continue")
                Phase.RIVER -> game.onTap("river:caulk")
                Phase.LANDMARK -> {
                    val id = Data.landmarkAt(game.landmarkIndex).id
                    if (id == "dalles") game.onTap("dalles:barlow") else game.onTap("land:continue")
                }
                Phase.STORE -> game.onTap("store:leave")
                Phase.HUNTING -> game.onTap("hunt:leave")
                Phase.RAFTING -> game.raftTick()
                Phase.BARLOW -> game.barlowTick()
                else -> break
            }
        }
        if (game.phase == Phase.ARRIVED) dump("ARRIVED")
        out.writeText(sb.toString())
        assertTrue(out.exists())
    }

    @Test
    fun hunting_shooting_an_aligned_animal_scores_a_hit() {
        // 0.99 keeps animals still and prevents new spawns, so the shot is deterministic.
        val rng = ScriptedRng.of(*DoubleArray(200) { 0.99 })
        val field = HuntField(20, 10, rng, listOf(AnimalKind.RABBIT))
        val startMeat = field.meat
        field.debugPlaceAnimal(field.hunterX + 3, field.hunterY, AnimalKind.RABBIT)
        assertTrue(field.debugAnimalCount() >= 1)
        field.move(1, 0)          // aim right
        assertTrue(field.shoot(), "shot should be able to leave the hunter")
        field.tick()
        assertTrue(field.lastShotHit, "bullet should have struck the aligned animal")
        assertTrue(field.meat > startMeat, "meat should increase after a hit")
        assertEquals(AnimalKind.RABBIT, field.lastKill)
    }

    @Test
    fun raft_field_completes_and_dodging_avoids_damage() {
        // 0.99: chance() is always false, so no rocks spawn and the run is clean.
        val calm = ScriptedRng.of(*DoubleArray(400) { 0.99 })
        val safe = RaftField(20, 10, calm, totalProgress = 25)
        var guard = 0
        while (!safe.done && guard++ < 200) safe.tick()
        assertTrue(safe.done)
        assertTrue(safe.success)
        assertEquals(0, safe.damage())

        // 0.0: rocks spawn constantly; steer the raft into them and take damage.
        val rough = ScriptedRng.of(*DoubleArray(800) { 0.0 })
        val dangerous = RaftField(16, 8, rough, totalProgress = 200)
        guard = 0
        while (!dangerous.done && guard++ < 400) {
            dangerous.moveLeft()
            dangerous.tick()
        }
        assertTrue(dangerous.done)
        assertTrue(dangerous.damage() > 0)
    }

    @Test
    fun dates_advance_across_month_and_year_ends() {
        val d = GameDate(1848, 5, 31)
        d.plusDays(1)
        assertEquals(6, d.month)
        assertEquals(1, d.day)

        val dec = GameDate(1848, 12, 31)
        dec.plusDays(1)
        assertEquals(1849, dec.year)
        assertEquals(1, dec.month)
        assertEquals(1, dec.day)

        val feb = GameDate(1848, 2, 28) // 1848 is a leap year
        feb.plusDays(1)
        assertEquals(29, feb.day)
        feb.plusDays(1)
        assertEquals(3, feb.month)
        assertEquals(1, feb.day)
    }

    @Test
    fun rafting_finale_is_reachable_and_completes() {
        val save = buildString {
            append("v=1\nocc=BANKER\nmonth=MAY\ndate=1848,8,1\nweather=CLEAR,72\n")
            append("miles=2040\nlandmark=16\npace=STEADY\nrations=FILLING\n")
            append("storeAtFort=false\nsound=true\ncash=100.0\noxen=6\nfood=600\n")
            append("clothing=5\nammo=120\nwheels=1\naxles=1\ntongues=1\nphase=LANDMARK\n")
            for (i in 0..4) append("p$i=Person$i,100,true,\n")
        }
        val g = Game(DefaultRng(5L), InMemoryScoreStore())
        g.setViewport(40, 30)
        assertTrue(g.load(save), "save should load")
        assertEquals(Phase.LANDMARK, g.phase)
        g.onTap("dalles:raft")
        assertEquals(Phase.RAFTING, g.phase)
        var guard = 0
        while (g.phase == Phase.RAFTING && guard++ < 500) g.raftTick()
        assertTrue(g.phase == Phase.NOTICE || g.phase == Phase.ARRIVED,
            "rafting should finish the trail, was ${g.phase}")
    }

    @Test
    fun save_and_restore_round_trip() {
        val game = newGame(11L)
        game.doIntro()
        game.buyBasics()
        game.onTap("notice:continue")
        // Move a little way down the trail.
        repeat(5) {
            if (game.phase == Phase.TRAVEL) game.onTap("travel:continue")
            if (game.phase == Phase.NOTICE) game.onTap("notice:continue")
        }
        val snapshot = game.save()
        assertTrue(snapshot.contains("v=1"))

        val restored = Game(DefaultRng(999L), InMemoryScoreStore())
        restored.setViewport(48, 34)
        assertTrue(restored.load(snapshot))
        assertEquals(game.miles, restored.miles)
        assertEquals(game.date.toString(), restored.date.toString())
        assertEquals(game.inventory.food, restored.inventory.food)
        assertEquals(game.inventory.cash, restored.inventory.cash, 0.001)
        assertEquals(game.party.map { it.name }, restored.party.map { it.name })
        // Restored game must be playable.
        if (restored.phase == Phase.TRAVEL) {
            restored.onTap("travel:continue")
            assertTrue(restored.phase != Phase.TITLE)
        }
    }

    @Test
    fun load_rejects_garbage() {
        val game = newGame()
        assertFalse(game.load("not a save file"))
    }
}
