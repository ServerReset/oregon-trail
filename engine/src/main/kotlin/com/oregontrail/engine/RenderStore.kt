package com.oregontrail.engine

import kotlin.math.min

internal fun Game.renderStore(screen: Screen) {
    if (ultraCompact) {
        renderStoreCompact(screen)
        return
    }
    val title = if (storeAtFort) "FORT TRADING POST" else "MATT'S GENERAL STORE"
    screen.center(0, title, Palette.BRIGHT_GREEN, bold = true)
    pauseButton(screen)
    screen.text(marginX + 1, 1, "You have $${"%.2f".format(inventory.cash)}", Palette.BRIGHT_YELLOW, bold = true)
    screen.hline(marginX, 2, contentW, '-', Palette.DIM)
    // Adaptive column layout that always fits the smallest supported width.
    val available = (contentW - 2).coerceAtLeast(20)
    val btnW = 7          // "[-][+]"
    val qtyW = 5
    val priceW = 7
    val gap = 1
    val nameW = (available - btnW - qtyW - priceW - gap).coerceIn(6, 16)
    val nameX = marginX + 1
    val priceX = nameX + nameW
    val qtyX = priceX + priceW
    val btnX = qtyX + qtyW + gap
    screen.text(nameX, 3, "Item", Palette.GRAY)
    screen.text(priceX, 3, "Price".padStart(priceW), Palette.GRAY)
    screen.text(qtyX, 3, "Qty".padStart(qtyW), Palette.GRAY)
    screen.text(btnX, 3, "Buy", Palette.GRAY)

    var y = 4
    Item.entries.forEachIndexed { index, item ->
        val name = "$index ${shortItemName(item)}".padEnd(nameW).take(nameW)
        screen.text(nameX, y, name, Palette.GREEN)
        screen.text(priceX, y, ("$" + "%.2f".format(priceOf(item))).padStart(priceW), Palette.GRAY)
        screen.text(qtyX, y, displayQty(item).toString().padStart(qtyW), Palette.WHITE)
        screen.text(btnX, y, "[-][+]", Palette.BRIGHT_GREEN)
        screen.hotspot("store:dec:${item.name}", btnX, y, 3)
        screen.hotspot("store:inc:${item.name}", btnX + 3, y, 4)
        y++
    }
    screen.hline(marginX, y, contentW, '-', Palette.DIM)
    y++
    val leave = "[ Leave the store ]"
    screen.text(marginX + 1, y, leave, Palette.BRIGHT_GREEN, bold = true)
    screen.hotspot("store:leave", marginX + 1, y, leave.length)
    screen.footer(rows, "Ammunition is sold by the box of 20 bullets")
}

/**
 * Watch / cover-screen store: each row is a tappable "buy one" button (hold to
 * repeat). No price or quantity columns are needed at this size.
 */
internal fun Game.renderStoreCompact(screen: Screen) {
    screen.center(0, "STORE", Palette.BRIGHT_GREEN, bold = true)
    screen.text(0, 1, "You have \$${"%.0f".format(inventory.cash)}".take(cols), Palette.BRIGHT_YELLOW, bold = true)
    var y = 2
    Item.entries.forEachIndexed { i, item ->
        if (y >= rows - 1) return@forEachIndexed
        val label = "$i ${shortItemName(item)} ${displayQty(item)} \$${"%.0f".format(priceOf(item))}"
        screen.text(0, y, label.take(cols), Palette.GREEN)
        screen.hotspot("store:inc:${item.name}", 0, y, min(cols, label.length))
        y++
    }
    val leave = "[ Leave ]"
    screen.text(0, rows - 1, leave, Palette.BRIGHT_GREEN, bold = true)
    screen.hotspot("store:leave", 0, rows - 1, leave.length)
}

internal fun Game.shortItemName(item: Item): String = when (item) {
    Item.OXEN -> "Oxen"
    Item.FOOD -> "Food"
    Item.CLOTHING -> "Cloths"
    Item.AMMUNITION -> "Ammo"
    Item.WHEEL -> "Wheel"
    Item.AXLE -> "Axle"
    Item.TONGUE -> "Tongue"
}
