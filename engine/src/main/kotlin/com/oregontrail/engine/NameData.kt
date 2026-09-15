package com.oregontrail.engine

/** Pioneer names and the seeded top-ten table. */
internal object NameData {
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
}
