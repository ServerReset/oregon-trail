package com.oregontrail.engine

/** A single entry in the Oregon Top Ten. */
data class ScoreEntry(
    val name: String,
    val points: Int,
    val occupation: String
)

/** A grave left by a previous traveler who died on the trail. */
data class Grave(
    val name: String,
    val cause: String,
    val landmarkId: String,
    val text: String
)

/** Persistence boundary for high scores, gravestones and trail graves. */
interface ScoreStore {
    fun loadScores(): MutableList<ScoreEntry>
    fun saveScores(entries: List<ScoreEntry>)
    fun loadGravestone(): String?
    fun saveGravestone(text: String)

    /** Graves from previous journeys, shown when you reach the same place. */
    fun loadGraves(): List<Grave> = emptyList()

    fun addGrave(grave: Grave) {}
}

/** Default in-memory store, useful for tests and the pure-Kotlin engine. */
class InMemoryScoreStore : ScoreStore {
    private var scores: MutableList<ScoreEntry> =
        Data.topTenSeed.map { (name, points) -> ScoreEntry(name, points, "Pioneer") }.toMutableList()
    private var gravestone: String? = null
    private val graves = ArrayList<Grave>()

    override fun loadScores(): MutableList<ScoreEntry> = scores.toMutableList()

    override fun saveScores(entries: List<ScoreEntry>) {
        scores = entries.toMutableList()
    }

    override fun loadGravestone(): String? = gravestone

    override fun saveGravestone(text: String) {
        gravestone = text
    }

    override fun loadGraves(): List<Grave> = graves.toList()

    override fun addGrave(grave: Grave) {
        graves.add(grave)
        if (graves.size > 50) graves.removeAt(0)
    }
}
