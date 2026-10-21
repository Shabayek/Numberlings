package com.vividoai.numberlings.core.engine

import com.vividoai.numberlings.core.model.SkillKind
import com.vividoai.numberlings.core.model.SkillScore
import kotlin.random.Random

/**
 * The adaptive heart of the game. Serves puzzles in the learner's zone
 * (≈70–85% expected success), skims toward the weakest unlocked skill, and
 * never repeats the just-answered skill like a metronome.
 *
 * No streak bracelets, no fail counters, no punished wrong answers — mastery
 * moves one way: kindly upward on success, softly downward on wobble.
 */
data class AdaptiveState(
    val scores: Map<SkillKind, SkillScore> = emptyMap(),
    val bias: Int = 0,
    val lastSkill: SkillKind? = null
) {
    fun scoreOf(skill: SkillKind): SkillScore = scores[skill] ?: SkillScore(skill, mastery = 0.0)

    /** Weakest skill in the unlocked pool, skipping the one just answered. */
    fun nextSkill(unlockedSkills: List<SkillKind>, seed: Long): SkillKind {
        if (unlockedSkills.size == 1) return unlockedSkills.first()
        val pool = unlockedSkills.filter { it != lastSkill }.ifEmpty { unlockedSkills }
        return pool.minByOrNull { scoreOf(it).mastery + Random(seed).nextDouble(0.05) } ?: pool.first()
    }

    fun after(answered: SkillKind, correct: Boolean): AdaptiveState = copy(
        scores = scores + (answered to scoreOf(answered).plus(correct)),
        lastSkill = answered
    )

    /**
     * Forget what was practised in these skills, so they can be met again from
     * the beginning. Everything else is left alone.
     */
    fun forget(skills: Collection<SkillKind>): AdaptiveState =
        copy(scores = scores - skills.toSet(), lastSkill = null)

    fun tierFor(skill: SkillKind): Int = scoreOf(skill).tier(bias)

    /** Decode the persisted `skill:mastery` CSV blanket. */
    fun fromCsv(csv: String): AdaptiveState {
        val map = csv.split(";")
            .mapNotNull { row ->
                if (row.isBlank()) return@mapNotNull null
                val parts = row.split(":")
                val skill = parts.getOrNull(0)?.toIntOrNull()?.let { SkillKind.entries.getOrNull(it) }
                    ?: return@mapNotNull null
                skill to SkillScore(skill, (parts.getOrNull(1)?.toIntOrNull() ?: 0) / 100.0)
            }
            .toMap()
        return copy(scores = map)
    }

    fun toCsv(): String = scores.entries.joinToString(";") { (skill, score) ->
        "${skill.ordinal}:${(score.mastery * 100).toInt()}"
    }
}
