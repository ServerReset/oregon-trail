package com.oregontrail.engine


internal fun Game.rollWeather() {
        val mountain = Data.landmarkAt(landmarkIndex).kind == LandmarkKind.MOUNTAINS || miles > 1150
        val base = when (date.month) {
            3 -> 52; 4 -> 60; 5 -> 68; 6 -> 76; 7 -> 82; 8 -> 82
            9 -> 72; 10 -> 58; 11 -> 44; else -> 34
        }
        var temp = base + rng.nextInt(-12, 13)
        if (mountain) temp -= 18
        val kind = when {
            temp <= 20 && mountain -> if (rng.chance(0.5)) WeatherKind.BLIZZARD else WeatherKind.SNOW
            temp <= 30 -> if (rng.chance(0.5)) WeatherKind.SNOW else WeatherKind.COLD
            temp >= 90 -> WeatherKind.HOT
            else -> when (rng.nextInt(10)) {
                0, 1 -> WeatherKind.CLEAR
                2, 3 -> WeatherKind.CLOUDY
                4 -> WeatherKind.RAIN
                5 -> WeatherKind.HEAVY_RAIN
                6 -> WeatherKind.THUNDERSTORM
                7 -> WeatherKind.WINDY
                8 -> WeatherKind.HAIL
                else -> WeatherKind.FOG
            }
        }
        weather = Weather(kind, temp)
    }



internal fun Game.weatherSpeedFactor(kind: WeatherKind): Double = when (kind) {
        WeatherKind.CLEAR -> 1.1
        WeatherKind.CLOUDY -> 1.0
        WeatherKind.RAIN -> 0.8
        WeatherKind.HEAVY_RAIN -> 0.6
        WeatherKind.THUNDERSTORM -> 0.5
        WeatherKind.SNOW -> 0.6
        WeatherKind.BLIZZARD -> 0.2
        WeatherKind.FOG -> 0.6
        WeatherKind.HOT -> 0.9
        WeatherKind.COLD -> 0.8
        WeatherKind.WINDY -> 0.9
        WeatherKind.HAIL -> 0.5
    }



internal fun Game.terrainFactor(kind: LandmarkKind): Double = when (kind) {
        LandmarkKind.MOUNTAINS -> 0.82
        LandmarkKind.RIVER -> 1.0
        else -> 1.0
    }

/** Frames in one in-game day (about 45 seconds at the app's tick rate). */
internal const val DAY_FRAMES = 160

/** Where we are in the current day: 0.0 dawn .. 0.5 noon .. 1.0 night. */
internal fun Game.dayPhase(): Double = (frame % DAY_FRAMES).toDouble() / DAY_FRAMES

/** Background mood for fair weather, drifting from day to dusk to night. */
internal fun Game.duskAmbient(): Palette = when {
    dayPhase() > 0.88 -> Palette.BLUE
    dayPhase() < 0.6 -> Palette.GREEN
    else -> Palette.BROWN
}
