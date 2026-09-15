package com.oregontrail.engine

object Facts {
    val byLandmark: Map<String, String> = mapOf(
        "independence" to "Independence was the main jumping-off point; by 1849 over 30,000 emigrants a year passed through.",
        "kansas" to "Most travelers crossed the Kansas River by ferry, paying a few dollars rather than risk a ford.",
        "bigblue" to "The Big Blue was notorious for spring floods that stranded whole wagon trains for days.",
        "kearney" to "Fort Kearney was established in 1848 to protect travelers on the Platte River road.",
        "chimney" to "Chimney Rock is a spire of clay and sandstone that once stood over 100 feet tall.",
        "laramie" to "Fort Laramie began as a fur-trading post and became the great supply point of the high plains.",
        "independence_rock" to "Independence Rock is a granite dome 1,900 feet long; reaching it by July 4 was a rite of passage.",
        "southpass" to "South Pass is a broad, gentle gap in the Rockies that wagons could cross without ropes.",
        "green" to "The Green River was the major obstacle of the Rockies; ferries and guides profited here.",
        "bridger" to "Jim Bridger's fort was famous for tall tales and expensive supplies.",
        "soda" to "Soda Springs still bubbles naturally; emigrants wrote it tasted like the new 'soda water'.",
        "hall" to "Fort Hall was a Hudson's Bay Company post and the last real store before the desert.",
        "snake" to "The Snake River's rapids drowned many; some travelers floated their wagons downstream.",
        "boise" to "Fort Boise was a small, remote post on the way to the Blue Mountains.",
        "bluemountains" to "The Blue Mountains were steep, wooded and muddy, and snow could close the road early.",
        "wallawalla" to "Fort Walla Walla stood where the Walla Walla River meets the Columbia.",
        "dalles" to "At The Dalles the Columbia squeezed through the Cascades; many rafted or took the Barlow toll road."
    )

    fun forLandmark(id: String): String? = byLandmark[id]
}

/** Things people say when you talk to them on the trail. */
object Talk {
    /** Flavor lines. */
    val lines: List<String> = listOf(
        "An old trapper warns: \"Keep to the high ground and watch for alkali water.\"",
        "\"The Snake River is fearsome this year,\" says a settler. \"Hire a guide.\"",
        "A missionary family shares a meal and news from the Willamette.",
        "\"We buried two on the plains,\" says a widow quietly. \"Take your time.\"",
        "A young man boasts he will be in Oregon by August. The old-timers smile.",
        "\"Buy all the food you can at Fort Hall,\" advises a wagon captain.",
        "\"There is good grass past Chimney Rock,\" says a scout. \"Rest your teams there.\"",
        "A mountain man trades tall tales for coffee and tells you Jim Bridger's best lies.",
        "A soldier at the fort grumbles about pay, whiskey and the long ride home.",
        "\"Cholera took half our company,\" says a shaken father. \"Boil your water.\"",
        "A Shoshone woman shows the children how to find camas root.",
        "\"Caulk the wagon and float,\" says a carpenter. \"Fording is how you lose everything.\"",
        "\"We saw a wagon train strung out for a mile,\" says a boy, wide-eyed.",
        "An emigrant reads from her diary: \"Rain again. The road is a river of mud.\"",
        "A preacher holds a Sunday service and blesses the wagons.",
        "\"Independence Rock by the Fourth of July,\" says a captain, \"or you'll winter in the mountains.\"",
        "A blacksmith sharpens plow blades for a dollar and tells you to grease your axles.",
        "\"Mind the dust storms,\" says a teamster. \"They'll steal the breath from you.\"",
        "Two children play at yoking a dog to a handcart while their mother laughs.",
        "\"I traded my watch for a sack of flour,\" admits a banker, shamefaced.",
        "A grandmother stitches by the fire and hums a tune from Ohio.",
        "\"The mountains are beautiful and they will kill you,\" says a quiet old man.",
        "A fiddler strikes up a reel and for one evening the whole camp forgets the trail.",
        "\"We saw a herd of antelope a mile wide,\" says a hunter, shaking his head.",
        "A boy trades a marble for a biscuit and calls it the best deal of his life.",
        "\"Write it down,\" urges a schoolteacher. \"Someone should remember all this.\""
    )

    /** Occasional useful rumors, tied to gameplay. */
    val rumors: List<String> = listOf(
        "\"The river ahead is running high. Be careful.\"",
        "\"Bandits have been seen near the next ford.\"",
        "\"There's a ferry at the crossing, but it costs dear.\"",
        "\"Rest your oxen before the mountains. You'll thank me.\"",
        "\"Wild berries grow along the streams past the next bluff.\"",
        "\"We found a spare wheel in an abandoned wagon. Look for one.\"",
        "\"A warm spring lies off the road. It'll ease aching bones.\"",
        "\"Buy clothing before the passes. Cold takes the poorly dressed.\"",
        "\"Firewood is scarce past the mountains. Gather it early.\"",
        "\"Keep a lantern lit for stragglers. The trail is dark at night.\""
    )
}

/** Occasional trail wisdom offered when the party rests. */
object TrailTips {
    val list: List<String> = listOf(
        "Boil your drinking water. Cholera lurks in the clearest stream.",
        "Grease the wheel hubs at every river. Dry axles split.",
        "Feed the oxen before yourself. They pull the wagon.",
        "Start early. The afternoon sun is a hammer on the plains.",
        "Keep the wagons close at night. Stragglers are easy prey.",
        "Swap news with every train you meet. Word travels faster than wheels.",
        "Do not ford a rising river. An hour's wait beats a lost wagon.",
        "Dry your clothes when the sun breaks. Damp bedding breeds fever.",
        "Mark each grave you pass, and say the name aloud.",
        "Mend the harness in camp, not on the road."
    )
}

/** Starting months. Leaving late means winter in the mountains. */
