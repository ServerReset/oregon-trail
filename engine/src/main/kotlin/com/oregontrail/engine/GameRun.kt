package com.oregontrail.engine


internal fun Game.newRun(occ: Occupation, month: TravelMonth) {
        occupation = occ
        travelMonth = month
        inventory.cash = occ.startingMoney.toDouble()
        inventory.oxen = 0
        inventory.food = 0
        inventory.clothing = 0
        inventory.ammo = 0
        inventory.wheels = 0
        inventory.axles = 0
        inventory.tongues = 0
        date = GameDate(1848, month.monthIndex, 1)
        weather = Weather(WeatherKind.CLEAR, 60)
        miles = 0
        landmarkIndex = 0
        cutoffTarget = -1
        pace = Pace.STEADY
        rations = Rations.FILLING
        oxHealth = 100
        party = ArrayList()
        val surname = Data.surnames[rng.nextInt(Data.surnames.size)]
        repeat(5) { i ->
            val name = if (i == 0) "Pa $surname" else {
                Data.firstNames[rng.nextInt(Data.firstNames.size)]
            }
            party.add(PartyMember(name))
        }
    }
