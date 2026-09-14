package com.oregontrail.engine

/** Occupations the player can choose. Higher difficulty = higher score multiplier. */
enum class Occupation(
    val displayName: String,
    val startingMoney: Int,
    val multiplier: Int,
    val blurb: String,
    val perk: String
) {
    BANKER(
        "Banker", 1600, 1,
        "You start with the most money but earn the fewest points.",
        "Perk: merchants give you a 10% discount at forts."
    ),
    CARPENTER(
        "Carpenter", 800, 2,
        "A skilled trade earns a middle income and middle points.",
        "Perk: you can often repair a broken wagon without a spare part."
    ),
    FARMER(
        "Farmer", 400, 3,
        "You start with little money but earn the most points.",
        "Perk: you know game, and bring home 50% more meat from a hunt."
    )
}

/** Overall difficulty, which scales how often trouble finds you. */
enum class Difficulty(val displayName: String, val eventChance: Double, val illnessScale: Double) {
    EASY("Easy", 0.08, 0.7),
    NORMAL("Normal", 0.12, 1.0),
    HARD("Hard", 0.17, 1.4)
}

/** Presentation settings owned by the front-end. */
interface UiSettings {
    var textScaleIndex: Int      // 0 = small, 1 = medium, 2 = large
    var highContrast: Boolean
    var scanlines: Boolean

    /** 0 = Retro green terminal, 1 = Material You dynamic colors. */
    var themeIndex: Int
        get() = 0
        set(_) {}
}

/** One dated line in the traveler's journal. */
data class JournalEntry(val date: String, val text: String)

/** Achievements the player can unlock across runs. */
object Achievements {
    const val REACHED_OREGON = "reached_oregon"
    const val ALL_FIVE_ALIVE = "all_five_alive"
    const val TOP_TEN = "top_ten"
    const val BIG_HUNT = "big_hunt"
    const val SHARPSHOOTER = "sharpshooter"
    const val FRUGAL = "frugal"
    const val WEALTHY = "wealthy"
    const val CARPENTER = "carpenter"
    const val FERRYMAN = "ferryman"
    const val DYSENTERY = "dysentery"
    const val SURVIVOR = "survivor"
    const val RIVERBANK = "riverbank"
    const val GOOD_SAMARITAN = "good_samaritan"
    const val HISTORIAN = "historian"
    const val BARGAINER = "bargainer"

    val all: List<Achievement> = listOf(
        Achievement(REACHED_OREGON, "Oregon or Bust", "Reach the Willamette Valley."),
        Achievement(ALL_FIVE_ALIVE, "Everyone Made It", "Arrive with all five travelers alive."),
        Achievement(TOP_TEN, "Trail Legend", "Finish with a score in the Oregon Top Ten."),
        Achievement(BIG_HUNT, "Meat on the Wagon", "Bring back a full 100 lb from a hunt."),
        Achievement(SHARPSHOOTER, "Sharpshooter", "Kill three animals in a single hunt."),
        Achievement(FRUGAL, "Frugal", "Arrive in Oregon with $500 or more."),
        Achievement(WEALTHY, "Well Off", "Arrive with $1,000 or more."),
        Achievement(CARPENTER, "Handy", "Repair a wagon breakdown without a spare part."),
        Achievement(FERRYMAN, "Ferryman", "Pay for a ferry crossing."),
        Achievement(RIVERBANK, "Forced a Crossing", "Ford or caulk a river."),
        Achievement(DYSENTERY, "You Have Died of Dysentery", "Lose a traveler to dysentery."),
        Achievement(SURVIVOR, "Sole Survivor", "Reach Oregon with only one traveler alive."),
        Achievement(GOOD_SAMARITAN, "Good Samaritan", "Share your food with a stranded family."),
        Achievement(HISTORIAN, "Student of the Trail", "Read the history at five landmarks."),
        Achievement(BARGAINER, "Wheeler-Dealer", "Haggle a trader into a better deal.")
    )

    fun name(id: String): String = all.firstOrNull { it.id == id }?.name ?: id
}

