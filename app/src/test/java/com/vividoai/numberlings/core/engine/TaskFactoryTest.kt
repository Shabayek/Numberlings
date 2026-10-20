package com.vividoai.numberlings.core.engine

import com.vividoai.numberlings.core.model.PuzzleLimits
import com.vividoai.numberlings.core.model.SkillKind
import com.vividoai.numberlings.core.model.VisualKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskFactoryTest {

    @Test
    fun `deterministic for the same seed`() {
        for (skill in SkillKind.entries) {
            val a = TaskFactory.make(skill, 1, seed = 42)
            val b = TaskFactory.make(skill, 1, seed = 42)
            assertEquals(a, b)
        }
    }

    @Test
    fun `every task has its correct choice present`() {
        for (skill in SkillKind.entries) {
            for (tier in 0..2) {
                for (seed in listOf(1L, 99L, 12345L, -7L)) {
                    val task = TaskFactory.make(skill, tier, seed)
                    assertTrue(
                        "skill=$skill tier=$tier seed=$seed",
                        task.correctIndex in task.choices.indices
                    )
                }
            }
        }
    }

    @Test
    fun `choices are unique`() {
        for (skill in SkillKind.entries) {
            for (tier in 0..2) {
                val task = TaskFactory.make(skill, tier, seed = 5L)
                assertEquals(
                    task.choices.map { it.label }.distinct(),
                    task.choices.map { it.label }
                )
            }
        }
    }

    @Test
    fun `numeric tasks stay in curriculum ranges`() {
        // Counting never asks above 20 in the gentlest tier.
        val t0 = TaskFactory.make(SkillKind.COUNT, 0, seed = 2)
        assertTrue(t0.visual.left in 3..6)
        val t2 = TaskFactory.make(SkillKind.COUNT, 2, seed = 2)
        assertTrue(t2.visual.left in 3..20)

        // Skip counting lands on multiples of its step (step is 2, 5 or 10 — landing shows that).
        val frog = TaskFactory.make(SkillKind.SKIP_COUNT, 1, seed = 8)
        val landing = frog.visual.hops
        assertTrue("landing $landing not a 2/5/10 multiple", landing % 2 == 0 || landing % 5 == 0 && landing % 10 != 3)

        // Sharing is exact.
        val shared = TaskFactory.make(SkillKind.SHARE_EVEN, 1, seed = 3)
        assertEquals(0, shared.visual.left % shared.visual.parts)
    }

    @Test
    fun `counting never asks for more than the picture can show`() {
        for (tier in 0..2) {
            for (seed in 1L..60L) {
                val task = TaskFactory.make(SkillKind.COUNT, tier, seed)
                assertTrue(
                    "seed=$seed tier=$tier asked for ${task.visual.left}",
                    task.visual.left <= PuzzleLimits.MAX_COUNTED
                )
            }
        }
    }

    @Test
    fun `the missing addend is never drawn for the child to count`() {
        for (tier in 0..2) {
            for (seed in 1L..40L) {
                val task = TaskFactory.make(SkillKind.ADD_MISSING, tier, seed)
                assertEquals(
                    "the unknown amount must not be pictured",
                    VisualKind.PARTIAL_JOIN,
                    task.visual.kind
                )
                assertEquals("only the known group is drawn", 0, task.visual.right)
            }
        }
    }
}
