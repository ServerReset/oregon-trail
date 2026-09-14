package com.oregontrail.app

import android.content.Context
import com.oregontrail.engine.Data
import com.oregontrail.engine.ScoreEntry
import com.oregontrail.engine.ScoreStore

/** Persists the Oregon Top Ten and the last gravestone in SharedPreferences. */
class PrefsScoreStore(context: Context) : ScoreStore {

    private val prefs = context.getSharedPreferences("oregon_trail", Context.MODE_PRIVATE)

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

    fun saveState(state: String) {
        prefs.edit().putString(KEY_STATE, state).apply()
    }

    fun loadState(): String? = prefs.getString(KEY_STATE, null)

    fun clearState() {
        prefs.edit().remove(KEY_STATE).apply()
    }

    private fun defaultScores(): MutableList<ScoreEntry> =
        Data.topTenSeed.map { (name, points) -> ScoreEntry(name, points, "Pioneer") }.toMutableList()

    companion object {
        private const val KEY_SCORES = "top_ten"
        private const val KEY_GRAVE = "gravestone"
        private const val KEY_STATE = "saved_game"
    }
}
