package com.oregontrail.app

import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.oregontrail.engine.recordPostcardSent
import com.oregontrail.engine.postcard
import com.oregontrail.engine.summaryText
import java.io.File
import java.io.FileOutputStream

/**
 * Turns the current journey into a postcard and hands it to the system share
 * sheet: a PNG image plus a plain-text summary.
 */
class ShareHost(private val activity: MainActivity) {

    fun sharePostcard() {
        val text = activity.game.summaryText()
        val bitmap = PostcardRenderer.render(
            activity.game.postcard(),
            activity.terminal.colors,
            activity.resources.displayMetrics.density
        )
        try {
            val dir = File(activity.cacheDir, "postcards").apply { mkdirs() }
            val file = File(dir, "oregon-trail-postcard.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
            }
            val uri = FileProvider.getUriForFile(
                activity, activity.packageName + ".fileprovider", file
            )
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, text)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            activity.startActivity(Intent.createChooser(send, "Share your postcard"))
        } catch (_: Exception) {
            // Fall back to sharing just the text.
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            try {
                activity.startActivity(Intent.createChooser(send, "Share your postcard"))
            } catch (_: Exception) {
                Toast.makeText(activity, "Could not share the postcard", Toast.LENGTH_SHORT).show()
            }
        }
        activity.game.recordPostcardSent()
    }
}
