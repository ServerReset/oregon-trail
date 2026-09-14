package com.oregontrail.engine

import kotlin.math.max
import kotlin.math.min

/** All screens/phases of the game. */
enum class Phase {
    TITLE,
    ABOUT,
    MANAGEMENT,
    TOP_TEN,
    PROFESSION,
    MONTH,
    NAMES,
    STORE,
    TRAVEL,
    LANDMARK,
    RIVER,
    MAP,
    CHOICE,
    HUNTING,
    RAFTING,
    BARLOW,
    JOURNAL,
    LOAD,
    ACHIEVEMENTS,
    STATS,
    EPILOGUE,
    PAUSE,
    NOTICE,
    DEATH,
    ARRIVED
}

private enum class DayResult { OK, EVENT, LANDMARK, DEATH }

/**
 * The complete, platform-independent Oregon Trail game.
 *
 * It exposes exactly three things to a front-end:
 *  - [render] returns a [Screen] (a grid of styled characters + tap targets)
 *  - [onTap] feeds a tapped hotspot id back in
 *  - [huntTick] advances the hunting minigame
 *
 * Everything else (state, rules, content) is internal.
 */
class Game(
    private val rng: Rng = DefaultRng(),
    private val scores: ScoreStore = InMemoryScoreStore()
) {

    // ----- viewport -----------------------------------------------------
    var cols: Int = 40
    var rows: Int = 30

    fun setViewport(cols: Int, rows: Int) {
        val c = cols.coerceAtLeast(16)
        val r = rows.coerceAtLeast(10)
        val changed = c != this.cols || r != this.rows
        this.cols = c
        this.rows = r
        if (changed) resizeActiveFields()
    }

    /** Field dimensions for the hunting minigame at the current viewport. */
    private fun huntSize(): Pair<Int, Int> =
        min(cols, 64).coerceIn(10, 64) to min(rows - 8, 16).coerceIn(4, 16)

    /** Field dimensions for the rafting finale at the current viewport. */
    private fun raftSize(): Pair<Int, Int> =
        min(contentW, 40).coerceIn(10, 40) to min(rows - 5, 16).coerceIn(4, 16)

    /**
     * Keeps an in-progress minigame usable when the form factor changes
     * (folding, unfolding, split-screen, watch size). Progress is preserved.
     */
    private fun resizeActiveFields() {
        huntField?.let { old ->
            val (w, h) = huntSize()
            if (w != old.width || h != old.height) {
                huntField = HuntField(w, h, rng, huntPool(), old.carryLimit, old.meat, old.kills, old.shotsFired)
            }
        }
        raftField?.let { old ->
            val (w, h) = raftSize()
            if (w != old.width || h != old.height) {
                raftField = RaftField(w, h, rng, old.totalProgress, old.maxHits, old.progress, old.hits)
            }
        }
        barlowField?.let { old ->
            val (w, h) = barlowSize()
            if (w != old.width || h != old.height) {
                barlowField = BarlowField(w, h, rng, old.totalProgress, old.maxDamage, old.progress, old.damage)
            }
        }
    }

    // ----- persistent settings -----------------------------------------
    var soundEnabled: Boolean = true
    var pendingSound: Sound? = null

    // ----- run state ----------------------------------------------------
    var phase: Phase = Phase.TITLE
        private set

    var occupation: Occupation = Occupation.BANKER
        private set
    var travelMonth: TravelMonth = TravelMonth.MARCH
        private set
    var party: MutableList<PartyMember> = ArrayList()
    val inventory = Inventory()
    var date: GameDate = GameDate()
    var weather: Weather = Weather(WeatherKind.CLEAR, 60)
    var miles: Int = 0
    var landmarkIndex: Int = 0
    var pace: Pace = Pace.STEADY
    var rations: Rations = Rations.FILLING
    /** Average condition of the oxen, 0..100. Hard driving and bad weather wear it down. */
    var oxHealth: Int = 100
        private set
    var difficulty: Difficulty = Difficulty.NORMAL
    var storeAtFort: Boolean = false
    var lastGravestone: String? = null
    var lastScore: Int = 0
    var topTen: MutableList<ScoreEntry> = ArrayList()
    val journal: MutableList<JournalEntry> = ArrayList()
    val graves: MutableList<Grave> = ArrayList()

    // ----- meta progression --------------------------------------------
    val achievements: MutableSet<String> = LinkedHashSet()
    var stats: GameStats = GameStats()
    /** Save states supplied by the front-end for the load screen. */
    var saveSlots: List<SaveSlot> = emptyList()
    /** True when the front-end has an autosave to continue. */
    var autosaveAvailable: Boolean = false
    /** Seed for the Trail of the Day, supplied by the front-end. */
    var dailySeed: Long = 0L
    /** Set when the player asks to continue the autosave. */
    var requestedAutosaveLoad: Boolean = false
        private set
    /** Set when the player asks to save; the front-end shows a name dialog. */
    var requestedSave: Boolean = false
        private set
    /** Set when the player picks a slot to load or delete. */
    var requestedLoadId: String? = null
        private set
    var requestedDeleteId: String? = null
        private set
    /** Name of an achievement just unlocked, for the front-end to toast. */
    var pendingUnlock: String? = null
    /** Set when the player asks to share their journey summary. */
    var requestedShare: Boolean = false
        private set
    /** Set when the player quick-saves or quick-loads from the pause menu. */
    var requestedQuickSave: Boolean = false
        private set
    var requestedQuickLoad: Boolean = false
        private set
    /** Where the pause / management / load screens return to. */
    private var pauseReturn: Phase = Phase.TRAVEL
    private var managementReturn: Phase = Phase.TITLE
    private var loadReturn: Phase = Phase.TITLE
    /** How many landmark histories the player has read this run. */
    private var factsRead = 0
    /** Animation clock, advanced by the front-end on a timer. */
    var frame: Int = 0
        private set

    /** Advances the animation clock (called a few times a second). */
    fun animate() {
        frame = (frame + 1) % 100000
    }

    /** Presentation settings supplied by the front-end (may be null in tests). */
    var uiSettings: UiSettings? = null

    // ----- transient UI state ------------------------------------------
    internal var aboutPage = 0
    internal var journalPage = 0
    internal var noticeTitle = ""
    internal val noticeLines = ArrayList<String>()
    internal var noticeNext: Phase = Phase.TRAVEL
    internal var choiceTitle = ""
    internal val choiceLines = ArrayList<String>()
    internal val choiceOptions = ArrayList<Pair<String, String>>()
    private var choiceNext: Phase = Phase.TRAVEL

    internal var huntField: HuntField? = null
    private var huntReturn = Phase.TRAVEL
    private var huntDays = 1

    internal var raftField: RaftField? = null
    internal var barlowField: BarlowField? = null
    private var cutoffTarget: Int = -1

    internal var deathCause = ""

    var requestedNameEdit: Int? = null
        private set

    /** True when the front-end should prompt for a gravestone epitaph. */
    var requestedEpitaphEdit: Boolean = false
        private set

    init {
        topTen = scores.loadScores()
        lastGravestone = scores.loadGravestone()
        graves.addAll(scores.loadGraves())
        achievements.addAll(scores.loadAchievements())
        stats = scores.loadStats()
        newRun(Occupation.BANKER, TravelMonth.MARCH)
        phase = Phase.TITLE
    }

    // ====================================================================
    //  Run setup
    // ====================================================================

    private fun newRun(occ: Occupation, month: TravelMonth) {
        occupation = occ
        travelMonth = month
        inventory.cash = occ.startingMoney.toDouble()
        inventory.oxen = 0
        inventory.food = 0
        inventory.clothing = 0
        inventory.ammo = 0
        inventory.wheels = 0
        inventory.axles = 0
        inventory.tongues = 0
        date = GameDate(1848, month.monthIndex, 1)
        weather = Weather(WeatherKind.CLEAR, 60)
        miles = 0
        landmarkIndex = 0
        cutoffTarget = -1
        pace = Pace.STEADY
        rations = Rations.FILLING
        oxHealth = 100
        party = ArrayList()
        val surname = Data.surnames[rng.nextInt(Data.surnames.size)]
        repeat(5) { i ->
            val name = if (i == 0) "Pa $surname" else {
                Data.firstNames[rng.nextInt(Data.firstNames.size)]
            }
            party.add(PartyMember(name))
        }
    }

    internal fun aliveMembers(): List<PartyMember> = party.filter { it.alive }
    internal val aliveCount: Int get() = aliveMembers().size

    // ====================================================================
    //  Public input
    // ====================================================================

    fun onTap(id: String) {
        pendingSound = Sound.CLICK
        when {
            id == "title:travel" -> { phase = Phase.PROFESSION }
            id == "title:daily" -> startDailyChallenge()
            id == "title:about" -> { aboutPage = 0; phase = Phase.ABOUT }
            id == "title:topten" -> phase = Phase.TOP_TEN
            id == "title:continue" -> requestedAutosaveLoad = true
            id == "title:load" -> phase = Phase.LOAD
            id == "title:ach" -> phase = Phase.ACHIEVEMENTS
            id == "title:stats" -> phase = Phase.STATS
            id == "title:manage" -> { managementReturn = Phase.TITLE; phase = Phase.MANAGEMENT }
            id == "slots:back" -> { phase = loadReturn; loadReturn = Phase.TITLE }
            id == "ach:back" -> phase = Phase.TITLE
            id == "stats:back" -> phase = Phase.TITLE
            id.startsWith("slot:load:") -> requestedLoadId = id.substringAfter("slot:load:")
            id.startsWith("slot:del:") -> requestedDeleteId = id.substringAfter("slot:del:")
            id == "title:end" -> { /* handled by front-end by finishing activity */ }
            id == "about:next" -> {
                aboutPage++
                if (aboutPage >= ABOUT_PAGES.size) phase = Phase.TITLE
            }
            id == "manage:topten" -> phase = Phase.TOP_TEN
            id == "manage:newleader" -> { newRun(occupation, travelMonth); phase = Phase.PROFESSION }
            id == "manage:sound" -> soundEnabled = !soundEnabled
            id == "manage:difficulty" -> cycleDifficulty()
            id == "manage:textsize" -> uiSettings?.let { it.textScaleIndex = (it.textScaleIndex + 1) % 3 }
            id == "manage:contrast" -> uiSettings?.let { it.highContrast = !it.highContrast }
            id == "manage:scanlines" -> uiSettings?.let { it.scanlines = !it.scanlines }
            id == "manage:back" -> phase = managementReturn
            id == "pause:open" -> openPause()
            id == "pause:resume" -> phase = pauseReturn
            id == "pause:save" -> requestedSave = true
            id == "pause:quicksave" -> requestedQuickSave = true
            id == "pause:quickload" -> requestedQuickLoad = true
            id == "pause:load" -> { loadReturn = Phase.PAUSE; phase = Phase.LOAD }
            id == "pause:manage" -> { managementReturn = Phase.PAUSE; phase = Phase.MANAGEMENT }
            id == "pause:title" -> phase = Phase.TITLE
            id == "pause:quit" -> { /* handled by the front-end by finishing the activity */ }
            id == "topten:back" -> phase = Phase.TITLE
            id.startsWith("prof:") -> {
                occupation = Occupation.entries[id.substringAfter("prof:").toInt()]
                phase = Phase.MONTH
            }
            id.startsWith("month:") -> {
                travelMonth = TravelMonth.entries[id.substringAfter("month:").toInt()]
                date = GameDate(1848, travelMonth.monthIndex, 1)
                phase = Phase.NAMES
            }
            id.startsWith("name:") -> requestedNameEdit = id.substringAfter("name:").toInt()
            id == "names:go" -> {
                if (inventory.cash <= 0) { newRun(occupation, travelMonth); phase = Phase.PROFESSION }
                else { storeAtFort = false; phase = Phase.STORE }
            }
            id == "store:leave" -> leaveStore()
            id.startsWith("store:inc:") -> changeItem(id.substringAfterLast(":"), 1)
            id.startsWith("store:dec:") -> changeItem(id.substringAfterLast(":"), -1)
            id.startsWith("travel:") -> handleTravelMenu(id.substringAfter("travel:"))
            id.startsWith("land:") -> handleLandmarkMenu(id.substringAfter("land:"))
            id.startsWith("river:") -> handleRiver(id.substringAfter("river:"))
            id.startsWith("choice:") -> handleChoice(id.substringAfter("choice:"))
            id.startsWith("riders:") || id.startsWith("trade:") || id.startsWith("rest:") ->
                handleChoice(id)
            id.startsWith("stranded:") -> handleStranded(id.substringAfter("stranded:"))
            id == "notice:continue" -> phase = noticeNext
            id == "map:back" -> phase = Phase.TRAVEL
            id == "journal:prev" -> journalPage = (journalPage - 1).coerceAtLeast(0)
            id == "journal:next" -> journalPage = min(journalPage + 1, journalLastPage())
            id == "journal:back" -> phase = Phase.TRAVEL
            id == "hunt:up" -> huntField?.move(0, -1)
            id == "hunt:down" -> huntField?.move(0, 1)
            id == "hunt:left" -> huntField?.move(-1, 0)
            id == "hunt:right" -> huntField?.move(1, 0)
            id == "hunt:shoot" -> huntShoot()
            id == "hunt:leave" -> endHunt()
            id == "raft:left" -> raftField?.moveLeft()
            id == "raft:right" -> raftField?.moveRight()
            id == "barlow:left" -> barlowField?.moveLeft()
            id == "barlow:right" -> barlowField?.moveRight()
            id == "death:topten" -> phase = Phase.TOP_TEN
            id == "death:epitaph" -> requestedEpitaphEdit = true
            id == "death:restart" -> { newRun(occupation, travelMonth); phase = Phase.PROFESSION }
            id == "arrived:topten" -> phase = Phase.TOP_TEN
            id == "arrived:epilogue" -> phase = Phase.EPILOGUE
            id == "epilogue:back" -> phase = Phase.ARRIVED
            id == "arrived:share" || id == "death:share" -> requestedShare = true
            id == "arrived:restart" -> { newRun(occupation, travelMonth); phase = Phase.PROFESSION }
            id.startsWith("dalles:") -> handleDalles(id.substringAfter("dalles:"))
        }
    }

    fun setName(index: Int, name: String) {
        if (index in party.indices) {
            val trimmed = name.trim().take(12)
            if (trimmed.isNotEmpty()) party[index].name = trimmed
        }
        requestedNameEdit = null
    }

    /** Called by the front-end after a text dialog closes. */
    fun clearNameRequest() {
        requestedNameEdit = null
    }

    /** Sets the gravestone epitaph typed by the player and clears the request. */
    fun setEpitaph(text: String) {
        val trimmed = text.trim().take(140)
        if (trimmed.isNotEmpty()) {
            lastGravestone = trimmed
            scores.saveGravestone(trimmed)
            if (graves.isNotEmpty()) {
                graves[graves.size - 1] = graves.last().copy(text = trimmed)
            }
        }
        requestedEpitaphEdit = false
    }

    fun clearEpitaphRequest() {
        requestedEpitaphEdit = false
    }

    fun huntTick() {
        if (phase == Phase.HUNTING) huntField?.tick()
    }

    /** Advances the Columbia River rafting finale; called on a timer by the front-end. */
    fun raftTick() {
        if (phase != Phase.RAFTING) return
        val field = raftField ?: return
        field.tick()
        if (field.done) finishRaft()
    }

    /** Advances the Barlow Road climb; called on a timer by the front-end. */
    fun barlowTick() {
        if (phase != Phase.BARLOW) return
        val field = barlowField ?: return
        field.tick()
        if (field.done) finishBarlow()
    }

    // ====================================================================
    //  Store
    // ====================================================================

    private fun unitsPerTap(item: Item): Int = when (item) {
        Item.OXEN -> 2
        Item.FOOD -> 50
        Item.CLOTHING -> 1
        Item.AMMUNITION -> 20
        Item.WHEEL, Item.AXLE, Item.TONGUE -> 1
    }

    internal fun priceOf(item: Item): Double {
        val base = if (storeAtFort) item.fortPrice else item.basePrice
        // Bankers are shrewd traders and get a modest discount at forts.
        return if (storeAtFort && occupation == Occupation.BANKER) base * 0.9 else base
    }

    private fun tapCost(item: Item): Double = when (item) {
        Item.OXEN -> priceOf(item)                 // per yoke
        Item.FOOD -> priceOf(item) * 50
        Item.CLOTHING -> priceOf(item)
        Item.AMMUNITION -> priceOf(item)           // per box
        Item.WHEEL, Item.AXLE, Item.TONGUE -> priceOf(item)
    }

    internal fun displayQty(item: Item): Int = when (item) {
        Item.OXEN -> inventory.oxen / 2
        Item.FOOD -> inventory.food
        Item.CLOTHING -> inventory.clothing
        Item.AMMUNITION -> inventory.ammo / 20
        Item.WHEEL -> inventory.wheels
        Item.AXLE -> inventory.axles
        Item.TONGUE -> inventory.tongues
    }

    private fun changeItem(name: String, dir: Int) {
        val item = Item.entries.firstOrNull { it.name == name } ?: return
        if (dir > 0) {
            val cost = tapCost(item)
            if (inventory.cash + 0.001 < cost) { pendingSound = Sound.BAD; return }
            inventory.cash -= cost
            when (item) {
                Item.OXEN -> inventory.oxen += 2
                Item.FOOD -> inventory.food += 50
                Item.CLOTHING -> inventory.clothing += 1
                Item.AMMUNITION -> inventory.ammo += 20
                Item.WHEEL -> inventory.wheels += 1
                Item.AXLE -> inventory.axles += 1
                Item.TONGUE -> inventory.tongues += 1
            }
        } else {
            // Selling returns whole steps only, so stock can never go negative.
            val step = unitsPerTap(item)
            val have = when (item) {
                Item.OXEN -> inventory.oxen
                Item.FOOD -> inventory.food
                Item.CLOTHING -> inventory.clothing
                Item.AMMUNITION -> inventory.ammo
                Item.WHEEL -> inventory.wheels
                Item.AXLE -> inventory.axles
                Item.TONGUE -> inventory.tongues
            }
            if (have < step) return
            when (item) {
                Item.OXEN -> { inventory.oxen -= 2; inventory.cash += priceOf(item) }
                Item.FOOD -> { inventory.food -= 50; inventory.cash += priceOf(item) * 50 }
                Item.CLOTHING -> { inventory.clothing -= 1; inventory.cash += priceOf(item) }
                Item.AMMUNITION -> { inventory.ammo -= 20; inventory.cash += priceOf(item) }
                Item.WHEEL -> { inventory.wheels -= 1; inventory.cash += priceOf(item) }
                Item.AXLE -> { inventory.axles -= 1; inventory.cash += priceOf(item) }
                Item.TONGUE -> { inventory.tongues -= 1; inventory.cash += priceOf(item) }
            }
        }
    }

    private fun leaveStore() {
        if (inventory.oxen < 2) {
            showNotice(
                "The Store",
                listOf(
                    "You cannot cross the plains without oxen.",
                    "You need at least one yoke (two oxen)."
                ),
                Phase.STORE
            )
            pendingSound = Sound.BAD
            return
        }
        if (!storeAtFort) {
            // Leaving Independence: begin the journey.
            phase = Phase.TRAVEL
            recordStats { it.copy(gamesPlayed = it.gamesPlayed + 1) }
            addJournal("We bought our supplies and left Independence for Oregon.")
            showNotice(
                "Heading Out",
                listOf(
                    "You load the wagon and roll out of",
                    "Independence on ${date}.",
                    "",
                    "The Oregon Trail stretches 2,040 miles",
                    "to the west. Good luck."
                ),
                Phase.TRAVEL
            )
        } else {
            phase = Phase.LANDMARK
        }
    }

    // ====================================================================
    //  Travel menu
    // ====================================================================

    private fun handleTravelMenu(action: String) {
        when (action) {
            "continue" -> continueOnTrail()
            "supplies" -> showNotice("Your Supplies", suppliesLines(), Phase.TRAVEL)
            "map" -> phase = Phase.MAP
            "journal" -> { journalPage = 0; phase = Phase.JOURNAL }
            "save" -> requestedSave = true
            "pace" -> cyclePace()
            "rations" -> cycleRations()
            "rest" -> startRest()
            "trade" -> attemptTrade()
            "hunt" -> startHunt(Phase.TRAVEL)
        }
    }

    private fun cyclePace() {        pace = when (pace) {
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

    private fun cycleRations() {
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

    private fun cycleDifficulty() {
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
    fun oxCondition(): String = when {
        oxHealth >= 75 -> "good"
        oxHealth >= 45 -> "fair"
        oxHealth >= 20 -> "poor"
        else -> "failing"
    }

    /** Records a dated line in the traveler's journal. */
    /** Starts a run seeded by today's date: the same trail for everyone. */
    private fun startDailyChallenge() {
        if (dailySeed == 0L) return
        rng.reseed(dailySeed)
        newRun(Occupation.BANKER, TravelMonth.MARCH)
        addJournal("Trail of the Day: a fresh start on a shared trail.")
        pendingSound = Sound.GOOD
        phase = Phase.PROFESSION
    }

    /** Test hook: fires a named event and returns its messages. */
    internal fun debugFireEvent(id: String): List<String> {
        val msgs = ArrayList<String>()
        applyEvent(id, msgs)
        return msgs
    }

    /** Test hook: the list of event ids the game can produce. */
    internal fun debugEventIds(): List<String> =
        (eventPool(0) + eventPool(1500)).toSet().toList()

    private fun addJournal(text: String) {
        val dateText = "${date.monthName} ${date.day}"
        journal.add(JournalEntry(dateText, text))
        if (journal.size > JOURNAL_LIMIT) journal.removeAt(0)
    }

    /** Unlocks an achievement once, saving it and queuing a notice for the front-end. */
    private fun unlock(id: String) {
        if (achievements.add(id)) {
            scores.saveAchievements(achievements)
            pendingUnlock = Achievements.name(id)
            addJournal("Achievement: ${Achievements.name(id)}")
            pendingSound = Sound.GOOD
        }
    }

    fun clearLoadRequest() {
        requestedLoadId = null
    }

    fun clearDeleteRequest() {
        requestedDeleteId = null
    }

    fun clearSaveRequest() {
        requestedSave = false
    }

    /** Called by the front-end once it has loaded (or failed to load) the autosave. */
    fun clearAutosaveRequest() {
        requestedAutosaveLoad = false
    }

    fun clearShareRequest() {
        requestedShare = false
    }

    fun clearQuickSaveRequest() {
        requestedQuickSave = false
    }

    fun clearQuickLoadRequest() {
        requestedQuickLoad = false
    }

    /** True when the Back button should open the pause menu. */
    fun canPause(): Boolean = phase in PAUSABLE_PHASES

    private fun openPause() {
        if (canPause()) {
            pauseReturn = phase
            phase = Phase.PAUSE
        }
    }

    /** A short, shareable summary of the journey. */
    fun summaryText(): String {
        val leader = party.firstOrNull()?.name ?: "Traveler"
        val sb = StringBuilder()
        sb.append("The Oregon Trail - $leader\n")
        sb.append("${occupation.displayName}, departed ${travelMonth.displayName} 1848\n")
        sb.append("${date}: $miles of ${Data.TOTAL_MILES} miles\n")
        if (phase == Phase.DEATH) {
            sb.append("Died of $deathCause on the trail.\n")
        } else {
            sb.append("Final score: $lastScore\n")
        }
        sb.append("Party:\n")
        for (m in party) {
            val fate = if (m.alive) "reached Oregon" else "died of ${m.condition ?: "the trail"}"
            sb.append("  ${m.name}: $fate\n")
        }
        return sb.toString()
    }

    private fun recordStats(transform: (GameStats) -> GameStats) {
        stats = transform(stats)
        scores.saveStats(stats)
    }

    /** The original let you choose how long to rest. */
    private fun startRest() {
        choiceTitle = "Rest"
        choiceLines.clear()
        choiceLines.add("How long should the party rest?")
        choiceLines.add("Longer rest heals more, but eats food and costs time.")
        choiceOptions.clear()
        choiceOptions.add("1 day" to "rest:1")
        choiceOptions.add("2 days" to "rest:2")
        choiceOptions.add("3 days" to "rest:3")
        choiceOptions.add("5 days" to "rest:5")
        choiceNext = Phase.TRAVEL
        phase = Phase.CHOICE
    }

    private fun rest(days: Int) {
        var report = "You camp and rest for $days day(s)."
        repeat(days) {
            date.plusDays(1)
            rollWeather()
            consumeFood()
        }
        aliveMembers().forEach { it.heal(days * 4) }
        oxHealth = (oxHealth + days * 5).coerceAtMost(100)
        val healed = aliveMembers().joinToString(", ") { "${it.name} (${it.state.displayName})" }
        report += "\n\nRest helps. Your party's health: $healed."
        addJournal("Rested for $days day(s) to recover.")
        showNotice("Resting", listOf(report), Phase.TRAVEL)
    }

    private fun attemptTrade() {
        // A trader, mountain man or soldier offers a deal.
        val offers = listOf(
            Triple("80 pounds of food", "1 set of clothing", "food80"),
            Triple("1 spare wheel", "1 set of clothing", "wheel"),
            Triple("40 bullets", "30 pounds of food", "ammo"),
            Triple("1 yoke of oxen", "50 pounds of food", "oxen")
        )
        val offer = offers[rng.nextInt(offers.size)]
        val who = listOf("A trader", "A mountain man", "A soldier from the fort")[rng.nextInt(3)]
        choiceTitle = "Trading"
        choiceLines.clear()
        choiceLines.add("$who camped nearby offers you")
        choiceLines.add("${offer.first} in exchange for ${offer.second}.")
        choiceLines.add("")
        choiceLines.add("What do you do?")
        choiceOptions.clear()
        choiceOptions.add("Accept the trade" to "trade:yes:${offer.third}")
        choiceOptions.add("Haggle for more" to "trade:haggle:${offer.third}")
        choiceOptions.add("Decline" to "trade:no")
        choiceNext = Phase.TRAVEL
        phase = Phase.CHOICE
    }

    // ====================================================================
    //  Core travel loop
    // ====================================================================

    private fun continueOnTrail() {
        if (aliveCount == 0) { dieOf("the trail"); return }
        val msgs = ArrayList<String>()
        var result = DayResult.OK
        var guard = 0
        while (guard++ < 90) {
            result = simulateDay(msgs)
            if (result != DayResult.OK) break
        }
        when (result) {
            DayResult.LANDMARK -> advanceToLandmark(msgs)
            DayResult.DEATH -> dieOf(deathCause.ifEmpty { "disease" })
            else -> {
                if (msgs.isEmpty()) msgs.add("The trail is long and quiet.")
                showNotice("On the Trail", msgs, Phase.TRAVEL)
            }
        }
    }

    internal fun nextLandmark(): Landmark? {
        if (cutoffTarget > landmarkIndex) return Data.landmarkAt(cutoffTarget)
        val idx = landmarkIndex + 1
        return if (idx <= Data.landmarks.lastIndex) Data.landmarkAt(idx) else null
    }

    private fun simulateDay(msgs: MutableList<String>): DayResult {
        if (aliveCount == 0) { deathCause = "disease"; return DayResult.DEATH }
        date.plusDays(1)
        rollWeather()

        // Eat.
        if (!consumeFood()) {
            msgs.add("There is no food left. The party is weak and hungry.")
            aliveMembers().forEach { it.hurt(rng.nextInt(4, 10)) }
            if (aliveCount == 0) { deathCause = "starvation"; return DayResult.DEATH }
        }

        // Make miles.
        val weatherFactor = weatherSpeedFactor(weather.kind)
        val oxenYokes = inventory.oxen / 2
        val oxenFactor = if (oxenYokes <= 0) 0.0 else min(1.2, 0.6 + 0.2 * oxenYokes)

        // Hard driving and harsh weather wear the oxen down (a cut feature the
        // original designer wished he could have tracked).
        when (pace) {
            Pace.STRENUOUS -> oxHealth -= rng.nextInt(0, 2)
            Pace.GRUELING -> oxHealth -= rng.nextInt(1, 3)
            else -> {}
        }
        if (weather.tempF >= 90 || weather.tempF <= 25) oxHealth -= 1
        if (inventory.food <= 0) oxHealth -= 1
        oxHealth = oxHealth.coerceIn(0, 100)
        val oxHealthFactor = 0.55 + 0.45 * oxHealth / 100.0

        val terrain = terrainFactor(Data.landmarkAt(landmarkIndex).kind)
        var gained = (pace.milesPerDay * weatherFactor * oxenFactor * terrain * oxHealthFactor).toInt()
        if (inventory.oxen <= 0) gained = 0
        if (oxHealth <= 15 && inventory.oxen >= 2 && rng.chance(0.12)) {
            inventory.oxen -= 2
            oxHealth = 45
            msgs.add("An exhausted ox collapses. You are down a yoke.")
        }
        miles += max(0, gained)

        // Ill health from hard travel.
        if (pace == Pace.GRUELING) aliveMembers().forEach { it.hurt(rng.nextInt(0, 3)) }

        // Illness.
        if (checkIllness(msgs)) { clampMiles(); return stopOrDeath() }

        // Random event.
        if (rng.chance(difficulty.eventChance)) {
            val before = msgs.size
            rollEvent(msgs)
            msgs.getOrNull(before)?.let { addJournal(it) }
            clampMiles()
            return stopOrDeath()
        }

        clampMiles()

        // Reached next landmark?
        val next = nextLandmark()
        if (next != null && miles >= next.mile) {
            // Do not overshoot badly.
            miles = next.mile
            return DayResult.LANDMARK
        }

        return DayResult.OK
    }

    /** Miles traveled can never fall behind the last landmark reached. */
    private fun clampMiles() {
        val floor = Data.landmarkAt(landmarkIndex).mile
        if (miles < floor) miles = floor
        if (miles < 0) miles = 0
    }

    private fun stopOrDeath(): DayResult {
        return if (aliveCount == 0) {
            if (deathCause.isEmpty()) deathCause = "disease"
            DayResult.DEATH
        } else DayResult.EVENT
    }

    private fun consumeFood(): Boolean {
        val needed = rations.poundsPerPersonPerDay * aliveCount
        if (inventory.food >= needed) {
            inventory.food -= needed
            return true
        }
        inventory.food = 0
        return false
    }

    private fun rollWeather() {
        val mountain = Data.landmarkAt(landmarkIndex).kind == LandmarkKind.MOUNTAINS || miles > 1150
        val base = when (date.month) {
            3 -> 52; 4 -> 60; 5 -> 68; 6 -> 76; 7 -> 82; 8 -> 82
            9 -> 72; 10 -> 58; 11 -> 44; else -> 34
        }
        var temp = base + rng.nextInt(-12, 13)
        if (mountain) temp -= 18
        val kind = when {
            temp <= 20 && mountain -> if (rng.chance(0.5)) WeatherKind.BLIZZARD else WeatherKind.SNOW
            temp <= 30 -> if (rng.chance(0.5)) WeatherKind.SNOW else WeatherKind.COLD
            temp >= 90 -> WeatherKind.HOT
            else -> when (rng.nextInt(10)) {
                0, 1 -> WeatherKind.CLEAR
                2, 3 -> WeatherKind.CLOUDY
                4 -> WeatherKind.RAIN
                5 -> WeatherKind.HEAVY_RAIN
                6 -> WeatherKind.THUNDERSTORM
                7 -> WeatherKind.WINDY
                8 -> WeatherKind.HAIL
                else -> WeatherKind.FOG
            }
        }
        weather = Weather(kind, temp)
    }

    private fun weatherSpeedFactor(kind: WeatherKind): Double = when (kind) {
        WeatherKind.CLEAR -> 1.1
        WeatherKind.CLOUDY -> 1.0
        WeatherKind.RAIN -> 0.8
        WeatherKind.HEAVY_RAIN -> 0.6
        WeatherKind.THUNDERSTORM -> 0.5
        WeatherKind.SNOW -> 0.6
        WeatherKind.BLIZZARD -> 0.2
        WeatherKind.FOG -> 0.6
        WeatherKind.HOT -> 0.9
        WeatherKind.COLD -> 0.8
        WeatherKind.WINDY -> 0.9
        WeatherKind.HAIL -> 0.5
    }

    private fun terrainFactor(kind: LandmarkKind): Double = when (kind) {
        LandmarkKind.MOUNTAINS -> 0.75
        LandmarkKind.RIVER -> 1.0
        else -> 1.0
    }

    private fun checkIllness(msgs: MutableList<String>): Boolean {
        var any = false
        for (m in aliveMembers()) {
            var chance = 0.012
            if (rations != Rations.FILLING) chance += 0.012
            if (rations == Rations.BARE_BONES) chance += 0.02
            if (pace == Pace.GRUELING) chance += 0.01
            if (inventory.food <= 20) chance += 0.02
            if (weather.tempF < 40 && inventory.clothing < aliveCount) chance += 0.02
            if (m.health < 50) chance += 0.015
            chance *= difficulty.illnessScale
            if (!rng.chance(chance)) continue
            any = true
            val illness = Data.illnesses[rng.nextInt(Data.illnesses.size)]
            val severity = rng.nextInt(8, 22)
            m.hurt(severity)
            m.condition = illness
            if (!m.alive) {
                msgs.add("${m.name} has died of $illness.")
                addJournal("${m.name} died of $illness.")
                deathCause = illness
            } else {
                msgs.add("${m.name} has come down with $illness.")
                addJournal("${m.name} came down with $illness.")
            }
        }
        return any
    }

    // ====================================================================
    //  Events
    // ====================================================================

    private fun rollEvent(msgs: MutableList<String>) {
        val pool = eventPool(miles)
        val event = pool[rng.nextInt(pool.size)]
        applyEvent(event, msgs)
    }

    private fun eventPool(m: Int): List<String> {
        val plains = listOf(
            "breakdown", "breakdown", "ox_lame", "ox_wander", "child_lost",
            "child_arm", "unsafe_water", "heavy_rain", "hail", "bandits",
            "wild_animals", "fire", "fog", "indians", "thief", "fruit", "riders",
            "stranded"
        )
        val mountains = listOf(
            "breakdown", "ox_lame", "ox_wander", "unsafe_water", "heavy_rain",
            "hail", "bandits", "wild_animals", "fire", "fog", "snakebite",
            "cold", "blizzard", "indians", "riders", "stranded"
        )
        return if (m > 900) mountains else plains
    }

    private fun applyEvent(event: String, msgs: MutableList<String>) {
        val lm = Data.landmarkAt(landmarkIndex)
        when (event) {
            "breakdown" -> {
                val part = Part.entries[rng.nextInt(Part.entries.size)]
                if (inventory.useSpare(part)) {
                    msgs.add("A wagon ${part.name.lowercase()} breaks, but you")
                    msgs.add("have a spare and replace it on the spot.")
                } else if (occupation == Occupation.CARPENTER && rng.chance(0.5)) {
                    msgs.add("A wagon ${part.name.lowercase()} breaks, but your")
                    msgs.add("carpentry skills repair it without a spare part.")
                    unlock(Achievements.CARPENTER)
                } else {
                    val delay = rng.nextInt(10, 20)
                    inventory.food = max(0, inventory.food - 8)
                    miles -= delay
                    msgs.add("A wagon ${part.name.lowercase()} breaks down.")
                    msgs.add("You lose $delay miles making repairs.")
                }
            }
            "ox_lame" -> {
                val lost = rng.nextInt(15, 26)
                miles -= lost
                oxHealth = (oxHealth - rng.nextInt(10, 25)).coerceIn(0, 100)
                msgs.add("An ox goes lame. You slow down and")
                msgs.add("lose $lost miles resting the animal.")
                if (oxHealth <= 0 && inventory.oxen >= 2) {
                    inventory.oxen -= 2
                    oxHealth = 45
                    msgs.add("The lame ox has to be cut from the team.")
                }
            }
            "ox_wander" -> {
                val lost = rng.nextInt(10, 18)
                miles -= lost
                msgs.add("An ox wanders off in the night.")
                msgs.add("You lose $lost miles searching for it.")
            }
            "child_lost" -> {
                val lost = rng.nextInt(5, 11)
                miles -= lost
                msgs.add("A child gets lost in the tall grass.")
                msgs.add("You spend half a day searching.")
            }
            "child_arm" -> {
                val lost = rng.nextInt(4, 9)
                inventory.clothing = max(0, inventory.clothing - 1)
                miles -= lost
                msgs.add("A child breaks an arm and needs a sling.")
                msgs.add("You stop and tend the injury.")
            }
            "unsafe_water" -> {
                val lost = rng.nextInt(2, 12)
                miles -= lost
                msgs.add("The water is unsafe. You lose time")
                msgs.add("looking for a clean spring.")
            }
            "heavy_rain" -> {
                inventory.food = max(0, inventory.food - 10)
                inventory.ammo = max(0, inventory.ammo - 20)
                val lost = rng.nextInt(3, 12)
                miles -= lost
                msgs.add("Heavy rains soak the wagon. Food and")
                msgs.add("ammunition are damaged.")
            }
            "hail" -> {
                inventory.ammo = max(0, inventory.ammo - 10)
                inventory.food = max(0, inventory.food - 4)
                val lost = rng.nextInt(4, 14)
                miles -= lost
                msgs.add("A hailstorm batters the wagon and")
                msgs.add("damages your supplies.")
            }
            "bandits" -> {
                if (inventory.ammo >= 40) {
                    inventory.ammo -= 20
                    msgs.add("Bandits attack! You fire back and")
                    msgs.add("drive them off, spending 20 bullets.")
                } else {
                    val stolen = rng.nextInt(10, 60).toDouble().coerceAtMost(inventory.cash)
                    inventory.cash -= stolen
                    msgs.add("Bandits attack and you are low on")
                    msgs.add("bullets. They steal $${"%.0f".format(stolen)}.")
                }
            }
            "wild_animals" -> {
                if (inventory.ammo >= 20) {
                    inventory.ammo -= 20
                    inventory.food += 40
                    msgs.add("Wild animals attack the oxen. You")
                    msgs.add("shoot one and gain 40 pounds of meat.")
                } else {
                    inventory.food = max(0, inventory.food - 20)
                    inventory.clothing = max(0, inventory.clothing - 1)
                    msgs.add("Wild animals raid the camp. They")
                    msgs.add("carry off food and clothing.")
                }
            }
            "fire" -> {
                inventory.food = max(0, inventory.food - 40)
                inventory.ammo = max(0, inventory.ammo - 20)
                msgs.add("A fire breaks out in the wagon.")
                msgs.add("Food and ammunition are damaged.")
            }
            "fog" -> {
                val lost = rng.nextInt(8, 16)
                miles -= lost
                msgs.add("Heavy fog rolls in and you lose the")
                msgs.add("trail for $lost miles.")
            }
            "snakebite" -> {
                val victim = aliveMembers().randomOrNull(rng) ?: return
                victim.hurt(30)
                victim.condition = "a snakebite"
                msgs.add("${victim.name} is bitten by a poisonous")
                if (victim.alive) msgs.add("snake and grows very ill.") else {
                    msgs.add("snake and does not survive.")
                    deathCause = "a snakebite"
                }
            }
            "cold" -> {
                if (inventory.clothing >= aliveCount * 2) {
                    msgs.add("Cold weather bites, but your warm")
                    msgs.add("clothing keeps the party comfortable.")
                } else {
                    msgs.add("Cold weather! You do not have enough")
                    msgs.add("clothing to keep everyone warm.")
                    aliveMembers().forEach { it.hurt(rng.nextInt(4, 10)) }
                }
            }
            "blizzard" -> {
                inventory.food = max(0, inventory.food - 25)
                val lost = rng.nextInt(30, 70)
                miles -= lost
                msgs.add("A blizzard strikes the mountain pass!")
                msgs.add("You lose $lost miles and much food.")
                if (inventory.clothing < aliveCount * 2) {
                    aliveMembers().forEach { it.hurt(rng.nextInt(6, 14)) }
                }
            }
            "indians" -> {
                inventory.food += 14
                msgs.add("Helpful Shoshone show you where to")
                msgs.add("find camas roots. You gain 14 pounds.")
            }
            "thief" -> {
                val stolenFood = min(inventory.food, rng.nextInt(20, 60))
                val stolenClothes = min(inventory.clothing, rng.nextInt(0, 2))
                inventory.food -= stolenFood
                inventory.clothing -= stolenClothes
                msgs.add("A thief creeps into camp at night.")
                msgs.add("You lose $stolenFood pounds of food.")
            }
            "fruit" -> {
                val gained = rng.nextInt(10, 31)
                inventory.food += gained
                msgs.add("You find bushes heavy with wild fruit")
                msgs.add("and gather $gained pounds of food.")
            }
            "stranded" -> startStrandedChoice()
            "riders" -> startRidersChoice()
        }
    }

    /** A moral encounter: help a family in need, at a cost. */
    private fun startStrandedChoice() {
        choiceTitle = "Stranded Family"
        choiceLines.clear()
        choiceLines.add("A family's wagon has thrown a wheel and")
        choiceLines.add("they are nearly out of food. They ask you")
        choiceLines.add("for help. What do you do?")
        choiceOptions.clear()
        choiceOptions.add("Share 50 lb of food" to "stranded:food")
        choiceOptions.add("Give them a spare wheel" to "stranded:wheel")
        choiceOptions.add("Wish them luck" to "stranded:no")
        choiceNext = Phase.TRAVEL
        phase = Phase.CHOICE
    }

    private fun handleStranded(action: String) {
        val msgs = ArrayList<String>()
        when (action) {
            "food" -> if (inventory.food >= 50) {
                inventory.food -= 50
                msgs.add("You share 50 pounds of food with the")
                msgs.add("stranded family. They thank you warmly.")
                unlock(Achievements.GOOD_SAMARITAN)
            } else {
                msgs.add("You have no food to spare and roll on,")
                msgs.add("heavy of heart.")
            }
            "wheel" -> if (inventory.wheels > 0) {
                inventory.wheels -= 1
                msgs.add("You give them a spare wheel. They vow")
                msgs.add("to repay the kindness someday.")
                unlock(Achievements.GOOD_SAMARITAN)
            } else {
                msgs.add("You have no spare wheel to give.")
            }
            else -> msgs.add("You wish them luck and move on. The trail is hard.")
        }
        addJournal("Stranded family on the trail: chose '$action'.")
        showNotice("Stranded Family", msgs, Phase.TRAVEL)
    }

    private fun startRidersChoice() {        choiceTitle = "Riders Ahead"
        choiceLines.clear()
        choiceLines.add("Riders appear on the horizon.")
        choiceLines.add(if (rng.chance(0.6)) "They look hostile." else "They look friendly.")
        choiceLines.add("")
        choiceLines.add("What do you do?")
        choiceOptions.clear()
        choiceOptions.add("1. Run" to "riders:run")
        choiceOptions.add("2. Attack" to "riders:attack")
        choiceOptions.add("3. Continue" to "riders:continue")
        choiceOptions.add("4. Circle the wagons" to "riders:circle")
        choiceNext = Phase.TRAVEL
        phase = Phase.CHOICE
    }

    private fun handleRiders(action: String) {
        val hostile = rng.chance(0.6)
        val msgs = ArrayList<String>()
        if (action == "attack") {
            if (inventory.ammo >= 40) {
                inventory.ammo -= 40
                msgs.add("You attack. A sharp fight follows, but")
                msgs.add("the riders withdraw. You spent 40 bullets.")
            } else {
                inventory.ammo = 0
                val stolen = (inventory.cash / 3).coerceAtLeast(0.0)
                inventory.cash -= stolen
                msgs.add("You ran low on bullets. The riders")
                msgs.add("took what they wanted and left.")
            }
        } else if (action == "run") {
            inventory.oxen = max(0, inventory.oxen - 2)
            miles += 15
            msgs.add("You run for it, gaining time but")
            msgs.add("wearing out an ox. The riders give up.")
        } else if (action == "circle") {
            miles -= 20
            if (hostile) {
                msgs.add("You circle the wagons. The riders")
                msgs.add("circle twice and ride off.")
            } else {
                msgs.add("You circle the wagons. The friendly")
                msgs.add("riders trade news and move on.")
            }
        } else {
            if (hostile && inventory.ammo < 20) {
                inventory.food = max(0, inventory.food - 30)
                msgs.add("The hostile riders raid your stores")
                msgs.add("and take 30 pounds of food.")
            } else {
                msgs.add("You keep moving. The riders pass by")
                msgs.add("without incident.")
            }
        }
        addJournal("Riders on the trail; we chose to $action.")
        showNotice(choiceTitle, msgs, Phase.TRAVEL)
    }

    // ====================================================================
    //  Landmarks
    // ====================================================================

    private fun advanceToLandmark(msgs: MutableList<String>) {
        val idx = if (cutoffTarget > landmarkIndex) cutoffTarget else landmarkIndex + 1
        landmarkIndex = min(idx, Data.landmarks.lastIndex)
        cutoffTarget = -1
        val lm = Data.landmarkAt(landmarkIndex)
        if (lm.kind == LandmarkKind.END) { arriveOregon(); return }
        val lines = ArrayList<String>()
        lines.add("You have reached ${lm.name}.")
        lines.add("")
        lines.addAll(lm.blurb)
        val here = graves.filter { it.landmarkId == lm.id }
        if (here.isNotEmpty()) {
            lines.add("")
            lines.add("You pass the graves of earlier travelers:")
            here.take(3).forEach { lines.add(it.text) }
        }
        msgs.clear()
        lines.forEach { msgs.add(it) }
        addJournal("Reached ${lm.name}.")
        val next = if (lm.kind == LandmarkKind.RIVER) Phase.RIVER else Phase.LANDMARK
        showNotice("Landmark", lines, next)
        pendingSound = Sound.GOOD
    }

    private fun handleLandmarkMenu(action: String) {
        when (action) {
            "continue" -> phase = Phase.TRAVEL
            "supplies" -> showNotice("Your Supplies", suppliesLines(), Phase.LANDMARK)
            "map" -> phase = Phase.MAP
            "rest" -> startRest()
            "buy" -> { storeAtFort = true; phase = Phase.STORE }
            "talk" -> talkToPeople()
            "fact" -> showHistory()
            "hunt" -> startHunt(Phase.LANDMARK)
            "cutoff" -> takeCutoff()
        }
    }

    /** Shows a historical note about the current landmark (the educational bit). */
    private fun showHistory() {
        val lm = Data.landmarkAt(landmarkIndex)
        val fact = Facts.forLandmark(lm.id)
            ?: "This stretch of the trail is remembered by the families who crossed it."
        factsRead++
        if (factsRead >= 5) unlock(Achievements.HISTORIAN)
        addJournal("Read about ${lm.name}.")
        showNotice("History of ${shortLandmarkTitle(lm)}", listOf(fact), Phase.LANDMARK)
    }

    private fun shortLandmarkTitle(lm: Landmark): String =
        if (lm.name.length <= cols - 12) lm.name else lm.name.substringBefore(",")

    /** Approximate river depth in feet, deepened by rain. */
    fun riverDepth(river: River): Double {
        val base = river.widthYards / 130.0 + 1.5
        val rain = when (weather.kind) {
            WeatherKind.HEAVY_RAIN, WeatherKind.THUNDERSTORM -> 2.0
            WeatherKind.RAIN, WeatherKind.HAIL -> 1.0
            else -> 0.0
        }
        return (base + rain)
    }

    private fun takeCutoff() {
        val lm = Data.landmarkAt(landmarkIndex)
        val targetId = lm.cutoffId ?: return
        val targetIdx = Data.indexOf(targetId)
        if (targetIdx <= landmarkIndex + 1) return
        cutoffTarget = targetIdx
        val targetMile = Data.landmarkAt(targetIdx).mile
        miles = min(targetMile - 1, miles + lm.cutoffMiles)
        pendingSound = Sound.GOOD
        addJournal("Took the ${lm.cutoffLabel?.removePrefix("Take the ")} to save ${lm.cutoffMiles} miles.")
        showNotice(
            "Taking the Cutoff",
            listOf(
                lm.cutoffLabel ?: "You take a cutoff.",
                "You leave the main trail and save ${lm.cutoffMiles} miles,",
                "but you will miss the next fort and its supplies."
            ),
            Phase.TRAVEL
        )
    }

    private fun talkToPeople() {
        val lm = Data.landmarkAt(landmarkIndex)
        val lines = ArrayList<String>()
        lines.add(Talk.lines[rng.nextInt(Talk.lines.size)])
        // Sometimes you also pick up a useful rumor.
        if (rng.chance(0.4)) {
            lines.add("")
            lines.add(Talk.rumors[rng.nextInt(Talk.rumors.size)])
        }
        showNotice(lm.name, lines, Phase.LANDMARK)
    }

    // ====================================================================
    //  Rivers
    // ====================================================================

    private fun handleRiver(action: String) {
        val lm = Data.landmarkAt(landmarkIndex)
        val river = lm.river ?: return
        val msgs = ArrayList<String>()
        when (action) {
            "ford" -> {
                unlock(Achievements.RIVERBANK)
                val risk = 0.25 + if (inventory.oxen < 4) 0.2 else 0.0 + weatherRisk()
                if (rng.chance(1 - risk)) {
                    msgs.add("You ford the ${lm.name.split(" ").first()} safely.")
                    pendingSound = Sound.GOOD
                } else {
                    supplyLoss(msgs, 30, 1)
                    msgs.add("The wagon is nearly swept away!")
                    msgs.add("You lose supplies but make the far bank.")
                    pendingSound = Sound.BAD
                }
            }
            "caulk" -> {
                unlock(Achievements.RIVERBANK)
                val days = rng.nextInt(1, 3)
                date.plusDays(days)
                val risk = 0.15 + if (inventory.oxen < 4) 0.15 else 0.0
                if (rng.chance(1 - risk)) {
                    msgs.add("You caulk the wagon and float across.")
                    msgs.add("It takes $days day(s) but works perfectly.")
                    pendingSound = Sound.GOOD
                } else {
                    supplyLoss(msgs, 40, 1)
                    msgs.add("The wagon tips and water pours in!")
                    msgs.add("You lose supplies but reach the far side.")
                    pendingSound = Sound.BAD
                }
            }
            "ferry" -> {
                val cost = river.ferryCost ?: return
                if (inventory.cash < cost) {
                    showNotice("River Crossing", listOf("You cannot afford the ferry."), Phase.RIVER)
                    pendingSound = Sound.BAD
                    return
                }
                inventory.cash -= cost
                date.plusDays(1)
                unlock(Achievements.FERRYMAN)
                msgs.add("You pay $${"%.2f".format(cost)} for the ferry and")
                msgs.add("cross the river without trouble.")
            }
            "guide" -> {
                val cost = river.guideCost ?: return
                if (inventory.cash < cost) {
                    showNotice("River Crossing", listOf("You cannot afford a guide."), Phase.RIVER)
                    pendingSound = Sound.BAD
                    return
                }
                inventory.cash -= cost
                date.plusDays(1)
                msgs.add("A local guide leads your wagon across")
                msgs.add("a safe ford for $${"%.2f".format(cost)}.")
            }
            "wait" -> {
                date.plusDays(1)
                rollWeather()
                consumeFood()
                msgs.add("You wait a day and watch the river.")
                msgs.add("The water is now: ${riverState(river)}.")
                showNotice("River Crossing", msgs, Phase.RIVER)
                return
            }
        }
        addJournal("Crossed the ${lm.name}.")
        showNotice("River Crossing", msgs, Phase.TRAVEL)
    }

    internal fun riverState(river: River): String =
        if (river.widthYards > 500) "wide and swift" else "deep and strong"

    private fun weatherRisk(): Double = when (weather.kind) {
        WeatherKind.HEAVY_RAIN, WeatherKind.THUNDERSTORM -> 0.15
        WeatherKind.RAIN, WeatherKind.HAIL -> 0.08
        else -> 0.0
    }

    private fun supplyLoss(msgs: MutableList<String>, food: Int, clothes: Int) {
        val lostFood = min(inventory.food, food)
        val lostClothes = min(inventory.clothing, clothes)
        inventory.food -= lostFood
        inventory.clothing -= lostClothes
        if (lostFood > 0) msgs.add("You lose $lostFood pounds of food.")
        if (lostClothes > 0) msgs.add("You lose $lostClothes set(s) of clothing.")
    }

    // ====================================================================
    //  The Dalles / end of trail
    // ====================================================================

    private fun handleDalles(action: String) {
        when (action) {
            "barlow" -> {
                val toll = 5.0
                if (inventory.cash < toll) {
                    showNotice("The Dalles", listOf("You cannot afford the Barlow Road toll."), Phase.LANDMARK)
                    return
                }
                inventory.cash -= toll
                startBarlow()
                return
            }
            "raft" -> {
                startRaft()
                return
            }
            "portage" -> {
                val days = rng.nextInt(4, 8)
                date.plusDays(days)
                arriveOregon(
                    listOf(
                        "You portage around the rapids, hauling the",
                        "wagons overland. It is slow and exhausting,",
                        "taking $days days, but nothing is lost.",
                        "At last, the Willamette Valley lies below."
                    )
                )
                return
            }
            "wait" -> {
                date.plusDays(1)
                showNotice("The Dalles", listOf("You wait for better conditions."), Phase.LANDMARK)
                return
            }
        }
    }

    private fun startBarlow() {
        val (w, h) = barlowSize()
        barlowField = BarlowField(w, h, rng)
        phase = Phase.BARLOW
    }

    private fun barlowSize(): Pair<Int, Int> =
        min(contentW, 40).coerceIn(10, 40) to min(rows - 5, 16).coerceIn(4, 16)

    private fun finishBarlow() {
        val field = barlowField ?: return
        val msgs = ArrayList<String>()
        if (field.success) {
            msgs.add("After a long climb over the Cascades, the")
            msgs.add("Barlow Road brings you down into the valley.")
            if (field.damage > 0) {
                supplyLoss(msgs, field.damage * 8, 0)
                msgs.add("The rocks and ruts cost you some supplies.")
            }
        } else {
            msgs.add("A wheel shatters on the rocks and the")
            msgs.add("wagon is dragged down the slope.")
            supplyLoss(msgs, 40, 1)
        }
        addJournal("Took the Barlow Road over the Cascades.")
        barlowField = null
        arriveOregon(msgs)
    }

    private fun startRaft() {
        val (w, h) = raftSize()
        raftField = RaftField(w, h, rng)
        phase = Phase.RAFTING
    }

    private fun finishRaft() {
        val field = raftField ?: return
        val msgs = ArrayList<String>()
        if (field.success) {
            msgs.add("You pilot the raft through the crashing")
            msgs.add("rapids of the Columbia and reach the shore.")
            if (field.damage() > 0) {
                supplyLoss(msgs, field.damage() * 12, 0)
                msgs.add("The rocks cost you some supplies.")
            }
        } else {
            msgs.add("Your raft is smashed on the rocks!")
            msgs.add("You struggle ashore, losing supplies.")
            supplyLoss(msgs, 60, 1)
        }
        raftField = null
        arriveOregon(msgs)
    }

    private fun arriveOregon(extra: List<String> = emptyList()) {
        lastScore = computeScore()
        addJournal("Arrived safely in the Willamette Valley!")
        topTen.add(ScoreEntry(party.firstOrNull()?.name ?: "Traveler", lastScore, occupation.displayName))
        topTen = topTen.sortedByDescending { it.points }.take(10).toMutableList()
        scores.saveScores(topTen)
        recordStats {
            it.copy(
                arrivals = it.arrivals + 1,
                bestScore = maxOf(it.bestScore, lastScore),
                totalMiles = it.totalMiles + miles
            )
        }
        unlock(Achievements.REACHED_OREGON)
        if (aliveCount == 5) unlock(Achievements.ALL_FIVE_ALIVE)
        if (aliveCount == 1) unlock(Achievements.SURVIVOR)
        if (inventory.cash >= 500) unlock(Achievements.FRUGAL)
        if (inventory.cash >= 1000) unlock(Achievements.WEALTHY)
        if (topTen.any { it.points == lastScore }) unlock(Achievements.TOP_TEN)
        pendingSound = Sound.ARRIVAL
        noticeTitle = "Oregon!"
        noticeLines.clear()
        noticeLines.addAll(extra)
        if (noticeLines.isNotEmpty()) noticeLines.add("")
        noticeLines.add("You have reached the Willamette Valley!")
        noticeLines.add("")
        noticeLines.add("You traveled ${miles} miles in ${daysOnTrail()} days.")
        noticeLines.add("Your final score is $lastScore points.")
        noticeNext = Phase.ARRIVED
        phase = Phase.NOTICE
    }

    internal fun daysOnTrail(): Int {
        // Rough day count from March 1 to current date.
        var count = 0
        val start = GameDate(1848, travelMonth.monthIndex, 1)
        while (start.month != date.month || start.day != date.day || start.year != date.year) {
            start.plusDays(1)
            count++
            if (count > 400) break
        }
        return count
    }

    // ====================================================================
    //  Death
    // ====================================================================

    private fun dieOf(cause: String) {
        deathCause = cause
        val leader = party.firstOrNull()?.name ?: "Traveler"
        val epitaph = "Here lies $leader, died of $cause on the Oregon Trail."
        lastGravestone = epitaph
        addJournal("$leader died of $cause.")
        val lm = Data.landmarkAt(landmarkIndex)
        val grave = Grave(leader, cause, lm.id, epitaph)
        graves.add(grave)
        if (graves.size > GRAVE_LIMIT) graves.removeAt(0)
        scores.addGrave(grave)
        scores.saveGravestone(epitaph)
        recordStats { it.copy(deaths = it.deaths + 1, totalMiles = it.totalMiles + miles) }
        if (cause.contains("dysentery", ignoreCase = true)) unlock(Achievements.DYSENTERY)
        pendingSound = Sound.DEATH
        phase = Phase.DEATH
    }

    // ====================================================================
    //  Hunting
    // ====================================================================

    private fun huntPool(): List<AnimalKind> {
        val m = miles
        return when {
            m < 600 -> listOf(AnimalKind.RABBIT, AnimalKind.SQUIRREL, AnimalKind.DEER, AnimalKind.ANTELOPE, AnimalKind.BUFFALO)
            m < 1300 -> listOf(AnimalKind.RABBIT, AnimalKind.SQUIRREL, AnimalKind.DEER, AnimalKind.ANTELOPE, AnimalKind.WOLF)
            else -> listOf(AnimalKind.SQUIRREL, AnimalKind.DEER, AnimalKind.BEAR, AnimalKind.WOLF)
        }
    }

    private fun startHunt(returnPhase: Phase) {
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
    }

    private fun huntShoot() {
        val field = huntField ?: return
        if (inventory.ammo <= 0) { pendingSound = Sound.BAD; return }
        if (field.shoot()) {
            inventory.ammo -= 1
            pendingSound = Sound.SHOOT
        }
    }

    private fun endHunt() {
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

    private fun handleChoice(id: String) {
        when {
            id.startsWith("riders:") -> handleRiders(id.substringAfter("riders:"))
            id.startsWith("trade:") -> handleTradeResult(id)
            id.startsWith("rest:") -> rest(id.substringAfter("rest:").toIntOrNull() ?: 3)
            else -> phase = choiceNext
        }
    }

    /** Applies a trade, scaling what you receive by [bonus]. */
    private fun applyTrade(kind: String, bonus: Double, msgs: MutableList<String>): Boolean {
        return when (kind) {
            "food80" -> {
                if (inventory.clothing >= 1) {
                    inventory.clothing -= 1
                    inventory.food += (80 * bonus).toInt()
                    msgs.add("You trade a set of clothing for")
                    msgs.add("${(80 * bonus).toInt()} pounds of food.")
                    true
                } else { msgs.add("You do not have the goods to trade."); false }
            }
            "wheel" -> {
                if (inventory.clothing >= 1) {
                    inventory.clothing -= 1
                    inventory.wheels += 1
                    if (bonus > 1.0) inventory.axles += 1
                    msgs.add("You trade a set of clothing for a spare wheel.")
                    if (bonus > 1.0) msgs.add("He throws in an axle as well.")
                    true
                } else { msgs.add("You do not have the goods to trade."); false }
            }
            "ammo" -> {
                if (inventory.food >= 30) {
                    inventory.food -= 30
                    inventory.ammo += (40 * bonus).toInt()
                    msgs.add("You trade 30 pounds of food for")
                    msgs.add("${(40 * bonus).toInt()} bullets.")
                    true
                } else { msgs.add("You do not have the goods to trade."); false }
            }
            "oxen" -> {
                if (inventory.food >= 50) {
                    inventory.food -= 50
                    inventory.oxen += 2
                    if (bonus > 1.0) inventory.food += 20
                    msgs.add("You trade 50 pounds of food for a yoke of oxen.")
                    if (bonus > 1.0) msgs.add("He throws in 20 pounds of food.")
                    true
                } else { msgs.add("You do not have the goods to trade."); false }
            }
            else -> false
        }
    }

    private fun handleTradeResult(id: String) {
        val parts = id.split(":")
        val action = parts.getOrElse(1) { "" }
        if (action == "no") {
            showNotice("Trading", listOf("You decline the trade and move on."), Phase.TRAVEL)
            return
        }
        val kind = parts.getOrElse(2) { "" }
        val msgs = ArrayList<String>()
        if (action == "haggle") {
            // Bankers are practised at driving a bargain.
            val skill = if (occupation == Occupation.BANKER) 0.6 else 0.45
            if (rng.chance(skill)) {
                unlock(Achievements.BARGAINER)
                msgs.add("You haggle hard and the trader sweetens the deal.")
                applyTrade(kind, 1.25, msgs)
            } else {
                msgs.add("The trader scowls at your cheek and packs up.")
                msgs.add("No deal.")
            }
            showNotice("Bargaining", msgs, Phase.TRAVEL)
            return
        }
        applyTrade(kind, 1.0, msgs)
        showNotice("Trading", msgs, Phase.TRAVEL)
    }

    // ====================================================================
    //  Scoring
    // ====================================================================

    private fun computeScore(): Int {
        val parts = scoreParts()
        return parts.last().second
    }

    /** Returns labelled score components; the final entry is the total. */
    internal fun scoreParts(): List<Pair<String, Int>> {
        val survivors = aliveCount * 400
        val oxen = inventory.oxen * 30
        val food = inventory.food / 5
        val clothing = inventory.clothing * 15
        val ammo = inventory.ammo / 2
        val spares = (inventory.wheels + inventory.axles + inventory.tongues) * 20
        val cash = (inventory.cash / 10).toInt()
        val subtotal = 1500 + survivors + oxen + food + clothing + ammo + spares + cash
        val total = subtotal * occupation.multiplier
        return listOf(
            "Survivors ($aliveCount x 400)" to survivors,
            "Oxen (${inventory.oxen} x 30)" to oxen,
            "Food (${inventory.food} lb)" to food,
            "Clothing (${inventory.clothing})" to clothing,
            "Ammunition (${inventory.ammo})" to ammo,
            "Spare parts" to spares,
            "Cash" to cash,
            "Arrival bonus" to 1500,
            "Subtotal x${occupation.multiplier} (${occupation.displayName})" to total
        )
    }

    // ====================================================================
    //  Notices
    // ====================================================================

    private fun showNotice(title: String, lines: List<String>, next: Phase) {
        noticeTitle = title
        noticeLines.clear()
        noticeLines.addAll(lines)
        noticeNext = next
        phase = Phase.NOTICE
    }

    internal fun wrapString(s: String, width: Int): List<String> {
        if (width < 4) return listOf(s)
        val out = ArrayList<String>()
        var line = StringBuilder()
        for (word in s.split(' ')) {
            when {
                line.isEmpty() && word.length > width -> {
                    var w = word
                    while (w.length > width) {
                        out.add(w.substring(0, width))
                        w = w.substring(width)
                    }
                    line.append(w)
                }
                line.isEmpty() -> line.append(word)
                line.length + 1 + word.length <= width -> line.append(' ').append(word)
                else -> {
                    out.add(line.toString())
                    line = StringBuilder(word)
                }
            }
        }
        if (line.isNotEmpty()) out.add(line.toString())
        if (out.isEmpty()) out.add("")
        return out
    }

    internal fun suppliesLines(): List<String> {
        val yokes = inventory.oxen / 2
        return listOf(
            "Cash: $${"%.2f".format(inventory.cash)}",
            "Oxen: ${inventory.oxen} ($yokes yoke, ${oxCondition()})",
            "Food: ${inventory.food} pounds",
            "Clothing: ${inventory.clothing} sets",
            "Ammunition: ${inventory.ammo / 20} boxes (${inventory.ammo} bullets)",
            "Spare parts: ${inventory.wheels} wheels, ${inventory.axles} axles, ${inventory.tongues} tongues",
            "",
            "Party health:",
            *party.map { "  ${it.name}: ${it.state.displayName}" }.toTypedArray()
        )
    }

    // ====================================================================
    //  Rendering (see GameRender.kt)
    // ====================================================================

    /** Builds the current screen. Rendering itself lives in GameRender.kt. */
    fun render(): Screen {
        val screen = Screen(cols, rows)
        screen.fillBackground(Palette.BLACK)
        when (phase) {
            Phase.TITLE -> renderTitle(screen)
            Phase.ABOUT -> renderAbout(screen)
            Phase.MANAGEMENT -> renderManagement(screen)
            Phase.TOP_TEN -> renderTopTen(screen)
            Phase.PROFESSION -> renderProfession(screen)
            Phase.MONTH -> renderMonth(screen)
            Phase.NAMES -> renderNames(screen)
            Phase.STORE -> renderStore(screen)
            Phase.TRAVEL -> renderTravel(screen)
            Phase.LANDMARK -> renderLandmark(screen)
            Phase.RIVER -> renderRiver(screen)
            Phase.MAP -> renderMap(screen)
            Phase.JOURNAL -> renderJournal(screen)
            Phase.LOAD -> renderLoad(screen)
            Phase.ACHIEVEMENTS -> renderAchievements(screen)
            Phase.STATS -> renderStats(screen)
            Phase.EPILOGUE -> renderEpilogue(screen)
            Phase.PAUSE -> renderPause(screen)
            Phase.CHOICE -> renderChoice(screen)
            Phase.HUNTING -> renderHunting(screen)
            Phase.RAFTING -> renderRafting(screen)
            Phase.BARLOW -> renderBarlow(screen)
            Phase.NOTICE -> renderNotice(screen)
            Phase.DEATH -> renderDeath(screen)
            Phase.ARRIVED -> renderArrived(screen)
        }
        screen.ambient = ambientFor()
        return screen
    }

    /** Chooses a background mood for the current situation. */
    private fun ambientFor(): Palette = when (phase) {
        Phase.RAFTING -> Palette.BLUE
        Phase.BARLOW -> Palette.BROWN
        Phase.HUNTING -> Palette.GREEN
        Phase.TRAVEL, Phase.LANDMARK, Phase.RIVER -> when (weather.kind) {
            WeatherKind.SNOW, WeatherKind.BLIZZARD, WeatherKind.COLD,
            WeatherKind.HEAVY_RAIN, WeatherKind.RAIN, WeatherKind.THUNDERSTORM,
            WeatherKind.HAIL -> Palette.BLUE
            WeatherKind.HOT -> Palette.BROWN
            WeatherKind.CLEAR -> Palette.GREEN
            else -> Palette.BLACK
        }
        else -> Palette.BLACK
    }

    // ====================================================================
    //  Save / restore
    // ====================================================================

    /** Serializes the whole run to a compact, line-based string. */
    fun save(): String {
        val sb = StringBuilder()
        fun line(k: String, v: Any) {
            sb.append(k).append('=').append(v.toString().replace('\n', ' ')).append('\n')
        }
        line("v", 1)
        line("occ", occupation.name)
        line("month", travelMonth.name)
        line("date", "${date.year},${date.month},${date.day}")
        line("weather", "${weather.kind.name},${weather.tempF}")
        line("miles", miles)
        line("landmark", landmarkIndex)
        line("cutoff", cutoffTarget)
        line("pace", pace.name)
        line("rations", rations.name)
        line("oxHealth", oxHealth)
        line("difficulty", difficulty.name)
        line("storeAtFort", storeAtFort)
        line("sound", soundEnabled)
        line("cash", inventory.cash)
        line("oxen", inventory.oxen)
        line("food", inventory.food)
        line("clothing", inventory.clothing)
        line("ammo", inventory.ammo)
        line("wheels", inventory.wheels)
        line("axles", inventory.axles)
        line("tongues", inventory.tongues)
        line("phase", safePhase().name)
        party.forEachIndexed { i, m ->
            line("p$i", "${enc(m.name)},${m.health},${m.alive},${enc(m.condition ?: "")}")
        }
        journal.forEachIndexed { i, e ->
            line("j$i", "${enc(e.date)}|${enc(e.text)}")
        }
        return sb.toString()
    }

    /** Restores a run from [data]. Returns false if the data is unusable. */
    fun load(data: String): Boolean {
        val map = HashMap<String, String>()
        for (l in data.split('\n')) {
            val i = l.indexOf('=')
            if (i > 0) map[l.substring(0, i)] = l.substring(i + 1)
        }
        if (map["v"] != "1") return false
        return try {
            occupation = Occupation.valueOf(map["occ"] ?: return false)
            travelMonth = TravelMonth.valueOf(map["month"] ?: return false)
            val d = (map["date"] ?: return false).split(',')
            date = GameDate(d[0].toInt(), d[1].toInt(), d[2].toInt())
            val w = (map["weather"] ?: return false).split(',')
            weather = Weather(WeatherKind.valueOf(w[0]), w[1].toInt())
            miles = map["miles"]?.toIntOrNull() ?: 0
            landmarkIndex = (map["landmark"]?.toIntOrNull() ?: 0).coerceIn(0, Data.landmarks.lastIndex)
            cutoffTarget = (map["cutoff"]?.toIntOrNull() ?: -1)
                .coerceIn(-1, Data.landmarks.lastIndex)
            pace = Pace.valueOf(map["pace"] ?: pace.name)
            rations = Rations.valueOf(map["rations"] ?: rations.name)
            oxHealth = (map["oxHealth"]?.toIntOrNull() ?: 100).coerceIn(0, 100)
            difficulty = try {
                Difficulty.valueOf(map["difficulty"] ?: "NORMAL")
            } catch (_: Exception) {
                Difficulty.NORMAL
            }
            storeAtFort = map["storeAtFort"]?.toBooleanStrictOrNull() ?: false
            soundEnabled = map["sound"]?.toBooleanStrictOrNull() ?: true
            inventory.cash = map["cash"]?.toDoubleOrNull() ?: 0.0
            inventory.oxen = map["oxen"]?.toIntOrNull() ?: 0
            inventory.food = map["food"]?.toIntOrNull() ?: 0
            inventory.clothing = map["clothing"]?.toIntOrNull() ?: 0
            inventory.ammo = map["ammo"]?.toIntOrNull() ?: 0
            inventory.wheels = map["wheels"]?.toIntOrNull() ?: 0
            inventory.axles = map["axles"]?.toIntOrNull() ?: 0
            inventory.tongues = map["tongues"]?.toIntOrNull() ?: 0
            val saved = ArrayList<PartyMember>()
            for (i in 0 until 5) {
                val raw = map["p$i"] ?: return false
                val parts = raw.split(',')
                saved.add(
                    PartyMember(
                        dec(parts[0]),
                        parts[1].toIntOrNull() ?: 100,
                        parts[2].toBooleanStrictOrNull() ?: true,
                        dec(parts.getOrElse(3) { "" }).ifEmpty { null }
                    )
                )
            }
            party = saved
            journal.clear()
            var ji = 0
            while (true) {
                val raw = map["j$ji"] ?: break
                val parts = raw.split('|')
                journal.add(JournalEntry(dec(parts[0]), dec(parts.getOrElse(1) { "" })))
                ji++
            }
            phase = try {
                Phase.valueOf(map["phase"] ?: "TRAVEL")
            } catch (_: Exception) {
                Phase.TRAVEL
            }
            if (phase in INVALID_RESUME_PHASES) phase = Phase.TRAVEL
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun safePhase(): Phase = when {
        phase == Phase.PAUSE -> if (pauseReturn in PAUSABLE_PHASES) pauseReturn else Phase.TRAVEL
        phase == Phase.RAFTING || phase == Phase.BARLOW -> Phase.LANDMARK
        phase in INVALID_RESUME_PHASES -> Phase.TRAVEL
        else -> phase
    }

    private fun enc(s: String): String =
        s.replace('\\', '/').replace(',', ';').replace('|', '/')
            .replace('\n', ' ').replace('\r', ' ')

    private fun dec(s: String): String = s

    companion object {
        /** Bumped when the engine or its content changes. */
        const val VERSION = "1.8.0"

        /** Caps to keep save files and memory bounded on very long runs. */
        const val JOURNAL_LIMIT = 400
        const val GRAVE_LIMIT = 50

        val INVALID_RESUME_PHASES = setOf(
            Phase.TITLE, Phase.ABOUT, Phase.MANAGEMENT, Phase.TOP_TEN,
            Phase.PROFESSION, Phase.MONTH, Phase.NAMES, Phase.DEATH,
            Phase.ARRIVED, Phase.CHOICE, Phase.HUNTING, Phase.RAFTING, Phase.BARLOW,
            Phase.JOURNAL, Phase.LOAD, Phase.ACHIEVEMENTS, Phase.STATS,
            Phase.EPILOGUE, Phase.PAUSE, Phase.NOTICE
        )

        /** Phases from which the pause menu can be opened. */
        val PAUSABLE_PHASES = setOf(
            Phase.TRAVEL, Phase.LANDMARK, Phase.RIVER, Phase.MAP,
            Phase.JOURNAL, Phase.STORE, Phase.HUNTING, Phase.RAFTING, Phase.BARLOW
        )

        val ABOUT_PAGES: List<String> = listOf(
            "In 1848, thousands of pioneers set out from Independence, Missouri, bound " +
                "for the fertile Willamette Valley of Oregon, 2,040 miles to the west.",
            "Families traveled in covered wagons pulled by oxen. They bought food, " +
                "clothing, ammunition and spare parts, and hoped it would last the " +
                "five or six months the journey required.",
            "Every day brought choices: how hard to push the oxen, how much to eat, " +
                "when to rest, and how to cross the wide rivers of the plains and " +
                "mountains.",
            "Disease was the greatest danger. Cholera, dysentery and typhoid killed " +
                "more travelers than accidents or attacks. Winter snows in the " +
                "mountains could trap a wagon train and destroy it.",
            "Those who survived reached Oregon City and the Willamette Valley, where " +
                "they took up land and began new lives. You are about to make that " +
                "journey. Good luck!"
        )
    }
}

/** A simple, dependency-free helper for picking a random element. */
private fun <T> List<T>.randomOrNull(rng: Rng): T? =
    if (isEmpty()) null else this[rng.nextInt(size)]

/** Named sound cues the front-end may play. */
enum class Sound { CLICK, GOOD, BAD, SHOOT, DEATH, ARRIVAL }
