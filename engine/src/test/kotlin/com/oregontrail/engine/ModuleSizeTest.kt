package com.oregontrail.engine

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Keeps the codebase modular: no Kotlin source file may grow past [LIMIT]
 * lines. The project was refactored into many small files on purpose, and this
 * test stops them from quietly turning back into monoliths.
 */
class ModuleSizeTest {

    private val limit = 150

    private fun repoRoot(): File? =
        generateSequence(File(".").absoluteFile) { it.parentFile }
            .firstOrNull { File(it, "settings.gradle.kts").exists() }

    @Test
    fun no_source_file_exceeds_the_line_limit() {
        val root = repoRoot() ?: return
        val offenders = ArrayList<String>()
        for (module in listOf("engine", "app")) {
            val src = File(root, "$module/src")
            if (!src.exists()) continue
            src.walkTopDown()
                .filter { it.isFile && it.name.endsWith(".kt") }
                .forEach { file ->
                    val lines = file.readLines().size
                    if (lines > limit) {
                        offenders.add("${file.relativeTo(root)} ($lines lines)")
                    }
                }
        }
        assertTrue(
            offenders.isEmpty(),
            "these files exceed $limit lines: " + offenders.joinToString(", ")
        )
    }
}
