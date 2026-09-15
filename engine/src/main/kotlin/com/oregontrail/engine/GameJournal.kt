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

/** The whole journal as shareable text, oldest entry first. */
fun Game.journalText(): String {
    if (journal.isEmpty()) return "My Oregon Trail journal is empty."
    val leader = party.firstOrNull()?.name ?: "Traveler"
    val sb = StringBuilder("The Oregon Trail - $leader's journal\n\n")
    for (e in journal) sb.append("${e.date}: ${e.text}\n")
    return sb.toString()
}
