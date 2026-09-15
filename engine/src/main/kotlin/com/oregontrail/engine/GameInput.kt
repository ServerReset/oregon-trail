package com.oregontrail.engine

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

fun Game.onTap(id: String) {
        pendingSound = Sound.CLICK
        when {
            id == "title:travel" -> { phase = Phase.PROFESSION }
            id == "title:daily" -> startDailyChallenge()
            id == "title:about" -> { aboutPage = 0; phase = Phase.ABOUT }
            id == "title:topten" -> phase = Phase.TOP_TEN
            id == "title:continue" -> requestedAutosaveLoad = true
            id == "title:load" -> { loadPage = 0; phase = Phase.LOAD }
            id == "title:ach" -> phase = Phase.ACHIEVEMENTS
            id == "title:stats" -> phase = Phase.STATS
            id == "title:manage" -> { managementReturn = Phase.TITLE; phase = Phase.MANAGEMENT }
            id == "slots:back" -> { phase = loadReturn; loadReturn = Phase.TITLE }
            id == "ach:back" -> phase = Phase.TITLE
            id == "stats:back" -> phase = Phase.TITLE
            id.startsWith("slot:load:") -> requestedLoadId = id.substringAfter("slot:load:")
            id.startsWith("slot:del:") -> requestedDeleteId = id.substringAfter("slot:del:")
            id.startsWith("slot:rename:") -> requestedRenameId = id.substringAfter("slot:rename:")
            id.startsWith("slot:overwrite:") -> requestedOverwriteId = id.substringAfter("slot:overwrite:")
            id == "slots:prev" -> { loadPage = (loadPage - 1).coerceAtLeast(0); pendingSound = Sound.PAGE }
            id == "slots:next" -> { loadPage++; pendingSound = Sound.PAGE }
            id == "title:end" -> { /* handled by front-end by finishing activity */ }
            id == "about:next" -> {
                aboutPage++
                pendingSound = Sound.PAGE
                if (aboutPage >= Game.ABOUT_PAGES.size) phase = Phase.TITLE
            }
            id == "manage:topten" -> phase = Phase.TOP_TEN
            id == "manage:newleader" -> { newRun(occupation, travelMonth); phase = Phase.PROFESSION }
            id == "manage:sound" -> soundEnabled = !soundEnabled
            id == "manage:haptics" -> uiSettings?.let { it.haptics = !it.haptics }
            id == "manage:difficulty" -> cycleDifficulty()
            id == "manage:textsize" -> uiSettings?.let { it.textScaleIndex = (it.textScaleIndex + 1) % 3 }
            id == "manage:contrast" -> uiSettings?.let { it.highContrast = !it.highContrast }
            id == "manage:scanlines" -> uiSettings?.let { it.scanlines = !it.scanlines }
            id == "manage:theme" -> uiSettings?.let { it.themeIndex = (it.themeIndex + 1) % 3 }
            id == "manage:export" -> { /* handled by the front-end (file export) */ }
            id == "manage:import" -> { /* handled by the front-end (file import) */ }
            id == "manage:back" -> phase = managementReturn
            id == "pause:open" -> openPause()
            id == "pause:resume" -> phase = pauseReturn
            id == "pause:save" -> requestedSave = true
            id == "pause:quicksave" -> requestedQuickSave = true
            id == "pause:quickload" -> requestedQuickLoad = true
            id == "pause:load" -> { loadPage = 0; loadReturn = Phase.PAUSE; phase = Phase.LOAD }
            id == "pause:manage" -> { managementReturn = Phase.PAUSE; phase = Phase.MANAGEMENT }
            id == "pause:title" -> phase = Phase.TITLE
            id == "pause:quit" -> { /* handled by the front-end by finishing the activity */ }
            id == "topten:back" -> phase = Phase.TITLE
            id.startsWith("prof:") -> {
                occupation = Occupation.entries[id.substringAfter("prof:").toInt()]
                phase = Phase.MONTH
            }
            id.startsWith("month:") -> {
                travelMonth = TravelMonth.entries[id.substringAfter("month:").toInt()]
                date = GameDate(1848, travelMonth.monthIndex, 1)
                phase = Phase.NAMES
            }
            id.startsWith("name:") -> requestedNameEdit = id.substringAfter("name:").toInt()
            id == "names:go" -> {
                if (inventory.cash <= 0) { newRun(occupation, travelMonth); phase = Phase.PROFESSION }
                else { storeAtFort = false; phase = Phase.STORE }
            }
            id == "store:leave" -> leaveStore()
            id.startsWith("store:inc:") -> changeItem(id.substringAfterLast(":"), 1)
            id.startsWith("store:dec:") -> changeItem(id.substringAfterLast(":"), -1)
            id.startsWith("travel:") -> handleTravelMenu(id.substringAfter("travel:"))
            id.startsWith("land:") -> handleLandmarkMenu(id.substringAfter("land:"))
            id.startsWith("river:") -> handleRiver(id.substringAfter("river:"))
            id.startsWith("choice:") -> handleChoice(id.substringAfter("choice:"))
            id.startsWith("riders:") || id.startsWith("trade:") || id.startsWith("rest:") ->
                handleChoice(id)
            id.startsWith("stranded:") -> handleStranded(id.substringAfter("stranded:"))
            id == "notice:continue" -> phase = noticeNext
            id == "map:back" -> phase = Phase.TRAVEL
            id == "journal:prev" -> { journalPage = (journalPage - 1).coerceAtLeast(0); pendingSound = Sound.PAGE }
            id == "journal:next" -> { journalPage = min(journalPage + 1, journalLastPage()); pendingSound = Sound.PAGE }
            id == "journal:back" -> phase = Phase.TRAVEL
            id == "hunt:up" -> huntField?.move(0, -1)
            id == "hunt:down" -> huntField?.move(0, 1)
            id == "hunt:left" -> huntField?.move(-1, 0)
            id == "hunt:right" -> huntField?.move(1, 0)
            id == "hunt:shoot" -> huntShoot()
            id == "hunt:leave" -> endHunt()
            id == "raft:left" -> raftField?.moveLeft()
            id == "raft:right" -> raftField?.moveRight()
            id == "barlow:left" -> barlowField?.moveLeft()
            id == "barlow:right" -> barlowField?.moveRight()
            id == "death:topten" -> phase = Phase.TOP_TEN
            id == "death:epitaph" -> requestedEpitaphEdit = true
            id == "death:restart" -> { newRun(occupation, travelMonth); phase = Phase.PROFESSION }
            id == "arrived:topten" -> phase = Phase.TOP_TEN
            id == "arrived:epilogue" -> phase = Phase.EPILOGUE
            id == "epilogue:back" -> phase = Phase.ARRIVED
            id == "arrived:share" || id == "death:share" -> requestedShare = true
            id == "arrived:restart" -> { newRun(occupation, travelMonth); phase = Phase.PROFESSION }
            id.startsWith("dalles:") -> handleDalles(id.substringAfter("dalles:"))
        }
    }
