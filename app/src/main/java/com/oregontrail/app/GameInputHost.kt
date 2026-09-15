package com.oregontrail.app

import android.widget.Toast
import com.oregontrail.engine.*

internal fun MainActivity.onHotspot(id: String) {
    if (id == "title:end" || id == "pause:quit") {
        finish()
        return
    }
    // Save the current position before leaving the pause menu to the title.
    if (id == "pause:title") store.saveState(game.save())
    game.onTap(id)
    if (game.requestedShare) share.sharePostcard()
    if (game.requestedJournalShare) {
        share.shareText(game.journalText(), "My Oregon Trail journal")
        game.clearJournalShareRequest()
    }
    if (id == "manage:export") saves.export()
    if (id == "manage:import") saves.import()
    handleNameRequest()
    handleEpitaphRequest()
    handleAutosaveRequest()
    handleQuickSave()
    handleQuickLoad()
    handleSaveRequest()
    handleLoadRequest()
    handleRenameRequest()
    handleOverwriteRequest()
    handleDeleteRequest()
    refreshSlots()
    game.pendingUnlock?.let { name ->
        Toast.makeText(this, "Achievement unlocked: $name", Toast.LENGTH_LONG).show()
        game.pendingUnlock = null
    }
    applyUi()
    render()
    handler.removeCallbacks(animator)
    handler.post(animator)
}

