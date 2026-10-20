package com.vividoai.numberlings.core.model

/**
 * The limits the paper pictures can honour. The engine and the artwork share
 * these numbers, so a puzzle can never ask for more things than the picture
 * is able to show — a hidden dot would make the puzzle a lie.
 */
object PuzzleLimits {
    /** The most things a counting picture ever shows. */
    const val MAX_COUNTED = 20
}

/**
 * How a pile of things is laid out on the paper: how many fit in a row, and
 * how many rows are needed. Pure, so it can be checked in tests.
 */
data class CountLayout(val count: Int, val perRow: Int, val rows: Int) {

    init {
        require(count >= 0) { "count must not be negative" }
        require(rows * perRow >= count) { "layout cannot hold $count things" }
    }

    companion object {
        fun of(count: Int): CountLayout {
            val perRow = if (count <= 12) 6 else 5
            val rows = if (count <= 0) 0 else (count + perRow - 1) / perRow
            return CountLayout(count, perRow, rows)
        }
    }
}
