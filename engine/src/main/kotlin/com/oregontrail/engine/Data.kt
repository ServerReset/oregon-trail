package com.oregontrail.engine

/**
 * Static trail data, split into focused files. This object is the facade the
 * rest of the game and the tests use.
 */
object Data {
    const val TOTAL_MILES = 2040

    val landmarks: List<Landmark> = LandmarkDataEast.list + LandmarkDataWest.list

    fun landmarkAt(index: Int): Landmark = landmarks[index.coerceIn(0, landmarks.lastIndex)]

    fun indexOf(id: String): Int = landmarks.indexOfFirst { it.id == id }.coerceAtLeast(0)

    val firstNames: List<String> get() = NameData.firstNames
    val surnames: List<String> get() = NameData.surnames
    val topTenSeed: List<Pair<String, Int>> get() = NameData.topTenSeed
    val illnesses: List<String> get() = IllnessData.list
}
