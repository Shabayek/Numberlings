package com.vividoai.numberlings.core.model

/**
 * The full album: 30 stickers, 5 per meadow. Every sticker is earned by
 * practising — never bought, never random — and the album fills in a calm,
 * predictable order, one sticker per Market Day. A sticker the child already
 * owns is never handed out twice.
 */
object StickerAlbum {
    const val TOTAL = 30
    const val EVERY = 5

    /** Market Day visits happen on every fifth finished block — a shop, never a countdown. */
    fun isMarketDay(blocksCompleted: Int): Boolean = blocksCompleted % EVERY == 0

    /**
     * The next sticker the album is still missing, in album order, or -1 when
     * the album is complete. Never re-awards a sticker the child already owns.
     */
    fun nextUnowned(owned: Set<Int>): Int = (0 until TOTAL).firstOrNull { it !in owned } ?: -1

    /** Meadow theme of a sticker: 5 consecutive stickers per meadow. */
    fun meadow(stickerIndex: Int): Int = stickerIndex / EVERY
}
