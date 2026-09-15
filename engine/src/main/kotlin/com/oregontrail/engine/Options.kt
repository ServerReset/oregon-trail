package com.oregontrail.engine

enum class TravelMonth(val displayName: String, val monthIndex: Int) {
    MARCH("March", 3),
    APRIL("April", 4),
    MAY("May", 5),
    JUNE("June", 6),
    JULY("July", 7);

}

/** How hard the party pushes each day. */
enum class Pace(val displayName: String, val milesPerDay: Int, val wear: Double) {
    STEADY("Steady", 19, 1.0),
    STRENUOUS("Strenuous", 25, 1.3),
    GRUELING("Grueling", 32, 1.8)
}

/** How much the party eats each day. */
enum class Rations(val displayName: String, val poundsPerPersonPerDay: Int) {
    FILLING("Filling", 3),
    MEAGER("Meager", 2),
    BARE_BONES("Bare bones", 1)
}

/** Broad health buckets used for display. */