/** Historical notes shown at landmarks ("Learn the history"). */
object Facts {
    val byLandmark: Map<String, String> = mapOf(
        "independence" to "Independence was the main jumping-off point; by 1849 over 30,000 emigrants a year passed through.",
        "kansas" to "Most travelers crossed the Kansas River by ferry, paying a few dollars rather than risk a ford.",
        "bigblue" to "The Big Blue was notorious for spring floods that stranded whole wagon trains for days.",
        "kearney" to "Fort Kearney was established in 1848 to protect travelers on the Platte River road.",
        "chimney" to "Chimney Rock is a spire of clay and sandstone that once stood over 100 feet tall.",
        "laramie" to "Fort Laramie began as a fur-trading post and became the great supply point of the high plains.",
        "independence_rock" to "Independence Rock is a granite dome 1,900 feet long; reaching it by July 4 was a rite of passage.",
        "southpass" to "South Pass is a broad, gentle gap in the Rockies that wagons could cross without ropes.",
        "green" to "The Green River was the major obstacle of the Rockies; ferries and guides profited here.",
        "bridger" to "Jim Bridger's fort was famous for tall tales and expensive supplies.",
        "soda" to "Soda Springs still bubbles naturally; emigrants wrote it tasted like the new 'soda water'.",
        "hall" to "Fort Hall was a Hudson's Bay Company post and the last real store before the desert.",
        "snake" to "The Snake River's rapids drowned many; some travelers floated their wagons downstream.",
        "boise" to "Fort Boise was a small, remote post on the way to the Blue Mountains.",
        "bluemountains" to "The Blue Mountains were steep, wooded and muddy, and snow could close the road early.",
        "wallawalla" to "Fort Walla Walla stood where the Walla Walla River meets the Columbia.",
        "dalles" to "At The Dalles the Columbia squeezed through the Cascades; many rafted or took the Barlow toll road."
    )

    fun forLandmark(id: String): String? = byLandmark[id]
}

/** Things people say when you talk to them on the trail. */
object Talk {
    /** Flavor lines. */
    val lines: List<String> = listOf(
        "An old trapper warns: \"Keep to the high ground and watch for alkali water.\"",
        "\"The Snake River is fearsome this year,\" says a settler. \"Hire a guide.\"",
        "A missionary family shares a meal and news from the Willamette.",
        "\"We buried two on the plains,\" says a widow quietly. \"Take your time.\"",
        "A young man boasts he will be in Oregon by August. The old-timers smile.",
        "\"Buy all the food you can at Fort Hall,\" advises a wagon captain.",
        "\"There is good grass past Chimney Rock,\" says a scout. \"Rest your teams there.\"",
        "A mountain man trades tall tales for coffee and tells you Jim Bridger's best lies.",
        "A soldier at the fort grumbles about pay, whiskey and the long ride home.",
        "\"Cholera took half our company,\" says a shaken father. \"Boil your water.\"",
        "A Shoshone woman shows the children how to find camas root.",
        "\"Caulk the wagon and float,\" says a carpenter. \"Fording is how you lose everything.\"",
        "\"We saw a wagon train strung out for a mile,\" says a boy, wide-eyed.",
        "An emigrant reads from her diary: \"Rain again. The road is a river of mud.\"",
        "A preacher holds a Sunday service and blesses the wagons.",
        "\"Independence Rock by the Fourth of July,\" says a captain, \"or you'll winter in the mountains.\""
    )

    /** Occasional useful rumors, tied to gameplay. */
    val rumors: List<String> = listOf(
        "\"The river ahead is running high. Be careful.\"",
        "\"Bandits have been seen near the next ford.\"",
        "\"There's a ferry at the crossing, but it costs dear.\"",
        "\"Rest your oxen before the mountains. You'll thank me.\""
    )
}

/** Starting months. Leaving late means winter in the mountains. */
enum class TravelMonth(val displayName: String, val monthIndex: Int) {
    MARCH("March", 3),
    APRIL("April", 4),
    MAY("May", 5),
    JUNE("June", 6),
    JULY("July", 7);

    companion object {
        val seasonByIndex: Map<Int, TravelMonth> = entries.associateBy { it.monthIndex }
    }
}

/** How hard the party pushes each day. */
enum class Pace(val displayName: String, val milesPerDay: Int, val wear: Double) {
    STEADY("Steady", 14, 1.0),
    STRENUOUS("Strenuous", 19, 1.3),
    GRUELING("Grueling", 24, 1.8)
}

/** How much the party eats each day. */
enum class Rations(val displayName: String, val poundsPerPersonPerDay: Int) {
    FILLING("Filling", 3),
    MEAGER("Meager", 2),
    BARE_BONES("Bare bones", 1)
}

/** Broad health buckets used for display. */
enum class HealthState(val displayName: String) {
    GOOD("good"),
    FAIR("fair"),
    POOR("poor"),
    VERY_POOR("very poor"),
    DEAD("dead");

    companion object {
        fun fromHealth(health: Int): HealthState = when {
            health <= 0 -> DEAD
            health < 25 -> VERY_POOR
            health < 50 -> POOR
            health < 75 -> FAIR
            else -> GOOD
        }
    }
}

