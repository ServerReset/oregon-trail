package com.oregontrail.engine


    /** Serializes the whole run to a compact, line-based string. */
fun Game.save(): String {
        val sb = StringBuilder()
        fun line(k: String, v: Any) {
            sb.append(k).append('=').append(v.toString().replace('\n', ' ')).append('\n')
        }
        line("v", 1)
        line("occ", occupation.name)
        line("month", travelMonth.name)
        line("date", "${date.year},${date.month},${date.day}")
        line("weather", "${weather.kind.name},${weather.tempF}")
        line("miles", miles)
        line("landmark", landmarkIndex)
        line("cutoff", cutoffTarget)
        line("pace", pace.name)
        line("rations", rations.name)
        line("oxHealth", oxHealth)
        line("difficulty", difficulty.name)
        line("storeAtFort", storeAtFort)
        line("sound", soundEnabled)
        line("cash", inventory.cash)
        line("oxen", inventory.oxen)
        line("food", inventory.food)
        line("clothing", inventory.clothing)
        line("ammo", inventory.ammo)
        line("wheels", inventory.wheels)
        line("axles", inventory.axles)
        line("tongues", inventory.tongues)
        line("phase", safePhase().name)
        party.forEachIndexed { i, m ->
            line("p$i", "${enc(m.name)},${m.health},${m.alive},${enc(m.condition ?: "")}")
        }
        journal.forEachIndexed { i, e ->
            line("j$i", "${enc(e.date)}|${enc(e.text)}")
        }
        return sb.toString()
    }

    /** Restores a run from [data]. Returns false if the data is unusable. */
fun Game.load(data: String): Boolean {
        val map = HashMap<String, String>()
        for (l in data.split('\n')) {
            val i = l.indexOf('=')
            if (i > 0) map[l.substring(0, i)] = l.substring(i + 1)
        }
        if (map["v"] != "1") return false
        return try {
            occupation = Occupation.valueOf(map["occ"] ?: return false)
            travelMonth = TravelMonth.valueOf(map["month"] ?: return false)
            val d = (map["date"] ?: return false).split(',')
            date = GameDate(d[0].toInt(), d[1].toInt(), d[2].toInt())
            val w = (map["weather"] ?: return false).split(',')
            weather = Weather(WeatherKind.valueOf(w[0]), w[1].toInt())
            miles = map["miles"]?.toIntOrNull() ?: 0
            landmarkIndex = (map["landmark"]?.toIntOrNull() ?: 0).coerceIn(0, Data.landmarks.lastIndex)
            cutoffTarget = (map["cutoff"]?.toIntOrNull() ?: -1)
                .coerceIn(-1, Data.landmarks.lastIndex)
            pace = Pace.valueOf(map["pace"] ?: pace.name)
            rations = Rations.valueOf(map["rations"] ?: rations.name)
            oxHealth = (map["oxHealth"]?.toIntOrNull() ?: 100).coerceIn(0, 100)
            difficulty = try {
                Difficulty.valueOf(map["difficulty"] ?: "NORMAL")
            } catch (_: Exception) {
                Difficulty.NORMAL
            }
            storeAtFort = map["storeAtFort"]?.toBooleanStrictOrNull() ?: false
            soundEnabled = map["sound"]?.toBooleanStrictOrNull() ?: true
            inventory.cash = map["cash"]?.toDoubleOrNull() ?: 0.0
            inventory.oxen = map["oxen"]?.toIntOrNull() ?: 0
            inventory.food = map["food"]?.toIntOrNull() ?: 0
            inventory.clothing = map["clothing"]?.toIntOrNull() ?: 0
            inventory.ammo = map["ammo"]?.toIntOrNull() ?: 0
            inventory.wheels = map["wheels"]?.toIntOrNull() ?: 0
            inventory.axles = map["axles"]?.toIntOrNull() ?: 0
            inventory.tongues = map["tongues"]?.toIntOrNull() ?: 0
            val saved = ArrayList<PartyMember>()
            for (i in 0 until 5) {
                val raw = map["p$i"] ?: return false
                val parts = raw.split(',')
                saved.add(
                    PartyMember(
                        dec(parts[0]),
                        parts[1].toIntOrNull() ?: 100,
                        parts[2].toBooleanStrictOrNull() ?: true,
                        dec(parts.getOrElse(3) { "" }).ifEmpty { null }
                    )
                )
            }
            party = saved
            journal.clear()
            var ji = 0
            while (true) {
                val raw = map["j$ji"] ?: break
                val parts = raw.split('|')
                journal.add(JournalEntry(dec(parts[0]), dec(parts.getOrElse(1) { "" })))
                ji++
            }
            phase = try {
                Phase.valueOf(map["phase"] ?: "TRAVEL")
            } catch (_: Exception) {
                Phase.TRAVEL
            }
            if (phase in Game.INVALID_RESUME_PHASES) phase = Phase.TRAVEL
            true
        } catch (_: Exception) {
            false
        }
    }

internal fun Game.safePhase(): Phase = when {
        phase == Phase.PAUSE -> if (pauseReturn in Game.PAUSABLE_PHASES) pauseReturn else Phase.TRAVEL
        phase == Phase.RAFTING || phase == Phase.BARLOW -> Phase.LANDMARK
        phase in Game.INVALID_RESUME_PHASES -> Phase.TRAVEL
        else -> phase
    }

internal fun Game.enc(s: String): String =
        s.replace('\\', '/').replace(',', ';').replace('|', '/')
            .replace('\n', ' ').replace('\r', ' ')

internal fun Game.dec(s: String): String = s
