package com.vividoai.numberlings.core.model

/**
 * One puzzle. Visual-first: every task has concrete art, short spoken prompt,
 * and 2–4 tap choices, exactly one correct.
 */
data class Choice(val id: String, val label: String)

enum class VisualKind {
    OBJECTS, JOIN, TAKE_AWAY, DIFFERENCE, GROUPS_ARRAY, NUMBER_LINE, PLATES, PIZZA, SHAPES,

    /** Only the known group is drawn; the unknown amount is left as an empty
     *  space so the picture never gives the answer away. */
    PARTIAL_JOIN
}

data class TaskVisual(
    val kind: VisualKind,
    /** OBJECTS/JOIN/TAKE_AWAY/DIFFERENCE: counts of loose things. */
    val left: Int = 0,
    val right: Int = 0,
    /** GROUPS_ARRAY: rows x cols baskets. */
    val rows: Int = 0,
    val cols: Int = 0,
    /** NUMBER_LINE hop spec. */
    val from: Int = 0,
    val hops: Int = 0,
    /** NUMBER_LINE: size of one hop, so the line can show its own steps. */
    val step: Int = 0,
    /** PLATES shoes items evenly shared; PIZZA slice fraction p/q. */
    val parts: Int = 0,
    val outOf: Int = 0,
    /** SHAPES: which shape the art draws. */
    val shape: ShapeKind = ShapeKind.TRIANGLE
) {
    val objectsTotal: Int get() = left + right
    val groupsTotal: Int get() = rows * cols
    val sharedPerPlate: Int get() = if (parts > 0) objectsTotal / parts else 0
    val remainder: Int get() = if (parts > 0) objectsTotal % parts else 0
}

enum class ShapeKind { TRIANGLE, RECTANGLE, CIRCLE, HEART, STAR }
