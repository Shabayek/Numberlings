package com.vividoai.numberlings.core.engine

import com.vividoai.numberlings.core.model.SkillKind
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveTest {

    private val Unlocked = listOf(
        SkillKind.COUNT, SkillKind.COMPARE, SkillKind.ADD_JOIN, SkillKind.ADD_MISSING
    )

    @Test
    fun `mastery rises on correct and sinks gently when wobbling`() {
        var adaptive = AdaptiveState()
        repeat(4) { adaptive = adaptive.after(SkillKind.COUNT, true) }
        val peak = adaptive.scoreOf(SkillKind.COUNT).mastery
        assertTrue("peak mastery after 4 corrects = $peak", peak > 0.5)
        repeat(3) { adaptive = adaptive.after(SkillKind.COUNT, false) }
        val after = adaptive.scoreOf(SkillKind.COUNT).mastery
        assertTrue("wobble must dip mastery ($peak → $after)", after < peak)
    }

    @Test
    fun `next skill is the weakest of the unlocked pool`() {
        var adaptive = AdaptiveState()
        repeat(6) { adaptive = adaptive.after(SkillKind.COUNT, true) }
        assertEquals(SkillKind.COMPARE, adaptive.nextSkill(Unlocked, seed = 1))
    }

    @Test
    fun `never the same skill twice in a row when choices exist`() {
        var adaptive = AdaptiveState().after(SkillKind.COUNT, false)
        repeat(20) { i ->
            val next = adaptive.nextSkill(Unlocked, seed = i.toLong())
            assertTrue(next != adaptive.lastSkill || Unlocked.size == 1)
            adaptive = adaptive.after(next, i % 2 == 0)
        }
    }

    @Test
    fun `tier grows with mastery and respects bias bounds`() {
        var adaptive = AdaptiveState()
        repeat(10) { adaptive = adaptive.after(SkillKind.ADD_JOIN, true) }
        assertEquals(2, adaptive.tierFor(SkillKind.ADD_JOIN))
        assertEquals(0, adaptive.copy(bias = -2).tierFor(SkillKind.ADD_JOIN))
    }

    @Test
    fun `curriculum pool order is stable`() {
        assertEquals(SkillKind.COUNT, Curriculum.allSkills.first())
        assertEquals(12, Curriculum.allSkills.size)
    }

    @Test
    fun `extra seed jitter never picks an out-of-pool skill`() {
        val pool = listOf(SkillKind.ADD_JOIN, SkillKind.ADD_MISSING)
        var adaptive = AdaptiveState()
        repeat(10) { i ->
            val next = adaptive.nextSkill(pool, seed = Random(i.toLong()).nextLong())
            adaptive = adaptive.after(next, true)
            assertTrue(next in pool)
        }
    }
}
