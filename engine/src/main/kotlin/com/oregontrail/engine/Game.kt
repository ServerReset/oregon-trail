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
    var storeAtFort: Boolean = false
    var lastGravestone: String? = null
    var lastScore: Int = 0
    var topTen: MutableList<ScoreEntry> = ArrayList()

    // ----- transient UI state ------------------------------------------
    private var aboutPage = 0
    private var noticeTitle = ""
    private val noticeLines = ArrayList<String>()
    private var noticeNext: Phase = Phase.TRAVEL
    private var choiceTitle = ""
    private val choiceLines = ArrayList<String>()
    private val choiceOptions = ArrayList<Pair<String, String>>()
    private var choiceNext: Phase = Phase.TRAVEL

    private var huntField: HuntField? = null
    private var huntReturn = Phase.TRAVEL
    private var huntDays = 1

    private var deathCause = ""
    private var restingDays = 0

    var requestedNameEdit: Int? = null
        private set

    private var titleMenuIndex = 0

    init {
        topTen = scores.loadScores()
        lastGravestone = scores.loadGravestone()
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

    private fun aliveMembers(): List<PartyMember> = party.filter { it.alive }
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
            id == "hunt:up" -> huntField?.move(0, -1)
            id == "hunt:down" -> huntField?.move(0, 1)
            id == "hunt:left" -> huntField?.move(-1, 0)
            id == "hunt:right" -> huntField?.move(1, 0)
            id == "hunt:shoot" -> huntShoot()
            id == "hunt:leave" -> endHunt()
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

    private fun priceOf(item: Item): Double = if (storeAtFort) item.fortPrice else item.basePrice

    private fun tapCost(item: Item): Double = when (item) {
        Item.OXEN -> priceOf(item)                 // per yoke
        Item.FOOD -> priceOf(item) * 50
        Item.CLOTHING -> priceOf(item)
        Item.AMMUNITION -> priceOf(item)           // per box
        Item.WHEEL, Item.AXLE, Item.TONGUE -> priceOf(item)
    }

    private fun displayQty(item: Item): Int = when (item) {
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
            "pace" -> cyclePace()
            "rations" -> cycleRations()
            "rest" -> startRest()
            "trade" -> attemptTrade()
            "hunt" -> startHunt(Phase.TRAVEL)
        }
    }

    private fun cyclePace() {
        pace = when (pace) {
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

    private fun nextLandmark(): Landmark? {
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
        if (checkIllness(msgs)) return stopOrDeath()

        // Random event.
        if (rng.chance(0.12)) {
            rollEvent(msgs)
            return stopOrDeath()
        }

        // Reached next landmark?
        val next = nextLandmark()
        if (next != null && miles >= next.mile) {
            // Do not overshoot badly.
            miles = next.mile
            return DayResult.LANDMARK
        }

        return DayResult.OK
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
            if (!rng.chance(chance)) continue
            any = true
            val illness = Data.illnesses[rng.nextInt(Data.illnesses.size)]
            val severity = rng.nextInt(8, 22)
            m.hurt(severity)
            m.condition = illness
            if (!m.alive) {
                msgs.add("${m.name} has died of $illness.")
                deathCause = illness
            } else {
                msgs.add("${m.name} has come down with $illness.")
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
        showNotice(choiceTitle, msgs, Phase.TRAVEL)
    }

    // ====================================================================
    //  Landmarks
    // ====================================================================

    private fun advanceToLandmark(msgs: MutableList<String>) {
        val idx = landmarkIndex + 1
        landmarkIndex = min(idx, Data.landmarks.lastIndex)
        val lm = Data.landmarkAt(landmarkIndex)
        if (lm.kind == LandmarkKind.END) { arriveOregon(); return }
        val lines = ArrayList<String>()
        lines.add("You have reached ${lm.name}.")
        lines.add("")
        lines.addAll(lm.blurb)
        msgs.clear()
        lines.forEach { msgs.add(it) }
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
        }
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
        showNotice("River Crossing", msgs, Phase.TRAVEL)
    }

    private fun riverState(river: River): String =
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
                date.plusDays(rng.nextInt(2, 5))
                if (rng.chance(0.35)) {
                    supplyLoss(msgs, 50, 1)
                    msgs.add("Your raft strikes a rock in the rapids!")
                    msgs.add("You lose supplies but survive the run.")
                    pendingSound = Sound.BAD
                } else {
                    msgs.add("You run the Columbia River rapids in a")
                    msgs.add("borrowed canoe. It is terrifying and quick.")
                }
            }
            "wait" -> {
                date.plusDays(1)
                showNotice("The Dalles", listOf("You wait for better conditions."), Phase.LANDMARK)
                return
            }
        }
        arriveOregon(msgs)
    }

    private fun arriveOregon(extra: List<String> = emptyList()) {
        lastScore = computeScore()
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
        lastGravestone = leader
        scores.saveGravestone(
            "Here lies $leader, died of $cause on the Oregon Trail."
        )
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
        val meat = field.finish()
        inventory.food += meat
        date.plusDays(huntDays)
        consumeFood()
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
        var base = 1500
        base += aliveCount * 400
        base += inventory.oxen * 30
        base += inventory.food / 5
        base += inventory.clothing * 15
        base += inventory.ammo / 2
        base += (inventory.wheels + inventory.axles + inventory.tongues) * 20
        base += (inventory.cash / 10).toInt()
        return base * occupation.multiplier
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

    private fun wrapString(s: String, width: Int): List<String> {
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

    private fun suppliesLines(): List<String> {
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
    //  Rendering
    // ====================================================================

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
            Phase.CHOICE -> renderChoice(screen)
            Phase.HUNTING -> renderHunting(screen)
            Phase.NOTICE -> renderNotice(screen)
            Phase.DEATH -> renderDeath(screen)
            Phase.ARRIVED -> renderArrived(screen)
        }
        return screen
    }

    private val contentW: Int get() = min(cols, 76)
    private val marginX: Int get() = ((cols - contentW) / 2).coerceAtLeast(0)

    private fun Screen.menuAt(x: Int, yStart: Int, options: List<Pair<String, String>>): Int {
        var y = yStart
        for ((label, id) in options) {
            text(x, y, label, Palette.GREEN)
            hotspot(id, x, y, label.length)
            y++
        }
        return y
    }

    private fun Screen.footer(s: String) {
        center(rows - 1, s, Palette.DIM)
    }

    private fun renderTitle(screen: Screen) {
        val art = ArrayList<String>()
        if (contentW >= 36) {
            art.addAll(Ascii.wagon)
            art.addAll(Ascii.blockWord("OREGON"))
            art.addAll(Ascii.blockWord("TRAIL"))
        } else {
            art.addAll(Ascii.wagonSmall)
            art.add("")
            art.add("T H E   O R E G O N   T R A I L")
        }
        val menu = listOf(
            "1. Travel the trail" to "title:travel",
            "2. Learn about the trail" to "title:about",
            "3. See the Oregon Top Ten" to "title:topten",
            "4. Choose Management Options" to "title:manage",
            "5. End" to "title:end"
        )
        val totalH = art.size + 2 + menu.size
        var y = ((rows - totalH) / 2).coerceAtLeast(0)
        Ascii.draw(screen, (cols - Ascii.width(art)) / 2, y, art, Palette.BRIGHT_GREEN)
        y += art.size + 1
        val x = (cols - menu.maxOf { it.first.length }) / 2
        for ((label, id) in menu) {
            screen.text(x, y, label, Palette.GREEN)
            screen.hotspot(id, x, y, label.length)
            y++
        }
        screen.footer("A faithful ASCII recreation of the classic game")
    }

    private fun renderAbout(screen: Screen) {
        val page = ABOUT_PAGES[aboutPage.coerceIn(0, ABOUT_PAGES.size - 1)]
        screen.center(1, "ABOUT THE OREGON TRAIL", Palette.BRIGHT_GREEN, bold = true)
        var y = 3
        y = screen.wrap(marginX + 1, y, contentW - 2, page, Palette.GREEN)
        screen.text(marginX + 1, rows - 3, "Page ${aboutPage + 1} of ${ABOUT_PAGES.size}", Palette.DIM)
        val label = if (aboutPage >= ABOUT_PAGES.size - 1) "[ Back to title ]" else "[ Continue ]"
        screen.text(marginX + 1, rows - 2, label, Palette.BRIGHT_GREEN, bold = true)
        screen.hotspot("about:next", marginX + 1, rows - 2, label.length)
    }

    private fun renderManagement(screen: Screen) {
        screen.center(2, "MANAGEMENT OPTIONS", Palette.BRIGHT_GREEN, bold = true)
        val options = ArrayList<Pair<String, String>>()
        options.add("See the Oregon Top Ten" to "manage:topten")
        options.add("Choose a different leader" to "manage:newleader")
        options.add("Sound is ${if (soundEnabled) "ON" else "OFF"}" to "manage:sound")
        options.add("Return to the title screen" to "manage:back")
        var y = 5
        for ((label, id) in options) {
            screen.text(marginX + 3, y, "$label", Palette.GREEN)
            screen.hotspot(id, marginX + 3, y, label.length)
            y += 2
        }
    }

    private fun renderTopTen(screen: Screen) {
        screen.center(1, "THE OREGON TOP TEN", Palette.BRIGHT_GREEN, bold = true)
        val nameW = (contentW - 12).coerceIn(8, 20)
        screen.text(marginX + 2, 3, "Rank  Name".padEnd(nameW + 8) + "Points", Palette.YELLOW)
        var y = 5
        topTen.take(10).forEachIndexed { i, e ->
            if (y >= rows - 3) return@forEachIndexed
            val name = e.name.take(nameW).padEnd(nameW)
            val points = e.points.toString().padStart(5)
            val line = "${(i + 1).toString().padStart(2)}.   $name $points"
            screen.text(marginX + 1, y, line, Palette.GREEN)
            y++
        }
        val label = "[ Back ]"
        screen.text(marginX + 1, rows - 2, label, Palette.BRIGHT_GREEN, bold = true)
        screen.hotspot("topten:back", marginX + 1, rows - 2, label.length)
    }

    private fun renderProfession(screen: Screen) {
        val compact = rows < 24
        screen.center(1, "CHOOSE YOUR OCCUPATION", Palette.BRIGHT_GREEN, bold = true)
        var y = 3
        y = screen.wrap(marginX + 1, y, contentW - 2,
            "Your occupation decides how much money you start with and how many points you earn.", Palette.GRAY)
        y++
        Occupation.entries.forEachIndexed { i, occ ->
            screen.text(marginX + 1, y, "${i + 1}. ${occ.displayName} - $${occ.startingMoney}", Palette.BRIGHT_YELLOW, bold = true)
            screen.hotspot("prof:$i", marginX + 1, y, 20)
            y++
            if (!compact) {
                y = screen.wrap(marginX + 4, y, contentW - 5, occ.blurb, Palette.GREEN)
                y++
            }
        }
    }

    private fun renderMonth(screen: Screen) {
        val compact = rows < 22
        screen.center(1, "WHEN DO YOU WANT TO START?", Palette.BRIGHT_GREEN, bold = true)
        var y = 3
        if (!compact) {
            y = screen.wrap(marginX + 1, y, contentW - 2,
                "Starting later means better grass, but winter may catch you in the mountains.", Palette.GRAY)
            y++
        }
        TravelMonth.entries.forEachIndexed { i, m ->
            screen.text(marginX + 3, y, "${i + 1}. ${m.displayName}", Palette.GREEN)
            screen.hotspot("month:$i", marginX + 3, y, 14)
            y += if (compact) 1 else 2
        }
    }

    private fun renderNames(screen: Screen) {
        val compact = rows < 24
        screen.center(1, "NAME YOUR PARTY", Palette.BRIGHT_GREEN, bold = true)
        var y = 3
        if (!compact) {
            screen.wrap(marginX + 1, 3, contentW - 2,
                "Tap a name to change it. These five will travel with you.", Palette.GRAY)
            y = 6
        }
        party.forEachIndexed { i, m ->
            val label = "${i + 1}. ${m.name}"
            screen.text(marginX + 3, y, label, Palette.GREEN)
            screen.hotspot("name:$i", marginX + 3, y, label.length)
            y += if (compact) 1 else 2
        }
        val go = "[ Begin the journey ]"
        screen.text(marginX + 1, rows - 2, go, Palette.BRIGHT_GREEN, bold = true)
        screen.hotspot("names:go", marginX + 1, rows - 2, go.length)
    }

    private fun renderStore(screen: Screen) {
        val title = if (storeAtFort) "FORT TRADING POST" else "MATT'S GENERAL STORE"
        screen.center(0, title, Palette.BRIGHT_GREEN, bold = true)
        screen.text(marginX + 1, 1, "Cash: $${"%.2f".format(inventory.cash)}", Palette.BRIGHT_YELLOW, bold = true)
        screen.hline(marginX, 2, contentW, '-', Palette.DIM)

        // Adaptive column layout that always fits the smallest supported width.
        val available = (contentW - 2).coerceAtLeast(20)
        val btnW = 7          // "[-][+]"
        val qtyW = 6
        val priceW = 6
        val nameW = (available - btnW - qtyW - priceW).coerceIn(6, 16)
        val nameX = marginX + 1
        val priceX = nameX + nameW
        val qtyX = priceX + priceW
        val btnX = qtyX + qtyW

        var y = 3
        Item.entries.forEachIndexed { index, item ->
            val name = "$index ${shortItemName(item)}".padEnd(nameW).take(nameW)
            screen.text(nameX, y, name, Palette.GREEN)
            screen.text(priceX, y, "$" + "%.2f".format(priceOf(item)), Palette.GRAY)
            screen.text(qtyX, y, displayQty(item).toString().padStart(qtyW - 1), Palette.WHITE)
            screen.text(btnX, y, "[-][+]", Palette.BRIGHT_GREEN)
            screen.hotspot("store:dec:${item.name}", btnX, y, 3)
            screen.hotspot("store:inc:${item.name}", btnX + 3, y, 4)
            y++
        }
        screen.hline(marginX, y, contentW, '-', Palette.DIM)
        y++
        val leave = "[ Leave the store ]"
        screen.text(marginX + 1, y, leave, Palette.BRIGHT_GREEN, bold = true)
        screen.hotspot("store:leave", marginX + 1, y, leave.length)
        screen.footer("Ammunition is sold by the box of 20 bullets")
    }

    private fun shortItemName(item: Item): String = when (item) {
        Item.OXEN -> "Oxen"
        Item.FOOD -> "Food"
        Item.CLOTHING -> "Cloths"
        Item.AMMUNITION -> "Ammo"
        Item.WHEEL -> "Wheel"
        Item.AXLE -> "Axle"
        Item.TONGUE -> "Tongue"
    }

    private fun renderTravel(screen: Screen) {
        var y = 0
        screen.center(y, "THE OREGON TRAIL", Palette.BRIGHT_GREEN, bold = true)
        y++
        // Status box, wrapped to fit narrow screens.
        val boxW = min(contentW, 46).coerceAtLeast(24)
        val inner = boxW - 4
        val wrapped = ArrayList<String>()
        for (line in statusLines()) wrapped.addAll(wrapString(line, inner))
        screen.box(marginX, y, boxW, wrapped.size + 2, Palette.GREEN, "Status")
        wrapped.forEachIndexed { i, line ->
            screen.text(marginX + 2, y + 1 + i, line, Palette.GREEN)
        }
        y += wrapped.size + 3

        // Scene art for the current region.
        val scene = sceneArt()
        Ascii.draw(screen, (cols - Ascii.width(scene)) / 2, y, scene, Palette.GREEN)
        y += Ascii.height(scene) + 1

        val remaining = rows - y - 1
        val options = travelOptions()
        if (remaining >= options.size + 1) {
            screen.text(marginX + 1, y, "What would you like to do?", Palette.BRIGHT_YELLOW)
            y++
            screen.menuAt(marginX + 1, y, options)
        } else {
            // Compact: put the menu in two columns.
            val half = (options.size + 1) / 2
            val colX = marginX + 1
            val colX2 = marginX + contentW / 2
            options.forEachIndexed { i, (label, id) ->
                val cx = if (i < half) colX else colX2
                val cy = y + (i % half)
                val short = label.substringBefore("  ").trim()
                screen.text(cx, cy, short, Palette.GREEN)
                screen.hotspot(id, cx, cy, short.length)
            }
        }
    }

    private fun statusLines(): List<String> {
        val next = nextLandmark()
        val toNext = if (next != null) "${next.name} in ${(next.mile - miles).coerceAtLeast(0)} mi" else "Oregon!"
        val healthy = aliveMembers().joinToString(", ") { it.name } 
        return listOf(
            date.toString(),
            "Weather: ${weather.description}",
            "Pace: ${pace.displayName}    Rations: ${rations.displayName}",
            "Miles: $miles / ${Data.TOTAL_MILES}",
            "Next: $toNext",
            "Food: ${inventory.food} lb   Clothing: ${inventory.clothing}",
            "Ammo: ${inventory.ammo}   Cash: $${"%.0f".format(inventory.cash)}   Oxen: ${inventory.oxen}",
            if (party.any { !it.alive }) "$healthy" else "All five are alive"
        )
    }

    private fun sceneArt(): List<String> = when (Data.landmarkAt(landmarkIndex).kind) {
        LandmarkKind.MOUNTAINS -> Ascii.mountains
        LandmarkKind.RIVER -> Ascii.river
        LandmarkKind.FORT -> Ascii.fort
        LandmarkKind.START -> Ascii.wagonSmall
        else -> if (miles > 700) Ascii.rock else Ascii.trees
    }

    private fun travelOptions(): List<Pair<String, String>> = listOf(
        "1. Continue on trail" to "travel:continue",
        "2. Check supplies" to "travel:supplies",
        "3. Look at map" to "travel:map",
        "4. Change pace" to "travel:pace",
        "5. Change food rations" to "travel:rations",
        "6. Stop to rest" to "travel:rest",
        "7. Attempt to trade" to "travel:trade",
        "8. Hunt for food" to "travel:hunt"
    )

    private fun renderLandmark(screen: Screen) {
        val lm = Data.landmarkAt(landmarkIndex)
        screen.center(0, lm.name.uppercase(), Palette.BRIGHT_GREEN, bold = true)
        var y = 2
        val art = when (lm.kind) {
            LandmarkKind.FORT -> Ascii.fort
            LandmarkKind.MOUNTAINS -> Ascii.mountains
            LandmarkKind.RIVER -> Ascii.river
            else -> Ascii.rock
        }
        Ascii.draw(screen, (cols - Ascii.width(art)) / 2, y, art, Palette.GREEN)
        y += Ascii.height(art) + 1
        y = screen.wrap(marginX + 1, y, contentW - 2, lm.blurb.joinToString(" "), Palette.GREEN)
        y++
        if (lm.id == "dalles") {
            screen.text(marginX + 1, y, "The last decision of the trail:", Palette.BRIGHT_YELLOW)
            y++
            val options = listOf(
                "1. Take the Barlow Road (toll $5)" to "dalles:barlow",
                "2. Raft down the Columbia River" to "dalles:raft",
                "3. Wait for better weather" to "dalles:wait"
            )
            screen.menuAt(marginX + 1, y, options)
            return
        }
        val options = ArrayList<Pair<String, String>>()
        if (lm.kind == LandmarkKind.FORT) options.add("1. Buy supplies" to "land:buy")
        var n = if (lm.kind == LandmarkKind.FORT) 2 else 1
        options.add("${n++}. Continue on the trail" to "land:continue")
        options.add("${n++}. Check supplies" to "land:supplies")
        options.add("${n++}. Look at the map" to "land:map")
        options.add("${n++}. Stop to rest" to "land:rest")
        if (lm.kind == LandmarkKind.FORT || lm.kind == LandmarkKind.LANDMARK) {
            options.add("${n++}. Talk to people" to "land:talk")
        }
        if (lm.kind == LandmarkKind.FORT || lm.kind == LandmarkKind.LANDMARK || lm.kind == LandmarkKind.MOUNTAINS) {
            options.add("${n++}. Hunt for food" to "land:hunt")
        }
        screen.menuAt(marginX + 1, y, options)
    }

    private fun renderRiver(screen: Screen) {
        val lm = Data.landmarkAt(landmarkIndex)
        val river = lm.river ?: return
        screen.center(0, lm.name.uppercase(), Palette.BRIGHT_GREEN, bold = true)
        var y = 2
        Ascii.draw(screen, (cols - Ascii.width(Ascii.river)) / 2, y, Ascii.river, Palette.CYAN)
        y += Ascii.height(Ascii.river) + 1
        y = screen.wrap(marginX + 1, y, contentW - 2, lm.blurb.joinToString(" "), Palette.GREEN)
        screen.text(marginX + 1, y, "The river is ${riverState(river)}.", Palette.CYAN); y += 2
        val options = ArrayList<Pair<String, String>>()
        var n = 1
        options.add("${n++}. Ford the river" to "river:ford")
        options.add("${n++}. Caulk and float across" to "river:caulk")
        if (river.ferryCost != null) options.add("${n++}. Take the ferry ($${"%.2f".format(river.ferryCost)})" to "river:ferry")
        if (river.guideCost != null) options.add("${n++}. Hire a guide ($${"%.2f".format(river.guideCost)})" to "river:guide")
        options.add("${n++}. Wait a day" to "river:wait")
        screen.menuAt(marginX + 1, y, options)
    }

    private fun renderMap(screen: Screen) {
        screen.center(0, "MAP OF THE OREGON TRAIL", Palette.BRIGHT_GREEN, bold = true)
        screen.text(marginX + 1, 2, "--> West                          East", Palette.DIM)
        val all = Data.landmarks
        val windowSize = min(all.size, max(8, rows - 6))
        val startIdx = (landmarkIndex - windowSize / 2).coerceIn(0, max(0, all.size - windowSize))
        val endIdx = (startIdx + windowSize).coerceAtMost(all.size)
        var y = 4
        for (i in startIdx until endIdx) {
            val lm = all[i]
            val marker = when {
                i < landmarkIndex -> "x"
                i == landmarkIndex -> "*"
                else -> "o"
            }
            val color = when (marker) {
                "x" -> Palette.GRAY
                "*" -> Palette.BRIGHT_YELLOW
                else -> Palette.GREEN
            }
            val name = mapShortName(lm.id)
            screen.text(marginX + 1, y, " $marker $name", color, bold = marker == "*")
            y++
        }
        screen.text(marginX + 1, y, "  Mile $miles of ${Data.TOTAL_MILES}", Palette.WHITE)
        val back = "[ Back ]"
        screen.text(marginX + 1, rows - 2, back, Palette.BRIGHT_GREEN, bold = true)
        screen.hotspot("map:back", marginX + 1, rows - 2, back.length)
    }

    private fun mapShortName(id: String): String = when (id) {
        "independence" -> "Independence, MO"
        "kansas" -> "Kansas River"
        "bigblue" -> "Big Blue River"
        "kearney" -> "Fort Kearney"
        "chimney" -> "Chimney Rock"
        "laramie" -> "Fort Laramie"
        "independence_rock" -> "Independence Rock"
        "southpass" -> "South Pass"
        "green" -> "Green River"
        "bridger" -> "Fort Bridger"
        "soda" -> "Soda Springs"
        "hall" -> "Fort Hall"
        "snake" -> "Snake River"
        "boise" -> "Fort Boise"
        "bluemountains" -> "Blue Mountains"
        "wallawalla" -> "Fort Walla Walla"
        "dalles" -> "The Dalles"
        "willamette" -> "Willamette Valley"
        else -> id
    }

    private fun renderChoice(screen: Screen) {
        screen.center(0, choiceTitle.uppercase(), Palette.BRIGHT_GREEN, bold = true)
        var y = 2
        y = screen.wrap(marginX + 1, y, contentW - 2, choiceLines.joinToString("\n"), Palette.GREEN)
        y++
        screen.menuAt(marginX + 1, y, choiceOptions)
    }

    private fun renderHunting(screen: Screen) {
        val field = huntField ?: return
        screen.center(0, "HUNTING", Palette.BRIGHT_GREEN, bold = true)
        screen.text(marginX + 1, 1, "Meat: ${field.meat}/${field.carryLimit} lb".padEnd(24), Palette.BRIGHT_YELLOW)
        screen.text(marginX + 25, 1, "Ammo: ${inventory.ammo}".padEnd(16), Palette.WHITE)
        screen.text(marginX + 43, 1, "Kills: ${field.kills}", Palette.GREEN)
        val fieldX = marginX + 1
        val fieldY = 3
        screen.box(fieldX - 1, fieldY - 1, field.width + 2, field.height + 2, Palette.GREEN)
        for (fy in 0 until field.height) {
            for (fx in 0 until field.width) {
                val ch: Char
                val color: Palette
                val animal = field.animalGlyphAt(fx, fy)
                when {
                    fx == field.hunterX && fy == field.hunterY -> { ch = '@'; color = Palette.BRIGHT_GREEN }
                    animal != null -> { ch = animal.first; color = animal.second }
                    field.bulletAt(fx, fy) -> { ch = '*'; color = Palette.BRIGHT_WHITE }
                    else -> {
                        val decor = field.decorationAt(fx, fy)
                        if (decor != null) { ch = decor; color = if (decor == 'o') Palette.GRAY else Palette.GREEN }
                        else { ch = ' '; color = Palette.DEFAULT }
                    }
                }
                screen.put(fieldX + fx, fieldY + fy, ch, color)
            }
        }
        // Controls
        var cy = fieldY + field.height + 1
        if (cy > rows - 4) cy = rows - 4
        val cx = marginX + 1
        val up = "  ^  "
        val left = "<    "
        val right = "    >"
        val down = "  v  "
        screen.text(cx + 2, cy, up, Palette.BRIGHT_GREEN); screen.hotspot("hunt:up", cx + 2, cy, up.length)
        screen.text(cx, cy + 1, left, Palette.BRIGHT_GREEN); screen.hotspot("hunt:left", cx, cy + 1, left.length)
        screen.text(cx + 6, cy + 1, right, Palette.BRIGHT_GREEN); screen.hotspot("hunt:right", cx + 6, cy + 1, right.length)
        screen.text(cx + 2, cy + 2, down, Palette.BRIGHT_GREEN); screen.hotspot("hunt:down", cx + 2, cy + 2, down.length)
        val shoot = "[ SHOOT ]"
        screen.text(cx + 14, cy + 1, shoot, Palette.BRIGHT_YELLOW, bold = true)
        screen.hotspot("hunt:shoot", cx + 14, cy + 1, shoot.length)
        val leave = "[ Return to trail ]"
        screen.text(cx + 14, cy + 2, leave, Palette.BRIGHT_GREEN)
        screen.hotspot("hunt:leave", cx + 14, cy + 2, leave.length)
    }

    private fun renderNotice(screen: Screen) {
        screen.center(0, noticeTitle.uppercase(), Palette.BRIGHT_GREEN, bold = true)
        var y = 2
        for (line in noticeLines) {
            y = screen.wrap(marginX + 1, y, contentW - 2, line, Palette.GREEN)
        }
        val label = "[ Continue ]"
        screen.text(marginX + 1, rows - 2, label, Palette.BRIGHT_GREEN, bold = true)
        screen.hotspot("notice:continue", marginX + 1, rows - 2, label.length)
    }

    private fun renderDeath(screen: Screen) {
        screen.center(0, "YOU HAVE DIED", Palette.RED, bold = true)
        val leader = party.firstOrNull()?.name ?: "Traveler"
        val art = Ascii.grave
        var y = max(2, (rows - art.size - 8) / 2)
        Ascii.draw(screen, (cols - Ascii.width(art)) / 2, y, art, Palette.GRAY)
        y += art.size + 1
        val cause = "died of $deathCause"
        screen.center(y, "$leader $cause", Palette.WHITE, bold = true)
        screen.center(y + 1, "on ${date}", Palette.GRAY)
        y += 3
        val options = listOf(
            "See the Oregon Top Ten" to "death:topten",
            "Try again" to "death:restart"
        )
        val x = (cols - 22) / 2
        screen.menuAt(x, y, options)
        lastGravestone?.let { screen.wrap(marginX + 1, y + 3, contentW - 2, it, Palette.DIM, bg = Palette.BLACK) }
    }

    private fun renderArrived(screen: Screen) {
        screen.center(0, "OREGON!", Palette.BRIGHT_GREEN, bold = true)
        val art = Ascii.blockWord("WELCOME")
        var y = 2
        Ascii.draw(screen, (cols - Ascii.width(art)) / 2, y, art, Palette.BRIGHT_YELLOW)
        y += art.size + 2
        y = screen.wrap(marginX + 1, y, contentW - 2,
            "You have reached the end of the Oregon Trail and the fertile Willamette Valley.",
            Palette.GREEN)
        y++
        screen.text(marginX + 1, y, "Final score: $lastScore points", Palette.BRIGHT_YELLOW, bold = true)
        y += 2
        val options = listOf(
            "See the Oregon Top Ten" to "arrived:topten",
            "Travel the trail again" to "arrived:restart"
        )
        screen.menuAt(marginX + 1, y, options)
    }

    companion object {
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
enum class Sound { CLICK, GOOD, BAD, SHOOT, HIT, DEATH, ARRIVAL }
