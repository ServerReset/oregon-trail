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

/** A named save state the player can return to. */
data class SaveSlot(
    val id: String,
    val label: String,
    val detail: String,
    val savedAt: Long,
    val data: String
)

/** Lifetime statistics across every run. */
data class GameStats(
    val gamesPlayed: Int = 0,
    val arrivals: Int = 0,
    val deaths: Int = 0,
    val bestScore: Int = 0,
    val totalMiles: Int = 0,
    val landmarksVisited: Int = 0,
    val postcardsSent: Int = 0
)

/** A milestone the player can unlock. */
data class Achievement(val id: String, val name: String, val description: String)

/** Persistence boundary for high scores, gravestones, graves, stats and achievements. */
interface ScoreStore {
    fun loadScores(): MutableList<ScoreEntry>
    fun saveScores(entries: List<ScoreEntry>)
    fun loadGravestone(): String?
    fun saveGravestone(text: String)

    /** Graves from previous journeys, shown when you reach the same place. */
    fun loadGraves(): List<Grave> = emptyList()

    fun addGrave(grave: Grave) {}

    fun loadStats(): GameStats = GameStats()

    fun saveStats(stats: GameStats) {}

    fun loadAchievements(): Set<String> = emptySet()

    fun saveAchievements(ids: Set<String>) {}
}

/** Default in-memory store, useful for tests and the pure-Kotlin engine. */
class InMemoryScoreStore : ScoreStore {
    private var scores: MutableList<ScoreEntry> =
        Data.topTenSeed.map { (name, points) -> ScoreEntry(name, points, "Pioneer") }.toMutableList()
    private var gravestone: String? = null
    private val graves = ArrayList<Grave>()
    private var stats = GameStats()
    private val achievements = LinkedHashSet<String>()

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

    override fun loadStats(): GameStats = stats

    override fun saveStats(stats: GameStats) {
        this.stats = stats
    }

    override fun loadAchievements(): Set<String> = achievements.toSet()

    override fun saveAchievements(ids: Set<String>) {
        achievements.clear()
        achievements.addAll(ids)
    }
}
