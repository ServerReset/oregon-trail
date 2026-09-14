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
}

/** One dated line in the traveler's journal. */
data class JournalEntry(val date: String, val text: String)

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
