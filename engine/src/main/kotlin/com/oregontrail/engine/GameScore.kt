package com.oregontrail.engine

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

internal fun Game.computeScore(): Int {
        val parts = scoreParts()
        return parts.last().second
    }

    /** Returns labelled score components; the final entry is the total. */
internal fun Game.scoreParts(): List<Pair<String, Int>> {
        val survivors = aliveCount * 400
        val oxen = inventory.oxen * 30
        val food = inventory.food / 5
        val clothing = inventory.clothing * 15
        val ammo = inventory.ammo / 2
        val spares = (inventory.wheels + inventory.axles + inventory.tongues) * 20
        val cash = (inventory.cash / 10).toInt()
        val subtotal = 1500 + survivors + oxen + food + clothing + ammo + spares + cash
        val total = subtotal * occupation.multiplier
        return listOf(
            "Survivors ($aliveCount x 400)" to survivors,
            "Oxen (${inventory.oxen} x 30)" to oxen,
            "Food (${inventory.food} lb)" to food,
            "Clothing (${inventory.clothing})" to clothing,
            "Ammunition (${inventory.ammo})" to ammo,
            "Spare parts" to spares,
            "Cash" to cash,
            "Arrival bonus" to 1500,
            "Subtotal x${occupation.multiplier} (${occupation.displayName})" to total
        )
    }

    // ====================================================================
    //  Notices
    // ====================================================================
