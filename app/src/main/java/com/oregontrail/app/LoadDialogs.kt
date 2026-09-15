package com.oregontrail.app

import android.text.InputFilter
import android.widget.EditText
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.oregontrail.engine.*

internal fun MainActivity.refreshSlots() {
        val slots = store.listSlots().toMutableList()
        val auto = store.loadState()
        if (auto != null) {
            slots.removeAll { it.id == AUTO_ID }
            slots.add(
                0,
                SaveSlot(AUTO_ID, "Quick save (auto)", "Resumes exactly where you left off", Long.MAX_VALUE, auto)
            )
        } else {
            slots.removeAll { it.id == AUTO_ID }
        }
        game.saveSlots = slots.sortedByDescending { it.savedAt }
    }



internal fun MainActivity.handleLoadRequest() {
        val id = game.requestedLoadId ?: return
        if (id == AUTO_ID) store.loadState()?.let { data -> game.load(data) }
        else store.loadSlotData(id)?.let { data -> game.load(data) }
        game.clearLoadRequest()
        refreshSlots()
    }



internal fun MainActivity.handleRenameRequest() {
        val id = game.requestedRenameId ?: return
        val current = game.saveSlots.firstOrNull { it.id == id }
        if (current == null || id == AUTO_ID) {
            game.clearRenameRequest()
            return
        }
        val input = EditText(this).apply {
            setText(current.label)
            setSelection(text.length)
            filters = arrayOf(InputFilter.LengthFilter(24))
            hint = "Save name"
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("Rename saved game")
            .setView(input)
            .setPositiveButton("Rename") { _, _ ->
                store.renameSlot(id, input.text.toString())
                game.clearRenameRequest()
                refreshSlots()
                render()
            }
            .setNegativeButton("Cancel") { _, _ ->
                game.clearRenameRequest()
                render()
            }
            .setOnCancelListener {
                game.clearRenameRequest()
                render()
            }
            .show()
    }



internal fun MainActivity.handleDeleteRequest() {
        val id = game.requestedDeleteId ?: return
        val slot = game.saveSlots.firstOrNull { it.id == id }
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete saved game")
            .setMessage(slot?.label ?: "this save")
            .setPositiveButton("Delete") { _, _ ->
                if (id == AUTO_ID) store.clearState() else store.deleteSlot(id)
                refreshSlots()
                game.clearDeleteRequest()
                render()
            }
            .setNegativeButton("Keep") { _, _ ->
                game.clearDeleteRequest()
                render()
            }
            .setOnCancelListener {
                game.clearDeleteRequest()
                render()
            }
            .show()
    }


