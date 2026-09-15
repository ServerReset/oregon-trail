package com.oregontrail.engine

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

fun Game.huntTick() {
        if (phase == Phase.HUNTING) huntField?.tick()
    }

    /** Advances the Columbia River rafting finale; called on a timer by the front-end. */
fun Game.raftTick() {
        if (phase != Phase.RAFTING) return
        val field = raftField ?: return
        field.tick()
        if (field.done) finishRaft()
    }

    /** Advances the Barlow Road climb; called on a timer by the front-end. */
fun Game.barlowTick() {
        if (phase != Phase.BARLOW) return
        val field = barlowField ?: return
        field.tick()
        if (field.done) finishBarlow()
    }

    // ====================================================================
    //  Store
    // ====================================================================
