package com.oregontrail.engine

/**
 * A portable, human-readable text format for exporting and importing save
 * slots (and the autosave). Pure Kotlin so it can be unit tested and reused by
 * any front-end.
 */
object SaveBundle {

    const val HEADER = "OREGON-SAVES-1"

    data class Bundle(val slots: List<SaveSlot>, val autosave: String?)

    fun encode(slots: List<SaveSlot>, autosave: String?): String {
        val sb = StringBuilder()
        sb.append(HEADER).append('\n')
        if (autosave != null) sb.append("A|").append(esc(autosave)).append('\n')
        for (s in slots) {
            sb.append("S|").append(esc(s.id)).append('|')
                .append(esc(s.label)).append('|')
                .append(esc(s.detail)).append('|')
                .append(s.savedAt).append('|')
                .append(esc(s.data)).append('\n')
        }
        return sb.toString()
    }

    fun decode(text: String): Bundle? {
        val lines = text.split('\n')
        if (lines.isEmpty() || lines[0].trim() != HEADER) return null
        val slots = ArrayList<SaveSlot>()
        var autosave: String? = null
        for (i in 1 until lines.size) {
            val line = lines[i]
            if (line.isBlank()) continue
            val parts = splitEscaped(line)
            when (parts.getOrNull(0)) {
                "A" -> autosave = parts.getOrNull(1)?.let { unesc(it) }
                "S" -> if (parts.size >= 6) {
                    slots.add(
                        SaveSlot(
                            unesc(parts[1]), unesc(parts[2]), unesc(parts[3]),
                            parts[4].toLongOrNull() ?: 0L, unesc(parts[5])
                        )
                    )
                }
            }
        }
        return Bundle(slots, autosave)
    }

    private fun esc(s: String): String =
        s.replace("\\", "\\\\").replace("|", "\\p").replace("\n", "\\n")

    private fun unesc(s: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) {
                when (s[i + 1]) {
                    'n' -> sb.append('\n')
                    'p' -> sb.append('|')
                    '\\' -> sb.append('\\')
                    else -> sb.append(s[i + 1])
                }
                i += 2
            } else {
                sb.append(c)
                i++
            }
        }
        return sb.toString()
    }

    private fun splitEscaped(line: String): List<String> {
        val out = ArrayList<String>()
        val sb = StringBuilder()
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '\\' && i + 1 < line.length) {
                sb.append(c).append(line[i + 1])
                i += 2
            } else if (c == '|') {
                out.add(sb.toString())
                sb.clear()
                i++
            } else {
                sb.append(c)
                i++
            }
        }
        out.add(sb.toString())
        return out
    }
}
