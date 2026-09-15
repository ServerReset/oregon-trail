package com.oregontrail.app

import android.text.InputFilter
import android.text.InputType
import android.widget.EditText
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.oregontrail.engine.SaveSlot
import com.oregontrail.engine.*

internal fun MainActivity.handleNameRequest() {
        val index = game.requestedNameEdit ?: return
        if (index !in game.party.indices) {
            game.clearNameRequest()
            return
        }
        val current = game.party[index].name
        val input = EditText(this).apply {
            setText(current)
            setSelection(text.length)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            filters = arrayOf(InputFilter.LengthFilter(12))
            hint = "Name"
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("Name your traveler")
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                game.setName(index, input.text.toString())
                render()
            }
            .setNegativeButton("Cancel") { _, _ ->
                game.clearNameRequest()
                render()
            }
            .setOnCancelListener {
                game.clearNameRequest()
                render()
            }
            .show()
    }


internal fun MainActivity.handleEpitaphRequest() {
        if (!game.requestedEpitaphEdit) return
        val input = EditText(this).apply {
            setText(game.lastGravestone ?: "")
            setSelection(text.length)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            filters = arrayOf(InputFilter.LengthFilter(140))
            hint = "Epitaph"
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("Write an epitaph")
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                game.setEpitaph(input.text.toString())
                render()
            }
            .setNegativeButton("Cancel") { _, _ ->
                game.clearEpitaphRequest()
                render()
            }
            .setOnCancelListener {
                game.clearEpitaphRequest()
                render()
            }
            .show()
    }

