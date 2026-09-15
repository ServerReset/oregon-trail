package com.oregontrail.engine

/**
 * Little stories the party tells around the campfire while they rest. One of
 * the travellers is picked at random and named in the tale.
 */
internal object CampStories {
    val templates: List<String> = listOf(
        "%s tells of the winter the river froze and the wagons crossed on the ice.",
        "%s remembers a fiddle tune from home and hums it by the fire.",
        "%s swears the oxen understand every word spoken to them.",
        "%s describes the first time a buffalo herd darkened the whole prairie.",
        "%s recounts a preacher's sermon about a land of milk and honey.",
        "%s tells a ghost story until the children beg for another.",
        "%s talks of the orchard left behind and the one to be planted.",
        "%s claims to have seen the Pacific Ocean in a dream."
    )

    fun forMember(name: String, rng: Rng): String =
        templates[rng.nextInt(templates.size)].format(name)
}
