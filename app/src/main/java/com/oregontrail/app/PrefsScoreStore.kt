package com.oregontrail.app

import android.content.Context
import com.oregontrail.engine.Data
import com.oregontrail.engine.GameStats
import com.oregontrail.engine.Grave
import com.oregontrail.engine.ScoreEntry
import com.oregontrail.engine.ScoreStore

/** Persists the Oregon Top Ten and the last gravestone in SharedPreferences. */
class PrefsScoreStore(context: Context) : ScoreStore {

    internal val prefs = context.getSharedPreferences("oregon_trail", Context.MODE_PRIVATE)

    override fun loadScores(): MutableList<ScoreEntry> {
        val raw = prefs.getString(KEY_SCORES, null) ?: return defaultScores()
        val entries = raw.split('\n').mapNotNull { line ->
            val parts = line.split('|')
            if (parts.size < 3) null
            else ScoreEntry(parts[0], parts[1].toIntOrNull() ?: 0, parts[2])
        }.toMutableList()
        return if (entries.isEmpty()) defaultScores() else entries
    }

    override fun saveScores(entries: List<ScoreEntry>) {
        val raw = entries.take(10).joinToString("\n") { "${it.name}|${it.points}|${it.occupation}" }
        prefs.edit().putString(KEY_SCORES, raw).apply()
    }

    override fun loadGravestone(): String? = prefs.getString(KEY_GRAVE, null)

    override fun saveGravestone(text: String) {
        prefs.edit().putString(KEY_GRAVE, text).apply()
    }

    override fun loadGraves(): List<Grave> {
        val raw = prefs.getString(KEY_GRAVES, null) ?: return emptyList()
        return raw.split('\n').mapNotNull { line ->
            val p = line.split('|')
            if (p.size < 4) null else Grave(p[0], p[1], p[2], p[3])
        }
    }

    override fun addGrave(grave: Grave) {
        val existing = loadGraves().toMutableList()
        existing.add(grave)
        while (existing.size > 50) existing.removeAt(0)
        val raw = existing.joinToString("\n") {
            listOf(it.name, it.cause, it.landmarkId, it.text).joinToString("|")
        }
        prefs.edit().putString(KEY_GRAVES, raw).apply()
    }

    fun saveState(state: String) {
        prefs.edit().putString(KEY_STATE, state).apply()
    }

    fun loadState(): String? = prefs.getString(KEY_STATE, null)

    fun clearState() {
        prefs.edit().remove(KEY_STATE).apply()
    }

    // ----- statistics & achievements ----------------------------------

    override fun loadStats(): GameStats = GameStats(
        gamesPlayed = prefs.getInt("st_games", 0),
        arrivals = prefs.getInt("st_arrivals", 0),
        deaths = prefs.getInt("st_deaths", 0),
        bestScore = prefs.getInt("st_best", 0),
        totalMiles = prefs.getInt("st_miles", 0),
        landmarksVisited = prefs.getInt("st_landmarks", 0),
        postcardsSent = prefs.getInt("st_postcards", 0)
    )

    override fun saveStats(stats: GameStats) {
        prefs.edit()
            .putInt("st_games", stats.gamesPlayed)
            .putInt("st_arrivals", stats.arrivals)
            .putInt("st_deaths", stats.deaths)
            .putInt("st_best", stats.bestScore)
            .putInt("st_miles", stats.totalMiles)
            .putInt("st_landmarks", stats.landmarksVisited)
            .putInt("st_postcards", stats.postcardsSent)
            .apply()
    }

    override fun loadAchievements(): Set<String> {
        val raw = prefs.getString("achievements", null) ?: return emptySet()
        return raw.split(',').filter { it.isNotEmpty() }.toSet()
    }

    override fun saveAchievements(ids: Set<String>) {
        prefs.edit().putString("achievements", ids.joinToString(",")).apply()
    }

    private fun defaultScores(): MutableList<ScoreEntry> =
        Data.topTenSeed.map { (name, points) -> ScoreEntry(name, points, "Pioneer") }.toMutableList()

    companion object {
        private const val KEY_SCORES = "top_ten"
        private const val KEY_GRAVE = "gravestone"
        private const val KEY_GRAVES = "graves"
        private const val KEY_STATE = "saved_game"
    }
}
