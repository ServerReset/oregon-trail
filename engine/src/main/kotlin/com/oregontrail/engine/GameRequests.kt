package com.oregontrail.engine


fun Game.setName(index: Int, name: String) {
        if (index in party.indices) {
            val trimmed = name.trim().take(12)
            if (trimmed.isNotEmpty()) party[index].name = trimmed
        }
        requestedNameEdit = null
    }

    /** Called by the front-end after a text dialog closes. */
fun Game.clearNameRequest() {
        requestedNameEdit = null
    }

    /** Sets the gravestone epitaph typed by the player and clears the request. */
fun Game.setEpitaph(text: String) {
        val trimmed = text.trim().take(140)
        if (trimmed.isNotEmpty()) {
            lastGravestone = trimmed
            scores.saveGravestone(trimmed)
            if (graves.isNotEmpty()) {
                graves[graves.size - 1] = graves.last().copy(text = trimmed)
            }
        }
        requestedEpitaphEdit = false
    }

fun Game.clearEpitaphRequest() {
        requestedEpitaphEdit = false
    }

fun Game.clearLoadRequest() {
        requestedLoadId = null
    }

fun Game.clearDeleteRequest() {
        requestedDeleteId = null
    }

fun Game.clearRenameRequest() {
        requestedRenameId = null
    }

fun Game.clearOverwriteRequest() {
        requestedOverwriteId = null
    }

fun Game.clearSaveRequest() {
        requestedSave = false
    }

    /** Called by the front-end once it has loaded (or failed to load) the autosave. */
fun Game.clearAutosaveRequest() {
        requestedAutosaveLoad = false
    }

fun Game.clearShareRequest() {
        requestedShare = false
    }

fun Game.clearQuickSaveRequest() {
        requestedQuickSave = false
    }

fun Game.clearQuickLoadRequest() {
        requestedQuickLoad = false
    }

fun Game.clearJournalShareRequest() {
    requestedJournalShare = false
}
