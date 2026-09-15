package com.oregontrail.engine

/** The first half of the trail, from Independence to the Green River. */
internal object LandmarkDataEast {
    val list: List<Landmark> = listOf(
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
    )
}
