package com.oregontrail.engine

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
