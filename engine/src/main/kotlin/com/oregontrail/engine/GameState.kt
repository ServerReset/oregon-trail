package com.oregontrail.engine

/**
 * The mutable state of one journey down the trail. Inherits the session state
 * from [GameSession]; [Game] itself only wires the two together.
 */
open class GameState : GameSession() {

    // ----- viewport -----------------------------------------------------
    var cols: Int = 40
    var rows: Int = 30

    // ----- run state ----------------------------------------------------
    var phase: Phase = Phase.TITLE
        internal set

    var occupation: Occupation = Occupation.BANKER
        internal set
    var travelMonth: TravelMonth = TravelMonth.MARCH
        internal set
    var party: MutableList<PartyMember> = ArrayList()
    val inventory = Inventory()
    var date: GameDate = GameDate()
    var weather: Weather = Weather(WeatherKind.CLEAR, 60)
    var miles: Int = 0
    var landmarkIndex: Int = 0
    var pace: Pace = Pace.STEADY
    var rations: Rations = Rations.FILLING

    /** Average oxen condition, 0..100; hard driving and bad weather wear it down. */
    var oxHealth: Int = 100
        internal set
    var difficulty: Difficulty = Difficulty.NORMAL
    var storeAtFort: Boolean = false
    var lastGravestone: String? = null
    var lastScore: Int = 0
    var topTen: MutableList<ScoreEntry> = ArrayList()
    val journal: MutableList<JournalEntry> = ArrayList()
    val graves: MutableList<Grave> = ArrayList()

    internal fun aliveMembers(): List<PartyMember> = party.filter { it.alive }
    internal val aliveCount: Int get() = aliveMembers().size
}
