package com.oregontrail.app

import android.util.Base64
import com.oregontrail.engine.SaveSlot
import com.oregontrail.engine.SaveBundle

/** Save-slot persistence and the portable export/import bundle. */
internal const val KEY_SLOTS = "save_slots"

    // ----- save slots -------------------------------------------------

fun PrefsScoreStore.listSlots(): List<SaveSlot> {
        val raw = prefs.getString(KEY_SLOTS, null) ?: return emptyList()
        return raw.split('\n').mapNotNull { line ->
            val parts = line.split('|')
            if (parts.size < 5) return@mapNotNull null
            val data = try {
                String(Base64.decode(parts[4], Base64.NO_WRAP), Charsets.UTF_8)
            } catch (_: Exception) {
                return@mapNotNull null
            }
            SaveSlot(parts[0], parts[1], parts[2], parts[3].toLongOrNull() ?: 0L, data)
        }.sortedByDescending { it.savedAt }
    }

fun PrefsScoreStore.saveSlot(label: String, detail: String, data: String): SaveSlot {
        val clean = label.replace('|', '/').replace('\n', ' ').take(24).ifBlank { "Saved game" }
        val cleanDetail = detail.replace('|', '/').replace('\n', ' ').take(48)
        val slot = SaveSlot(
            System.currentTimeMillis().toString(), clean, cleanDetail,
            System.currentTimeMillis(), data
        )
        val slots = listSlots().toMutableList()
        slots.add(slot)
        writeSlots(slots)
        return slot
    }

fun PrefsScoreStore.deleteSlot(id: String) {
        writeSlots(listSlots().filterNot { it.id == id })
    }

fun PrefsScoreStore.renameSlot(id: String, newLabel: String) {
        val clean = newLabel.replace('|', '/').replace('\n', ' ').take(24)
        if (clean.isBlank()) return
        writeSlots(listSlots().map { if (it.id == id) it.copy(label = clean) else it })
    }

    /** Replaces a slot's contents with the current game (used by "save over"). */
fun PrefsScoreStore.overwriteSlot(id: String, label: String, detail: String, data: String) {
        val clean = label.replace('|', '/').replace('\n', ' ').take(24).ifBlank { "Saved game" }
        val slots = listSlots().map {
            if (it.id == id) {
                SaveSlot(it.id, clean, detail.replace('|', '/').take(48), System.currentTimeMillis(), data)
            } else it
        }
        writeSlots(slots)
    }

    // ----- export / import -------------------------------------------

fun PrefsScoreStore.exportAll(): String = com.oregontrail.engine.SaveBundle.encode(listSlots(), loadState())

    /** Imports a bundle, merging slots by id. Returns the number of new slots. */
fun PrefsScoreStore.importAll(text: String): Int {
        val bundle = com.oregontrail.engine.SaveBundle.decode(text) ?: return 0
        val existing = listSlots().toMutableList()
        val ids = existing.map { it.id }.toMutableSet()
        var added = 0
        for (slot in bundle.slots) {
            if (ids.add(slot.id)) {
                existing.add(slot)
                added++
            }
        }
        writeSlots(existing)
        val auto = bundle.autosave
        if (auto != null) saveState(auto)
        return added
    }

fun PrefsScoreStore.loadSlotData(id: String): String? = listSlots().firstOrNull { it.id == id }?.data

internal fun PrefsScoreStore.writeSlots(slots: List<SaveSlot>) {
        val raw = slots.joinToString("\n") { slot ->
            val encoded = Base64.encodeToString(slot.data.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            "${slot.id}|${slot.label}|${slot.detail}|${slot.savedAt}|$encoded"
        }
        prefs.edit().putString(KEY_SLOTS, raw).apply()
    }