/** Wagon spare parts that can break. */
enum class Part { WHEEL, AXLE, TONGUE }

/** Everything that can be bought or used. */
enum class Item(
    val label: String,
    val unit: String,
    val basePrice: Double,
    val fortPrice: Double,
    val step: Int,
    val max: Int
) {
    OXEN("Oxen", "yoke", 20.0, 25.0, 1, 12),
    FOOD("Food", "pound", 0.20, 0.25, 50, 3000),
    CLOTHING("Clothing", "set", 10.0, 12.5, 1, 99),
    AMMUNITION("Ammunition", "box", 2.0, 2.5, 1, 99),
    WHEEL("Spare wheel", "wheel", 10.0, 12.5, 1, 9),
    AXLE("Spare axle", "axle", 10.0, 12.5, 1, 9),
    TONGUE("Spare tongue", "tongue", 10.0, 12.5, 1, 9)
}

enum class LandmarkKind { START, RIVER, FORT, LANDMARK, MOUNTAINS, END }

/** A river crossing. */
data class River(
    val widthYards: Int,
    val ferryCost: Double?,
    val guideCost: Double?,
    val canFord: Boolean = true,
    val canCaulk: Boolean = true
)

/** A point along the trail. */
data class Landmark(
    val id: String,
    val name: String,
    val mile: Int,
    val kind: LandmarkKind,
    val blurb: List<String>,
    val river: River? = null,
    val cutoffId: String? = null,
    val cutoffLabel: String? = null,
    val cutoffMiles: Int = 0
)

/** Static game data, kept in one place so the trail is easy to extend. */
object Data {

    const val TOTAL_MILES = 2040

    val landmarks: List<Landmark> = listOf(
        Landmark(
            "independence", "Independence, Missouri", 0, LandmarkKind.START,
            listOf(
                "You are in Independence, Missouri. The jumping-off",
                "point for the Oregon Trail. It is a busy town full",
                "of emigrants buying supplies and hitching up."
            )
        ),
        Landmark(
            "kansas", "Kansas River Crossing", 102, LandmarkKind.RIVER,
            listOf(
                "The Kansas River is broad and muddy. There is a",
                "ferry here, and the water is usually shallow enough",
                "to ford if you are careful."
            ),
            river = River(620, 7.0, null)
        ),
        Landmark(
            "bigblue", "Big Blue River Crossing", 185, LandmarkKind.RIVER,
            listOf(
                "The Big Blue River is deep and fast this time of",
                "year. Many wagons have been swamped here."
            ),
            river = River(240, 5.0, 5.0)
        ),
        Landmark(
            "kearney", "Fort Kearney", 304, LandmarkKind.FORT,
            listOf(
                "Fort Kearney is the first Army post on the trail.",
                "There is a post office, a blacksmith, and a store",
                "where supplies cost more than in Independence."
            )
        ),
        Landmark(
            "chimney", "Chimney Rock", 554, LandmarkKind.LANDMARK,
            listOf(
                "Chimney Rock rises 500 feet out of the plains. It is",
                "the most famous landmark on the trail. Weary",
                "travelers carve their names at its base."
            )
        ),
        Landmark(
            "laramie", "Fort Laramie", 640, LandmarkKind.FORT,
            listOf(
                "Old Fort Laramie is a bustling trading post. Here",
                "you can rest, repair wagons, and buy supplies.",
                "Prices are higher than back in Missouri."
            )
        ),
        Landmark(
            "independence_rock", "Independence Rock", 830, LandmarkKind.LANDMARK,
            listOf(
                "Independence Rock is a huge rounded granite dome.",
                "Emigrants try to reach it by the Fourth of July.",
                "Names are painted and carved all over its face."
            )
        ),
        Landmark(
            "southpass", "South Pass", 932, LandmarkKind.MOUNTAINS,
            listOf(
                "South Pass is the great doorway through the Rocky",
                "Mountains. The grade is gentle and wagons can",
                "roll through, if the weather holds."
            )
        ),
        Landmark(
            "green", "Green River Crossing", 1151, LandmarkKind.RIVER,
            listOf(
                "The Green River is wide, cold, and swift. A ferry",
                "and a trading post operate here, and a Shoshone",
                "guide can lead you across for a fee."
            ),
            river = River(400, 8.0, 10.0)
        ),
        Landmark(
            "bridger", "Fort Bridger", 1295, LandmarkKind.FORT,
            listOf(
                "Fort Bridger is a small trading post of rough log",
                "cabins. Jim Bridger sells supplies and spare parts.",
                "A cutoff here can save miles but skips a fort."
            ),
            cutoffId = "hall", cutoffLabel = "Take the Lander cutoff", cutoffMiles = 35
        ),
        Landmark(
            "soda", "Soda Springs", 1395, LandmarkKind.LANDMARK,
            listOf(
                "Soda Springs bubbles up naturally from the ground.",
                "The water tastes of soda and is a welcome change.",
                "A cutoff west begins here."
            ),
            cutoffId = "snake", cutoffLabel = "Take the Sublette cutoff", cutoffMiles = 50
        ),
        Landmark(
            "hall", "Fort Hall", 1534, LandmarkKind.FORT,
            listOf(
                "Fort Hall is the last fort before the Snake River",
                "country. Stock up well here; supplies grow scarce",
                "and expensive from now on."
            )
        ),
        Landmark(
            "snake", "Snake River Crossing", 1698, LandmarkKind.RIVER,
            listOf(
                "The Snake River runs fast through black lava rocks.",
                "Many emigrants hire a guide or float their wagons",
                "across rather than risk a ford."
            ),
            river = River(1000, 10.0, 8.0)
        ),
        Landmark(
            "boise", "Fort Boise", 1724, LandmarkKind.FORT,
            listOf(
                "Fort Boise is a Hudson's Bay Company post. It is a",
                "lonely place, but a welcome one after the Snake.",
                "Supplies are costly but necessary."
            )
        ),
        Landmark(
            "bluemountains", "Blue Mountains", 1851, LandmarkKind.MOUNTAINS,
            listOf(
                "The Blue Mountains rise in a maze of timbered",
                "ridges. The road is steep, muddy, and easy to",
                "lose. Snow comes early here."
            )
        ),
        Landmark(
            "wallawalla", "Fort Walla Walla", 1933, LandmarkKind.FORT,
            listOf(
                "Fort Walla Walla is a fur-trading post on the",
                "Columbia River. From here the trail descends to",
                "the Dalles and the last great barrier."
            )
        ),
        Landmark(
            "dalles", "The Dalles", 2040, LandmarkKind.LANDMARK,
            listOf(
                "At The Dalles the Columbia River roars through the",
                "Cascade Mountains. You may pay a toll to take the",
                "Barlow Road, or raft the dangerous river."
            )
        ),
        Landmark(
            "willamette", "Willamette Valley", 2040, LandmarkKind.END,
            listOf(
                "You have reached the green Willamette Valley.",
                "The journey is over. A new life begins."
            )
        )
    )

