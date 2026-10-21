package com.vividoai.numberlings.core.engine

import com.vividoai.numberlings.core.model.SkillKind
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
        assertTrue(report.contains("Hearts earned"))
        assertTrue(report.contains("Hearts earned .............. 20"))
        assertTrue(report.contains("Puzzle blocks finished .... 20"))
        assertTrue(report.contains("Stickers collected ......... 3 of 30"))

        // the machine-readable tail is still there, and in a stable order
        assertTrue(report.contains("skill_id,skill,mastery_percent"))
        assertTrue(report.contains("0,Counting,99"))
        assertTrue(report.contains("hearts,20"))
        assertTrue(report.contains("blocks_completed,20"))
        val ids = report.lines()
            .filter { it.matches(Regex("^\\d+,.*,\\d+$")) }
            .map { it.substringBefore(",").toInt() }
        assertTrue("skill ids should be in order", ids == ids.sorted())
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
        assertFalse(report.contains("STICKERS"))
    }
}
