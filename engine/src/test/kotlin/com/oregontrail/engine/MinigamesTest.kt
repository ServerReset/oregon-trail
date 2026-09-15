package com.oregontrail.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MinigamesTest {

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
}
