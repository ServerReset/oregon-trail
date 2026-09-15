package com.oregontrail.engine

import kotlin.test.Test
import kotlin.test.assertTrue

class ViewportLayoutTest {

private val widths = listOf(16, 18, 20, 22, 24, 28, 32, 40, 48, 60, 76, 100)

    private val heights = listOf(10, 11, 12, 14, 16, 18, 22, 30, 40, 60)

    private fun assertHotspotsInside(s: Screen, w: Int, h: Int, label: String) {
        for (hot in s.hotspots) {
            assertTrue(
                hot.x0 in 0 until w && hot.x1 in 0 until w &&
                    hot.y0 in 0 until h && hot.y1 in 0 until h,
                "$label: hotspot ${hot.id} ${hot.x0},${hot.y0}-${hot.x1},${hot.y1} outside ${w}x$h"
            )
        }
    }

    private fun nextAction(g: Game, s: Screen, steps: Int): String? = when (g.phase) {
        Phase.TITLE -> "title:travel"
        Phase.PROFESSION -> "prof:0"
        Phase.MONTH -> "month:0"
        Phase.NAMES -> "names:go"
        Phase.STORE -> if (g.inventory.oxen == 0 && g.inventory.cash >= 20) "store:inc:OXEN" else "store:leave"
        Phase.TRAVEL -> {
            if (g.inventory.food < 300) g.inventory.food = 1200
            when {
                steps % 9 == 0 -> "travel:rest"     // leads to the rest CHOICE
                steps % 13 == 0 -> "travel:trade"   // leads to the trade CHOICE
                else -> "travel:continue"
            }
        }
        Phase.NOTICE -> "notice:continue"
        Phase.MAP -> "map:back"
        Phase.HUNTING -> "hunt:leave"
        Phase.RAFTING -> "raft:right"
        Phase.BARLOW -> "barlow:right"
        Phase.CHOICE -> s.hotspots.firstOrNull {
            it.id.startsWith("riders:") || it.id.startsWith("trade:") || it.id.startsWith("rest:")
        }?.id ?: "choice:continue"
        Phase.RIVER -> s.hotspots.firstOrNull { it.id == "river:ferry" }?.id ?: "river:caulk"
        Phase.LANDMARK -> {
            val dalles = s.hotspots.firstOrNull { it.id.startsWith("dalles:") }
            if (dalles != null) {
                // Exercise both endings: the rafting and Barlow Road minigames.
                if (steps % 2 == 0) {
                    s.hotspots.firstOrNull { it.id == "dalles:raft" }?.id
                        ?: s.hotspots.firstOrNull { it.id == "dalles:barlow" }?.id
                } else {
                    s.hotspots.firstOrNull { it.id == "dalles:barlow" }?.id
                        ?: s.hotspots.firstOrNull { it.id == "dalles:raft" }?.id
                }
            } else "land:continue"
        }
        Phase.DEATH, Phase.ARRIVED -> null
        else -> null
    }

    @Test
    fun every_phase_renders_safely_on_every_viewport() {
        for (w in widths) {
            for (h in heights) {
                val g = Game(DefaultRng(97L * w + h), InMemoryScoreStore())
                g.setViewport(w, h)
                var steps = 0
                while (steps++ < 2000) {
                    val s = g.render()
                    assertHotspotsInside(s, w, h, "phase=${g.phase} viewport=${w}x$h step=$steps")
                    val action = nextAction(g, s, steps) ?: break
                    g.onTap(action)
                    // Minigames advance on a timer in the app; drive them here.
                    if (g.phase == Phase.HUNTING) g.huntTick()
                    if (g.phase == Phase.RAFTING) g.raftTick()
                    if (g.phase == Phase.BARLOW) g.barlowTick()
                    if (g.phase == Phase.DEATH || g.phase == Phase.ARRIVED) {
                        val end = g.render()
                        assertHotspotsInside(end, w, h, "end viewport=${w}x$h")
                        break
                    }
                }
                assertTrue(
                    steps < 2000,
                    "did not terminate for viewport ${w}x$h: phase=${g.phase} miles=${g.miles} " +
                        "landmark=${g.landmarkIndex} alive=${g.party.count { it.alive }} " +
                        "oxen=${g.inventory.oxen} food=${g.inventory.food}"
                )
            }
        }
    }
}
