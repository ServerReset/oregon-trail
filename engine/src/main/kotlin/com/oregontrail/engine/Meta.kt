package com.oregontrail.engine

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
    const val RAINBOW = "rainbow"
    const val POSTMASTER = "postmaster"
    const val TRAILBLAZER = "trailblazer"

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
        Achievement(BARGAINER, "Wheeler-Dealer", "Haggle a trader into a better deal."),
        Achievement(RAINBOW, "Over the Rainbow", "See a rainbow on the trail."),
        Achievement(POSTMASTER, "Postmaster", "Send a postcard from the trail."),
        Achievement(TRAILBLAZER, "Trailblazer", "Reach ten landmarks in a single journey.")
    )

    fun name(id: String): String = all.firstOrNull { it.id == id }?.name ?: id
}

/** Historical notes shown at landmarks ("Learn the history"). */