    fun landmarkAt(index: Int): Landmark = landmarks[index.coerceIn(0, landmarks.lastIndex)]

    fun indexOf(id: String): Int = landmarks.indexOfFirst { it.id == id }.coerceAtLeast(0)

    val firstNames: List<String> = listOf(
        "Andrew", "Elizabeth", "Mary", "James", "Sarah", "John", "Martha", "William",
        "Rebecca", "Henry", "Hannah", "Thomas", "Nancy", "Samuel", "Emily", "Daniel",
        "Rachel", "Joseph", "Abigail", "David", "Lucy", "Jacob", "Anna", "Elias",
        "Charlotte", "George", "Susan", "Peter", "Jane", "Moses", "Ellen", "Amos"
    )

    val surnames: List<String> = listOf(
        "Whitman", "Meeker", "Applegate", "Barlow", "Hastings", "Sager", "Donner",
        "Bidwell", "Frémont", "Carson", "Bridger", "Sublette", "Lovejoy", "Nesmith",
        "Burnett", "Champoeg", "Kesley", "Palmer", "Smith", "Miller", "Johnson"
    )

    /** Fake-but-plausible pioneers used to seed the top-ten table. */
    val topTenSeed: List<Pair<String, Int>> = listOf(
        "Ezra Meeker" to 6904,
        "Marcus Whitman" to 6555,
        "Narcissa Whitman" to 6054,
        "Jesse Applegate" to 5890,
        "Tabitha Brown" to 5422,
        "John McLoughlin" to 5100,
        "Samuel Barlow" to 4848,
        "Mary Walker" to 4310,
        "Joseph Meek" to 3925,
        "Rebecca Applegate" to 3400
    )

    /** Diseases/conditions a traveler can contract. */
    val illnesses: List<String> = listOf(
        "dysentery", "cholera", "typhoid fever", "measles",
        "exhaustion", "a broken arm", "a broken leg", "a snakebite",
        "a fever", "dysentery", "cholera", "a cold"
    )
}
