package com.vividoai.numberlings.core.engine

import com.vividoai.numberlings.core.model.Meadow
import com.vividoai.numberlings.core.model.SkillKind

object Curriculum {
    /** Skills unlocked by each meadow as the village wakes up. */
    val meadowSkills: Map<Meadow, List<SkillKind>> = mapOf(
        Meadow.COUNT to listOf(SkillKind.COUNT, SkillKind.COMPARE),
        Meadow.ADD to listOf(SkillKind.ADD_JOIN, SkillKind.ADD_MISSING),
        Meadow.SUB to listOf(SkillKind.SUB_TAKE, SkillKind.SUB_DIFFERENCE),
        Meadow.TIMES to listOf(SkillKind.SKIP_COUNT, SkillKind.TIMES_GROUPS),
        Meadow.SHARE to listOf(SkillKind.SHARE_EVEN, SkillKind.SHARE_LEFTOVERS),
        Meadow.SHAPE to listOf(SkillKind.PARTITION, SkillKind.SHAPE_HUNT)
    )

    val allSkills: List<SkillKind> = Meadow.entries.flatMap { meadowSkills[it]!! }

    fun moveTo(nextMeadow: Meadow): List<SkillKind> =
        Meadow.entries.take(nextMeadow.ordinal + 1).flatMap { meadowSkills[it]!! }
}
