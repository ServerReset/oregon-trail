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
    JOURNAL,
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
        this.cols = cols.coerceAtLeast(20)
        this.rows = rows.coerceAtLeast(16)
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
    var difficulty: Difficulty = Difficulty.NORMAL
    var storeAtFort: Boolean = false
    var lastGravestone: String? = null
    var lastScore: Int = 0
    var topTen: MutableList<ScoreEntry> = ArrayList()
    val journal: MutableList<JournalEntry> = ArrayList()
    val graves: MutableList<Grave> = ArrayList()

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
    private var cutoffTarget: Int = -1

    internal var deathCause = ""

    var requestedNameEdit: Int? = null
        private set

    init {
        topTen = scores.loadScores()
        lastGravestone = scores.loadGravestone()
        graves.addAll(scores.loadGraves())
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
    private val aliveCount: Int get() = aliveMembers().size

    // ====================================================================
    //  Public input
    // ====================================================================

    fun onTap(id: String) {
        pendingSound = Sound.CLICK
        when {
            id == "title:travel" -> { phase = Phase.PROFESSION }
            id == "title:about" -> { aboutPage = 0; phase = Phase.ABOUT }
            id == "title:topten" -> phase = Phase.TOP_TEN
            id == "title:manage" -> phase = Phase.MANAGEMENT
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
            id == "manage:back" -> phase = Phase.TITLE
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
            id == "death:topten" -> phase = Phase.TOP_TEN
            id == "death:restart" -> { newRun(occupation, travelMonth); phase = Phase.PROFESSION }
            id == "arrived:topten" -> phase = Phase.TOP_TEN
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
            if (displayQty(item) <= 0) return
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

    /** Records a dated line in the traveler's journal. */
    private fun addJournal(text: String) {
        val dateText = "${date.monthName} ${date.day}"
        journal.add(JournalEntry(dateText, text))
        if (journal.size > JOURNAL_LIMIT) journal.removeAt(0)
    }

    private fun startRest() {
        // Rest for a few days: heal, consume food, pass time.
        val days = 3
        var report = "You camp and rest for $days days."
        repeat(days) {
            date.plusDays(1)
            rollWeather()
            consumeFood()
        }
        aliveMembers().forEach { it.heal(12) }
        val healed = aliveMembers().joinToString(", ") { "${it.name} (${it.state.displayName})" }
        report += "\n\nRest helps. Your party's health: $healed."
        addJournal("Rested for $days days to recover.")
        showNotice("Resting", listOf(report), Phase.TRAVEL)
    }

    private fun attemptTrade() {
        // A nearby party offers a trade.
        val offers = listOf(
            Triple("80 pounds of food", "1 set of clothing", "food80"),
            Triple("1 spare wheel", "1 set of clothing", "wheel"),
            Triple("40 bullets", "30 pounds of food", "ammo"),
            Triple("1 yoke of oxen", "50 pounds of food", "oxen")
        )
        val offer = offers[rng.nextInt(offers.size)]
        choiceTitle = "Trading"
        choiceLines.clear()
        choiceLines.add("A party camped nearby offers you")
        choiceLines.add("${offer.first} in exchange for ${offer.second}.")
        choiceLines.add("")
        choiceLines.add("Do you accept?")
        choiceOptions.clear()
        choiceOptions.add("Yes, trade" to "trade:yes:${offer.third}")
        choiceOptions.add("No thanks" to "trade:no")
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
        val terrain = terrainFactor(Data.landmarkAt(landmarkIndex).kind)
        var gained = (pace.milesPerDay * weatherFactor * oxenFactor * terrain).toInt()
        if (inventory.oxen <= 0) gained = 0
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
            "wild_animals", "fire", "fog", "indians", "thief", "fruit", "riders"
        )
        val mountains = listOf(
            "breakdown", "ox_lame", "ox_wander", "unsafe_water", "heavy_rain",
            "hail", "bandits", "wild_animals", "fire", "fog", "snakebite",
            "cold", "blizzard", "indians", "riders"
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
                msgs.add("An ox goes lame. You slow down and")
                msgs.add("lose $lost miles resting the animal.")
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
            "riders" -> startRidersChoice()
        }
    }

    private fun startRidersChoice() {
        choiceTitle = "Riders Ahead"
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
            "hunt" -> startHunt(Phase.LANDMARK)
            "cutoff" -> takeCutoff()
        }
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
        val tales = listOf(
            "An old trapper warns: \"Keep to the high ground and watch for alkali water.\"",
            "\"The Snake River is fearsome this year,\" says a settler. \"Hire a guide.\"",
            "A missionary family shares a meal and news from the Willamette.",
            "\"We buried two on the plains,\" says a widow quietly. \"Take your time.\"",
            "A young man boasts he will be in Oregon by August. The old-timers smile.",
            "\"Buy all the food you can at Fort Hall,\" advises a wagon captain.",
            "\"There is good grass past Chimney Rock,\" says a scout. \"Rest your teams there.\""
        )
        showNotice(lm.name, listOf(tales[rng.nextInt(tales.size)]), Phase.LANDMARK)
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
        val msgs = ArrayList<String>()
        when (action) {
            "barlow" -> {
                val toll = 5.0
                if (inventory.cash < toll) {
                    showNotice("The Dalles", listOf("You cannot afford the Barlow Road toll."), Phase.LANDMARK)
                    return
                }
                inventory.cash -= toll
                val lost = rng.nextInt(4, 10)
                date.plusDays(lost)
                msgs.add("You pay the $5 toll and take the Barlow")
                msgs.add("Road over the Cascades. It takes $lost days.")
                msgs.add("At last, the Willamette Valley lies below.")
            }
            "raft" -> {
                startRaft()
                return
            }
            "wait" -> {
                date.plusDays(1)
                showNotice("The Dalles", listOf("You wait for better conditions."), Phase.LANDMARK)
                return
            }
        }
        arriveOregon(msgs)
    }

    private fun startRaft() {
        raftField = RaftField(
            min(contentW - 2, 40).coerceAtLeast(14),
            min(rows - 10, 16).coerceIn(6, 16),
            rng
        )
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

    private fun daysOnTrail(): Int {
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
        val fieldW = min(cols - 2, 64).coerceIn(20, 64)
        val fieldH = min(rows - 11, 16).coerceIn(6, 16)
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
            else -> phase = choiceNext
        }
    }

    private fun handleTradeResult(id: String) {
        val parts = id.split(":")
        if (parts.size < 2 || parts[1] == "no") {
            showNotice("Trading", listOf("You decline the trade and move on."), Phase.TRAVEL)
            return
        }
        val kind = parts.getOrNull(2) ?: ""
        val msgs = ArrayList<String>()
        when (kind) {
            "food80" -> {
                if (inventory.clothing >= 1) {
                    inventory.clothing -= 1; inventory.food += 80
                    msgs.add("You trade a set of clothing for")
                    msgs.add("80 pounds of food. A good bargain.")
                } else msgs.add("You do not have the goods to trade.")
            }
            "wheel" -> {
                if (inventory.clothing >= 1) {
                    inventory.clothing -= 1; inventory.wheels += 1
                    msgs.add("You trade a set of clothing for a")
                    msgs.add("spare wheel.")
                } else msgs.add("You do not have the goods to trade.")
            }
            "ammo" -> {
                if (inventory.food >= 30) {
                    inventory.food -= 30; inventory.ammo += 40
                    msgs.add("You trade 30 pounds of food for")
                    msgs.add("40 bullets.")
                } else msgs.add("You do not have the goods to trade.")
            }
            "oxen" -> {
                if (inventory.food >= 50) {
                    inventory.food -= 50; inventory.oxen += 2
                    msgs.add("You trade 50 pounds of food for a")
                    msgs.add("yoke of oxen.")
                } else msgs.add("You do not have the goods to trade.")
            }
        }
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
            "Oxen: ${inventory.oxen} ($yokes yoke)",
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
            Phase.CHOICE -> renderChoice(screen)
            Phase.HUNTING -> renderHunting(screen)
            Phase.RAFTING -> renderRafting(screen)
            Phase.NOTICE -> renderNotice(screen)
            Phase.DEATH -> renderDeath(screen)
            Phase.ARRIVED -> renderArrived(screen)
        }
        return screen
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
        phase == Phase.RAFTING -> Phase.LANDMARK
        phase in INVALID_RESUME_PHASES -> Phase.TRAVEL
        else -> phase
    }

    private fun enc(s: String): String =
        s.replace('\\', '/').replace(',', ';').replace('|', '/')
            .replace('\n', ' ').replace('\r', ' ')

    private fun dec(s: String): String = s

    companion object {
        /** Bumped when the engine or its content changes. */
        const val VERSION = "1.1.0"

        /** Caps to keep save files and memory bounded on very long runs. */
        const val JOURNAL_LIMIT = 400
        const val GRAVE_LIMIT = 50

        val INVALID_RESUME_PHASES = setOf(
            Phase.TITLE, Phase.ABOUT, Phase.MANAGEMENT, Phase.TOP_TEN,
            Phase.PROFESSION, Phase.MONTH, Phase.NAMES, Phase.DEATH,
            Phase.ARRIVED, Phase.CHOICE, Phase.HUNTING, Phase.RAFTING,
            Phase.JOURNAL, Phase.NOTICE
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
