package com.vividoai.numberlings.core.engine

import com.vividoai.numberlings.core.model.SkillKind
import com.vividoai.numberlings.core.model.VisualKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The pizza/fraction task must always be answerable by counting: the number the
 * child can work out has to be the number the app accepts.
 */
class PartitionTest {

    @Test
    fun `the offered answer always matches the question`() {
        var quarters = 0
        var halves = 0
        for (tier in 0..2) {
            for (seed in 1L..400L) {
                val task = TaskFactory.make(SkillKind.PARTITION, tier, seed)
                val total = Regex("A pizza has (\\d+) slices").find(task.prompt)!!.groupValues[1].toInt()
                val quarter = task.prompt.endsWith("one quarter?")
                val expected = if (quarter) total / 4 else total / 2
                val correct = task.choices[task.correctIndex].label
                assertEquals(
                    "tier=$tier seed=$seed prompt='${task.prompt}'",
                    expected.toString(),
                    correct
                )
                assertTrue(
                    "tier=$tier seed=$seed: the answer $expected is not among ${task.choices.map { it.label }}",
                    task.choices.any { it.label == expected.toString() }
                )
                // the picture must show the same number of slices as the question
                assertEquals(total, task.visual.outOf)
                assertEquals(VisualKind.PIZZA, task.visual.kind)
                if (quarter) quarters++ else halves++
            }
        }
        assertTrue("the quarter variant should be reachable", quarters > 0)
        assertTrue("the half variant should be reachable", halves > 0)
    }

    @Test
    fun `a quarter is only asked when it divides the pizza exactly`() {
        for (seed in 1L..400L) {
            val task = TaskFactory.make(SkillKind.PARTITION, 2, seed)
            if (task.prompt.endsWith("one quarter?")) {
                val total = task.visual.outOf
                assertEquals("a quarter of $total must be whole slices", 0, total % 4)
            }
        }
    }

    @Test
    fun `the pizza task asks more than one question`() {
        val prompts = (1L..400L)
            .map { TaskFactory.make(SkillKind.PARTITION, 2, it).prompt }
            .toSet()
        assertTrue(
            "the pizza task should offer a few different questions, found ${prompts.size}",
            prompts.size >= 4
        )
    }
}
