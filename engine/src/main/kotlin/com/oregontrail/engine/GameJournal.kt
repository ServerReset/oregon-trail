package com.oregontrail.engine


internal fun Game.addJournal(text: String) {
        val dateText = "${date.monthName} ${date.day}"
        journal.add(JournalEntry(dateText, text))
        if (journal.size > Game.JOURNAL_LIMIT) journal.removeAt(0)
    }

    /** Unlocks an achievement once, saving it and queuing a notice for the front-end. */
internal fun Game.unlock(id: String) {
        if (achievements.add(id)) {
            scores.saveAchievements(achievements)
            pendingUnlock = Achievements.name(id)
            addJournal("Achievement: ${Achievements.name(id)}")
            pendingSound = Sound.UNLOCK
        }
    }

internal fun Game.recordStats(transform: (GameStats) -> GameStats) {
        stats = transform(stats)
        scores.saveStats(stats)
    }
