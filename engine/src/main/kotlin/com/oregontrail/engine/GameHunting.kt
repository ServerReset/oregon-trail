package com.oregontrail.engine


internal fun Game.huntPool(): List<AnimalKind> {
        val m = miles
        return when {
            m < 600 -> listOf(AnimalKind.RABBIT, AnimalKind.SQUIRREL, AnimalKind.DEER, AnimalKind.ANTELOPE, AnimalKind.BUFFALO)
            m < 1300 -> listOf(AnimalKind.RABBIT, AnimalKind.SQUIRREL, AnimalKind.DEER, AnimalKind.ANTELOPE, AnimalKind.WOLF)
            else -> listOf(AnimalKind.SQUIRREL, AnimalKind.DEER, AnimalKind.BEAR, AnimalKind.WOLF)
        }
    }

internal fun Game.startHunt(returnPhase: Phase) {
        if (inventory.ammo <= 0) {
            showNotice("Hunting", listOf("You are out of ammunition and cannot hunt."), returnPhase)
            pendingSound = Sound.BAD
            return
        }
        val (fieldW, fieldH) = huntSize()
        huntField = HuntField(fieldW, fieldH, rng, huntPool())
        huntReturn = returnPhase
        huntDays = 1
        phase = Phase.HUNTING
        pendingSound = Sound.SELECT
    }

internal fun Game.huntShoot() {
        val field = huntField ?: return
        if (inventory.ammo <= 0) { pendingSound = Sound.BAD; return }
        val before = field.kills
        if (field.shoot()) {
            inventory.ammo -= 1
            pendingSound = if (field.kills > before) Sound.HIT else Sound.SHOOT
        }
    }

internal fun Game.endHunt() {
        val field = huntField ?: return
        val raw = field.finish()
        val meat = if (occupation == Occupation.FARMER) {
            (raw * 1.5).toInt().coerceAtMost(field.carryLimit)
        } else raw
        inventory.food += meat
        date.plusDays(huntDays)
        consumeFood()
        addJournal("Hunted and brought back $meat pounds of meat.")
        if (meat >= field.carryLimit) unlock(Achievements.BIG_HUNT)
        if (field.kills >= 3) unlock(Achievements.SHARPSHOOTER)
        val lines = listOf(
            "You return to the wagon with $meat pounds",
            "of meat from ${field.kills} animal(s).",
            "You fired ${field.shotsFired} shots."
        )
        huntField = null
        showNotice("After the Hunt", lines, huntReturn)
        pendingSound = Sound.GOOD
    }

    // ====================================================================
    //  Choices
    // ====================================================================
