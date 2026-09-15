package com.oregontrail.engine


internal fun Game.unitsPerTap(item: Item): Int = when (item) {
        Item.OXEN -> 2
        Item.FOOD -> 50
        Item.CLOTHING -> 1
        Item.AMMUNITION -> 20
        Item.WHEEL, Item.AXLE, Item.TONGUE -> 1
    }

internal fun Game.priceOf(item: Item): Double {
        val base = if (storeAtFort) item.fortPrice else item.basePrice
        // Bankers are shrewd traders and get a modest discount at forts.
        return if (storeAtFort && occupation == Occupation.BANKER) base * 0.9 else base
    }

internal fun Game.tapCost(item: Item): Double = when (item) {
        Item.OXEN -> priceOf(item)                 // per yoke
        Item.FOOD -> priceOf(item) * 50
        Item.CLOTHING -> priceOf(item)
        Item.AMMUNITION -> priceOf(item)           // per box
        Item.WHEEL, Item.AXLE, Item.TONGUE -> priceOf(item)
    }

internal fun Game.displayQty(item: Item): Int = when (item) {
        Item.OXEN -> inventory.oxen / 2
        Item.FOOD -> inventory.food
        Item.CLOTHING -> inventory.clothing
        Item.AMMUNITION -> inventory.ammo / 20
        Item.WHEEL -> inventory.wheels
        Item.AXLE -> inventory.axles
        Item.TONGUE -> inventory.tongues
    }

internal fun Game.changeItem(name: String, dir: Int) {
        val item = Item.entries.firstOrNull { it.name == name } ?: return
        if (dir > 0) {
            val cost = tapCost(item)
            if (inventory.cash + 0.001 < cost) { pendingSound = Sound.BAD; return }
            inventory.cash -= cost
            when (item) {
                Item.OXEN -> inventory.oxen += 2
                Item.FOOD -> inventory.food += 50
                Item.CLOTHING -> inventory.clothing += 1
                Item.AMMUNITION -> inventory.ammo += 20
                Item.WHEEL -> inventory.wheels += 1
                Item.AXLE -> inventory.axles += 1
                Item.TONGUE -> inventory.tongues += 1
            }
        } else {
            // Selling returns whole steps only, so stock can never go negative.
            val step = unitsPerTap(item)
            val have = when (item) {
                Item.OXEN -> inventory.oxen
                Item.FOOD -> inventory.food
                Item.CLOTHING -> inventory.clothing
                Item.AMMUNITION -> inventory.ammo
                Item.WHEEL -> inventory.wheels
                Item.AXLE -> inventory.axles
                Item.TONGUE -> inventory.tongues
            }
            if (have < step) return
            when (item) {
                Item.OXEN -> { inventory.oxen -= 2; inventory.cash += priceOf(item) }
                Item.FOOD -> { inventory.food -= 50; inventory.cash += priceOf(item) * 50 }
                Item.CLOTHING -> { inventory.clothing -= 1; inventory.cash += priceOf(item) }
                Item.AMMUNITION -> { inventory.ammo -= 20; inventory.cash += priceOf(item) }
                Item.WHEEL -> { inventory.wheels -= 1; inventory.cash += priceOf(item) }
                Item.AXLE -> { inventory.axles -= 1; inventory.cash += priceOf(item) }
                Item.TONGUE -> { inventory.tongues -= 1; inventory.cash += priceOf(item) }
            }
        }
    }

internal fun Game.leaveStore() {
        if (inventory.oxen < 2) {
            showNotice(
                "The Store",
                listOf(
                    "You cannot cross the plains without oxen.",
                    "You need at least one yoke (two oxen)."
                ),
                Phase.STORE
            )
            pendingSound = Sound.BAD
            return
        }
        if (!storeAtFort) {
            // Leaving Independence: begin the journey.
            phase = Phase.TRAVEL
            recordStats { it.copy(gamesPlayed = it.gamesPlayed + 1) }
            addJournal("We bought our supplies and left Independence for Oregon.")
            showNotice(
                "Heading Out",
                listOf(
                    "You load the wagon and roll out of",
                    "Independence on ${date}.",
                    "",
                    "The Oregon Trail stretches 2,040 miles",
                    "to the west. Good luck."
                ),
                Phase.TRAVEL
            )
        } else {
            phase = Phase.LANDMARK
        }
    }

    // ====================================================================
    //  Travel menu
    // ====================================================================
