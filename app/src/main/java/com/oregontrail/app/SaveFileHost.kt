package com.oregontrail.app

import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts

/**
 * Wraps the system file pickers used to export and import save bundles, so the
 * activity itself stays free of result plumbing.
 */
class SaveFileHost(private val activity: MainActivity) {

    private val exportLauncher =
        activity.registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
            if (uri == null) return@registerForActivityResult
            try {
                activity.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(activity.store.exportAll().toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(activity, "Saves exported", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
                Toast.makeText(activity, "Export failed", Toast.LENGTH_SHORT).show()
            }
        }

    private val importLauncher =
        activity.registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@registerForActivityResult
            try {
                val text = activity.contentResolver.openInputStream(uri)?.use {
                    it.readBytes().toString(Charsets.UTF_8)
                } ?: ""
                val added = activity.store.importAll(text)
                activity.refreshSlots()
                activity.render()
                Toast.makeText(
                    activity,
                    if (added > 0) "Imported $added save(s)" else "No new saves found",
                    Toast.LENGTH_LONG
                ).show()
            } catch (_: Exception) {
                Toast.makeText(activity, "Import failed", Toast.LENGTH_SHORT).show()
            }
        }

    fun export() = exportLauncher.launch("oregon-trail-saves.txt")

    fun import() = importLauncher.launch(arrayOf("text/*", "application/octet-stream", "*/*"))
}
