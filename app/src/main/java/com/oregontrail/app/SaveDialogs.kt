package com.oregontrail.app

import android.text.InputFilter
import android.widget.EditText
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.oregontrail.engine.*

/** Pseudo-id for the autosave shown in the saved-games list. */
internal const val AUTO_ID = "__auto__"

internal fun MainActivity.handleAutosaveRequest() {
        if (!game.requestedAutosaveLoad) return
        store.loadState()?.let { data -> game.load(data) }
        game.clearAutosaveRequest()
        game.autosaveAvailable = false
    }



internal fun MainActivity.handleQuickSave() {
        if (!game.requestedQuickSave) return
        store.saveState(game.save())
        game.clearQuickSaveRequest()
        game.autosaveAvailable = true
        Toast.makeText(this, "Quick saved", Toast.LENGTH_SHORT).show()
    }



internal fun MainActivity.handleQuickLoad() {
        if (!game.requestedQuickLoad) return
        store.loadState()?.let { data -> game.load(data) }
        game.clearQuickLoadRequest()
        Toast.makeText(this, "Quick loaded", Toast.LENGTH_SHORT).show()
    }



internal fun MainActivity.detailFor(): String = "${game.date}, ${game.miles} mi, ${game.occupation.displayName}"



internal fun MainActivity.handleSaveRequest() {
        if (!game.requestedSave) return
        val suggestion = "${game.party.firstOrNull()?.name ?: "Wagon"} - " +
            "${game.date.monthName} ${game.date.day}"
        val input = EditText(this).apply {
            setText(suggestion)
            setSelection(text.length)
            filters = arrayOf(InputFilter.LengthFilter(24))
            hint = "Save name"
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("Save game")
            .setMessage("Type a name. Using an existing name overwrites that save.")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                saveWithOverwriteCheck(input.text.toString())
            }
            .setNegativeButton("Cancel") { _, _ ->
                game.clearSaveRequest()
                render()
            }
            .setOnCancelListener {
                game.clearSaveRequest()
                render()
            }
            .show()
    }


    /** Saves under [label], asking to overwrite if a slot with that name exists. */

internal fun MainActivity.saveWithOverwriteCheck(label: String) {
        val existing = store.listSlots().firstOrNull { it.label.equals(label.trim(), ignoreCase = true) }
        if (existing == null) {
            store.saveSlot(label, detailFor(), game.save())
            game.clearSaveRequest()
            refreshSlots()
            render()
            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
        } else {
            MaterialAlertDialogBuilder(this)
                .setTitle("Overwrite saved game")
                .setMessage("A save named '${existing.label}' already exists. Replace it?")
                .setPositiveButton("Overwrite") { _, _ ->
                    store.overwriteSlot(existing.id, existing.label, detailFor(), game.save())
                    game.clearSaveRequest()
                    refreshSlots()
                    render()
                    Toast.makeText(this, "Overwritten", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel") { _, _ ->
                    game.clearSaveRequest()
                    render()
                }
                .setOnCancelListener {
                    game.clearSaveRequest()
                    render()
                }
                .show()
        }
    }



internal fun MainActivity.handleOverwriteRequest() {
        val id = game.requestedOverwriteId ?: return
        val slot = game.saveSlots.firstOrNull { it.id == id }
        MaterialAlertDialogBuilder(this)
            .setTitle("Overwrite saved game")
            .setMessage("Replace '${slot?.label ?: "this save"}' with your current journey?")
            .setPositiveButton("Overwrite") { _, _ ->
                if (id == AUTO_ID) store.saveState(game.save())
                else store.overwriteSlot(id, slot?.label ?: "Saved game", detailFor(), game.save())
                game.clearOverwriteRequest()
                refreshSlots()
                render()
                Toast.makeText(this, "Overwritten", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel") { _, _ ->
                game.clearOverwriteRequest()
                render()
            }
            .setOnCancelListener {
                game.clearOverwriteRequest()
                render()
            }
            .show()
    }


    /** Rebuilds the slot list, including the autosave as a "Quick save" entry. */
