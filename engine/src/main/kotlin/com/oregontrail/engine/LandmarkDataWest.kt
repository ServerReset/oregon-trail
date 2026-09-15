package com.oregontrail.engine

/** The second half of the trail, from Fort Bridger to the Willamette Valley. */
internal object LandmarkDataWest {
    val list: List<Landmark> = listOf(
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
}
