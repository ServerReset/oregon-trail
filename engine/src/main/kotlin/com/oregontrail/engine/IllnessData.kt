package com.oregontrail.engine

/** Diseases and conditions a traveler can contract (some weighted by repeats). */
internal object IllnessData {
    val list: List<String> = listOf(
        "dysentery", "cholera", "typhoid fever", "measles",
        "exhaustion", "a broken arm", "a broken leg", "a snakebite",
        "a fever", "dysentery", "cholera", "a cold"
    )
}
