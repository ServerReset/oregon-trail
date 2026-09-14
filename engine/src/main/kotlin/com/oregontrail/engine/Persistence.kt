package com.oregontrail.engine

/** A single entry in the Oregon Top Ten. */
data class ScoreEntry(
    val name: String,
    val points: Int,
    val occupation: String
)

/** Persistence boundary for high scores and the last gravestone. */
interface ScoreStore {
    fun loadScores(): MutableList<ScoreEntry>
    fun saveScores(entries: List<ScoreEntry>)
    fun loadGravestone(): String?
    fun saveGravestone(text: String)
}

/** Default in-memory store, useful for tests and the pure-Kotlin engine. */
class InMemoryScoreStore : ScoreStore {
    private var scores: MutableList<ScoreEntry> =
        Data.topTenSeed.map { (name, points) -> ScoreEntry(name, points, "Pioneer") }.toMutableList()
    private var gravestone: String? = null

    override fun loadScores(): MutableList<ScoreEntry> = scores.toMutableList()

    override fun saveScores(entries: List<ScoreEntry>) {
        scores = entries.toMutableList()
    }

    override fun loadGravestone(): String? = gravestone

    override fun saveGravestone(text: String) {
        gravestone = text
    }
}
