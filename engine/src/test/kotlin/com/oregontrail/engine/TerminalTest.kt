package com.oregontrail.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TerminalTest {

    @Test
    fun text_and_center_are_clipped_to_the_screen() {
        val s = Screen(10, 3)
        s.text(-3, 0, "HELLO")          // partially off the left edge
        assertEquals("LO", s.toLines()[0])

        s.text(8, 1, "ABCDEF")          // runs off the right edge
        assertEquals("        AB", s.toLines()[1])

        s.center(2, "MID")
        assertTrue(s.toLines()[2].contains("MID"))
        assertEquals("MID", s.toLines()[2].trim())
    }

    @Test
    fun wrap_handles_blank_lines_long_words_and_narrow_widths() {
        val s = Screen(40, 12)
        val next = s.wrap(0, 0, 10, "one two three four\n\nsupercalifragilistic")
        val joined = s.toLines().joinToString("\n")
        assertTrue(joined.contains("supercalif"), joined)
        assertTrue(joined.contains("ragilistic"), joined)
        // one blank paragraph must consume a row
        assertTrue(next >= 4)
    }

    @Test
    fun hotspots_clip_and_ignore_offscreen_regions() {
        val s = Screen(20, 5)
        val visible = s.hotspot("a", -2, 1, 6)
        assertEquals(0, visible!!.x0)
        assertEquals(3, visible.x1)

        assertNull(s.hotspot("gone", 0, 99, 5))     // entirely below
        assertNull(s.hotspot("gone2", 0, -4, 3))    // entirely above
        assertNull(s.hotspot("zero", 0, 0, 0))      // zero length
        assertTrue(s.hotspots.none { it.id == "gone" || it.id == "gone2" || it.id == "zero" })

        // Last registered wins, so overlapping regions behave predictably.
        s.hotspot("first", 4, 2, 10)
        s.hotspot("second", 6, 2, 2)
        assertEquals("second", s.hotspotAt(6, 2))
        assertEquals("first", s.hotspotAt(4, 2))
    }

    @Test
    fun box_draws_corners_and_sides() {
        val s = Screen(12, 5)
        s.box(0, 0, 12, 5)
        val lines = s.toLines()
        assertEquals("+----------+", lines[0])
        assertEquals("+----------+", lines[4])
        assertTrue(lines[2].startsWith("|") && lines[2].endsWith("|"))
    }

    @Test
    fun toLines_preserves_trailing_blank_rows_but_toText_trims_them() {
        val s = Screen(4, 3)
        s.text(0, 0, "AB")
        val lines = s.toLines()
        assertEquals(3, lines.size)
        assertEquals("AB", lines[0])
        assertEquals("", lines[1])
        assertEquals("", lines[2])
        assertEquals("AB", s.toText())
    }
}
