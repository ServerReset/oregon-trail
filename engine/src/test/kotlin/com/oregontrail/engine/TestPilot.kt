package com.oregontrail.engine

/**
 * A simple, competent bot used by the balance tests. It plays whole games in
 * the JVM so we can measure arrival rates, scores and pace without a device.
 * It is deliberately conservative (buys a full outfit, rests when hurt, takes
 * safe river options) to approximate a thoughtful player.
 */
class TestPilot(private val seed: Long, private val occupation: Occupation, private val difficulty: Difficulty) {

    private var hunts = 0
    private var rests = 0

    data class Outcome(
        val arrived: Boolean, val survived: Int, val score: Int, val days: Int, val phase: Phase,
        val food: Int, val ammo: Int, val cash: Int, val oxen: Int, val clothing: Int,
        val hunts: Int, val rests: Int, val deathCauses: List<String>
    )

    fun play(): Outcome {
        val g = Game(DefaultRng(seed), InMemoryScoreStore())
        g.setViewport(48, 34)
        g.occupation = occupation
        g.difficulty = difficulty
        g.onTap("title:travel")
        g.onTap("prof:${Occupation.entries.indexOf(occupation)}")
        g.onTap("month:0")
        g.onTap("names:go")
        outfit(g)
        hunts = 0
        rests = 0
        var guard = 0
        while (guard++ < 5000) {
            when (g.phase) {
                Phase.TRAVEL -> travel(g)
                Phase.NOTICE -> g.onTap("notice:continue")
                Phase.CHOICE -> choice(g)
                Phase.RIVER -> river(g)
                Phase.LANDMARK -> landmark(g)
                Phase.HUNTING -> hunt(g)
                Phase.BARLOW, Phase.RAFTING -> minigame(g)
                Phase.MAP -> g.onTap("map:back")
                Phase.JOURNAL -> g.onTap("journal:back")
                Phase.PAUSE -> g.onTap("pause:resume")
                Phase.ARRIVED, Phase.DEATH -> break
                else -> break
            }
        }
        val causes = g.party.filter { !it.alive }.map { it.condition ?: g.deathCause }
        return Outcome(
            g.phase == Phase.ARRIVED, g.party.count { it.alive }, g.lastScore,
            g.daysOnTrail(), g.phase,
            g.inventory.food, g.inventory.ammo, g.inventory.cash.toInt(), g.inventory.oxen,
            g.inventory.clothing, hunts, rests, causes
        )
    }

    private fun outfit(g: Game) {
        buy(g, "OXEN", 3)
        buy(g, "CLOTHING", 6)
        buy(g, "AMMUNITION", 5)
        buy(g, "WHEEL", 1); buy(g, "AXLE", 1); buy(g, "TONGUE", 1)
        // Spend most of what is left on food, keeping a small reserve for
        // ferries and fort restocking.
        repeat(80) { if (g.inventory.cash > 90.0) g.onTap("store:inc:FOOD") }
        g.onTap("store:leave")
        if (g.phase == Phase.NOTICE) g.onTap("notice:continue")
    }

    private fun buy(g: Game, item: String, taps: Int) {
        repeat(taps) { if (g.phase == Phase.STORE) g.onTap("store:inc:$item") }
    }

    private fun travel(g: Game) {
        val hunger = if (occupation == Occupation.FARMER) 380 else 280
        if (g.inventory.food < hunger && g.inventory.ammo >= 12) {
            hunts++; g.onTap("travel:hunt"); return
        }
        val hurt = g.party.any { it.alive && it.health < 45 } || g.oxHealth < 40
        if (hurt && g.inventory.food > 60) { rests++; g.onTap("travel:rest"); return }
        val want = when {
            g.inventory.food > 1200 -> Rations.FILLING
            g.inventory.food > 400 -> Rations.MEAGER
            else -> Rations.BARE_BONES
        }
        if (g.rations != want) { g.onTap("travel:rations"); return }
        g.onTap("travel:continue")
    }

    private fun choice(g: Game) {
        val ids = g.choiceOptions.map { it.second }
        val pick = ids.firstOrNull { it.startsWith("rest:3") }
            ?: ids.firstOrNull { it == "stranded:no" }
            ?: ids.firstOrNull { it == "riders:continue" }
            ?: ids.firstOrNull { it == "trade:no" }
            ?: ids.firstOrNull()
        if (pick != null) g.onTap(pick)
    }

    private fun river(g: Game) {
        val cost = Data.landmarkAt(g.landmarkIndex).river?.ferryCost
        val guide = Data.landmarkAt(g.landmarkIndex).river?.guideCost
        when {
            cost != null && g.inventory.cash >= cost -> g.onTap("river:ferry")
            guide != null && g.inventory.cash >= guide -> g.onTap("river:guide")
            else -> g.onTap("river:caulk")
        }
    }

    private fun landmark(g: Game) {
        val lm = Data.landmarkAt(g.landmarkIndex)
        if (lm.id == "dalles") { g.onTap("dalles:portage"); return }
        if (lm.kind == LandmarkKind.FORT && g.inventory.cash > 120 && g.inventory.food < 900) {
            g.onTap("land:buy")
            repeat(40) { if (g.phase == Phase.STORE && g.inventory.cash > 70.0) g.onTap("store:inc:FOOD") }
            if (g.phase == Phase.STORE) g.onTap("store:leave")
            return
        }
        g.onTap("land:continue")
    }

    private fun hunt(g: Game) {
        val dirs = listOf("hunt:up", "hunt:down", "hunt:left", "hunt:right")
        repeat(30) { i ->
            if (g.phase != Phase.HUNTING) return
            g.onTap(dirs[(seed.toInt() + i) % 4])
            g.onTap("hunt:shoot")
            g.huntTick()
        }
        if (g.phase == Phase.HUNTING) g.onTap("hunt:leave")
    }

    private fun minigame(g: Game) {
        repeat(80) {
            when (g.phase) {
                Phase.RAFTING -> { g.onTap("raft:right"); g.raftTick() }
                Phase.BARLOW -> { g.onTap("barlow:right"); g.barlowTick() }
                else -> return
            }
        }
    }
}
