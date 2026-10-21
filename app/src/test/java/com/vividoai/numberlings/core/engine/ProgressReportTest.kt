package com.vividoai.numberlings.core.engine

import com.vividoai.numberlings.core.model.SkillKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressReportTest {

    private val sample = mapOf(
        SkillKind.COUNT to 99,
        SkillKind.COMPARE to 99,
        SkillKind.ADD_JOIN to 97,
        SkillKind.SUB_TAKE to 90,
        SkillKind.SKIP_COUNT to 69,
        SkillKind.SHARE_EVEN to 67,
        SkillKind.PARTITION to 69
    )

    @Test
    fun `the report speaks in plain words, not raw ids`() {
        val report = ProgressReport.render(sample, hearts = 20, blocks = 20, stickers = setOf(4, 9, 24))

        // a greeting and an explanation of what the numbers mean
        assertTrue(report.contains("PROGRESS REPORT"))
        assertTrue(report.contains("Hello!"))
        assertTrue(report.contains("practising"))

        // named skills, grouped by meadow (only the skills that were met)
        assertTrue(report.contains("Counting Meadow"))
        assertTrue(report.contains("Counting"))
        assertTrue(report.contains("Sharing equally"))
        assertFalse(report.contains("The missing number"))

        // the totals in plain words
        assertTrue(report.contains("Hearts earned .............. 20"))
        assertTrue(report.contains("Puzzle blocks finished .... 20"))
        assertTrue(report.contains("Stickers collected ......... 3 of 30"))

        // no raw data dump anywhere
        assertFalse(report.contains("skill_id"))
        assertFalse(report.contains("mastery_percent"))
        assertFalse(report.contains("blocks_completed"))
    }

    @Test
    fun `the sticker album explains itself`() {
        val report = ProgressReport.render(sample, hearts = 20, blocks = 20, stickers = setOf(4, 9, 24))

        // how a sticker is earned
        assertTrue(report.contains("A sticker is earned at every Market Day"))
        assertTrue(report.contains("every five finished blocks"))

        // which slots, said out loud, and the album drawn slot by slot
        assertTrue(report.contains("5, 10 and 25"))
        val rows = report.lines().filter { it.trim().startsWith("[") }
        assertEquals(2, rows.size)
        assertEquals(30, rows.sumOf { row -> row.count { it == '[' } })
        assertEquals(3, rows.sumOf { row -> row.count { it == 'x' } })
    }

    @Test
    fun `the closing lines are warm and never read like surveillance`() {
        val report = ProgressReport.render(sample, hearts = 20, blocks = 20, stickers = setOf(4, 9, 24))

        // no talk of what the app "knows about the child"
        assertFalse(report.contains("knows about"))
        assertFalse(report.contains("child"))
        assertFalse(report.contains("collect about"))

        // and it ends by saying the numbers stay put
        assertTrue(report.contains("A snapshot of the practice so far"))
        assertTrue(report.contains("keeps no copy of it anywhere else"))
    }

    @Test
    fun `a skill that has not been met is simply left out`() {
        val report = ProgressReport.render(
            mapOf(SkillKind.COUNT to 99),
            hearts = 1,
            blocks = 1,
            stickers = emptySet()
        )
        assertTrue(report.contains("Counting Meadow"))
        assertFalse(report.contains("Comparing groups"))
        assertFalse(report.contains("THE STICKER ALBUM"))
    }
}
