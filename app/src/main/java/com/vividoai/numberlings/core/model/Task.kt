package com.vividoai.numberlings.core.model

/** One puzzle shown to a child. */
data class Task(
    val skill: SkillKind,
    val tier: Int,
    val prompt: String,
    val visual: TaskVisual,
    val choices: List<Choice>,
    val correctIndex: Int,
    /** Fuller spoken phrasing for pre-readers (system text-to-speech, fully offline). */
    val spoken: String
)
