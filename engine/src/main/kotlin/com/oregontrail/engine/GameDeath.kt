package com.oregontrail.engine


internal fun Game.dieOf(cause: String) {
        deathCause = cause
        val leader = party.firstOrNull()?.name ?: "Traveler"
        val epitaph = "Here lies $leader, died of $cause on the Oregon Trail."
        lastGravestone = epitaph
        addJournal("$leader died of $cause.")
        val lm = Data.landmarkAt(landmarkIndex)
        val grave = Grave(leader, cause, lm.id, epitaph)
        graves.add(grave)
        if (graves.size > Game.GRAVE_LIMIT) graves.removeAt(0)
        scores.addGrave(grave)
        scores.saveGravestone(epitaph)
        recordStats { it.copy(deaths = it.deaths + 1, totalMiles = it.totalMiles + miles) }
        if (cause.contains("dysentery", ignoreCase = true)) unlock(Achievements.DYSENTERY)
        pendingSound = Sound.DEATH
        phase = Phase.DEATH
    }

    // ====================================================================
    //  Hunting
    // ====================================================================
