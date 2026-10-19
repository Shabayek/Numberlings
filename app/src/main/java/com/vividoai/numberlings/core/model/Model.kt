package com.vividoai.numberlings.core.model

/**
 * Six meadows == six curriculum strands (approx. K–Grade 3).
 * Order is the world order; each meadow unlocks after the previous one.
 */
enum class Meadow(val title: String, val subtitle: String) {
    COUNT("Counting Meadow", "counting, comparing, ordering"),
    ADD("Addition Field", "joining groups, sums within 20"),
    SUB("Subtraction Brook", "taking away, finding differences"),
    TIMES("Times Orchard", "groups of things, times tables"),
    SHARE("Share Fair", "fair sharing, leftovers"),
    SHAPE("Fraction Forest & Shape Shire", "shapes, halves, quarters");
}

enum class SkillKind {
    COUNT,
    COMPARE,
    ADD_JOIN,
    ADD_MISSING,
    SUB_TAKE,
    SUB_DIFFERENCE,
    TIMES_GROUPS,
    SKIP_COUNT,
    SHARE_EVEN,
    SHARE_LEFTOVERS,
    PARTITION,
    SHAPE_HUNT
}

/** Mastery score per skill, 0.0 (new) → 1.0 (confident). */
data class SkillScore(val skill: SkillKind, val mastery: Double = 0.0) {
    operator fun plus(correct: Boolean): SkillScore {
        val target = if (correct) 1.0 else 0.0
        val next = mastery + (target - mastery) * LearningRate
        return SkillScore(skill, next.coerceIn(0.0, 1.0))
    }

    fun tier(bias: Int = 0): Int {
        // Tier grows as mastery grows; parent (grown-up) bias can bump/pre-simplify for
        // accessibility, never as a punishment-spike.
        val earned = ((mastery * 3.0).toInt()).coerceIn(0, 2)
        return (earned + bias).coerceIn(0, 2)
    }

    companion object {
        const val LearningRate = 0.18
    }
}
