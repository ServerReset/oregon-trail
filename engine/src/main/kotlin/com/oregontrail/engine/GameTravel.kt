package com.oregontrail.engine


internal enum class DayResult { OK, EVENT, LANDMARK, DEATH }

internal fun Game.handleTravelMenu(action: String) {
        when (action) {
            "continue" -> continueOnTrail()
            "supplies" -> showNotice("Your Supplies", suppliesLines(), Phase.TRAVEL)
            "map" -> { pendingSound = Sound.PAGE; phase = Phase.MAP }
            "journal" -> { pendingSound = Sound.PAGE; journalPage = 0; phase = Phase.JOURNAL }
            "save" -> requestedSave = true
            "pace" -> cyclePace()
            "rations" -> cycleRations()
            "rest" -> startRest()
            "trade" -> attemptTrade()
            "hunt" -> startHunt(Phase.TRAVEL)
        }
    }

internal fun Game.cyclePace() {        pace = when (pace) {
            Pace.STEADY -> Pace.STRENUOUS
            Pace.STRENUOUS -> Pace.GRUELING
            Pace.GRUELING -> Pace.STEADY
        }
        showNotice(
            "Pace",
            listOf("You will now travel at a ${pace.displayName.lowercase()} pace.",
                "Top speed is about ${pace.milesPerDay} miles per day, but",
                "hard driving wears out your oxen."),
            Phase.TRAVEL
        )
    }

internal fun Game.cycleRations() {
        rations = when (rations) {
            Rations.FILLING -> Rations.MEAGER
            Rations.MEAGER -> Rations.BARE_BONES
            Rations.BARE_BONES -> Rations.FILLING
        }
        showNotice(
            "Food Rations",
            listOf("The party will now eat ${rations.displayName.lowercase()} rations:",
                "${rations.poundsPerPersonPerDay} pounds of food per person each day.",
                "Meager rations can make folks weak and sick."),
            Phase.TRAVEL
        )
    }

internal fun Game.cycleDifficulty() {
        difficulty = when (difficulty) {
            Difficulty.EASY -> Difficulty.NORMAL
            Difficulty.NORMAL -> Difficulty.HARD
            Difficulty.HARD -> Difficulty.EASY
        }
        showNotice(
            "Difficulty",
            listOf(
                "Difficulty is now ${difficulty.displayName}.",
                "Trouble on the trail strikes more often on Hard and",
                "less often on Easy. Your score is unaffected."
            ),
            Phase.MANAGEMENT
        )
    }

    /** The average condition of the oxen as a word. */
fun Game.oxCondition(): String = when {
        oxHealth >= 75 -> "good"
        oxHealth >= 45 -> "fair"
        oxHealth >= 20 -> "poor"
        else -> "failing"
    }

    /** Records a dated line in the traveler's journal. */
    /** Starts a run seeded by today's date: the same trail for everyone. */
internal fun Game.startDailyChallenge() {
        if (dailySeed == 0L) return
        rng.reseed(dailySeed)
        newRun(Occupation.BANKER, TravelMonth.MARCH)
        addJournal("Trail of the Day: a fresh start on a shared trail.")
        pendingSound = Sound.GOOD
        phase = Phase.PROFESSION
    }
