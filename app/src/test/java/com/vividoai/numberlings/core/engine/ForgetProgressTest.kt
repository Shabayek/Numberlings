package com.vividoai.numberlings.core.engine

import com.vividoai.numberlings.core.model.SkillKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ForgetProgressTest {

    @Test
    fun `forgetting a meadow leaves the other meadows alone`() {
        var state = AdaptiveState()
        state = state.after(SkillKind.COUNT, true)
        state = state.after(SkillKind.COMPARE, true)
        state = state.after(SkillKind.ADD_JOIN, true)
        state = state.after(SkillKind.ADD_MISSING, true)

        val counting = Curriculum.meadowSkills.getValue(com.vividoai.numberlings.core.model.Meadow.COUNT)
        val forgotten = state.forget(counting)

        assertTrue("the meadow's own skills are gone", forgotten.scoreOf(SkillKind.COUNT).mastery == 0.0)
        assertTrue(forgotten.scoreOf(SkillKind.COMPARE).mastery == 0.0)
        assertTrue("the other meadow is untouched", forgotten.scoreOf(SkillKind.ADD_JOIN).mastery > 0.0)
        assertTrue(forgotten.scoreOf(SkillKind.ADD_MISSING).mastery > 0.0)
    }

    @Test
    fun `forgetting everything leaves an empty slate`() {
        var state = AdaptiveState()
        state = state.after(SkillKind.COUNT, true)
        state = state.after(SkillKind.PARTITION, true)
        val empty = state.forget(SkillKind.entries.toList())
        assertEquals(0, empty.scores.size)
    }
}
