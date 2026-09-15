package com.oregontrail.engine

enum class WeatherKind(val displayName: String) {
    CLEAR("clear"),
    CLOUDY("cloudy"),
    RAIN("rainy"),
    HEAVY_RAIN("heavy rain"),
    THUNDERSTORM("thunderstorms"),
    SNOW("snowy"),
    BLIZZARD("blizzard"),
    FOG("foggy"),
    HOT("very hot"),
    COLD("very cold"),
    WINDY("windy"),
    HAIL("hailstorms")
}

data class Weather(val kind: WeatherKind, val tempF: Int) {
    val description: String
        get() = "${kind.displayName}, ${tempF}\u00B0F"
}

/** One member of the traveling party. */
