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
    val spoken: String,
    /** When true the puzzle has no answer buttons: the child taps the two
     *  groups inside the drawing itself (top half = choice 0, bottom = 1). */
    val tapTheGroups: Boolean = false
)
