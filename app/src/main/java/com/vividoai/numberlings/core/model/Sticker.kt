package com.vividoai.numberlings.core.model

/**
 * The full album: 30 stickers, 5 per meadow. Every sticker's index is a pure
 * function of blocks completed — nothing is random, nothing is purchasable,
 * nothing can be missed forever: the album fills at the child's own pace.
 */
object StickerAlbum {
    const val TOTAL = 30
    const val EVERY = 5

    /** Sticker earned by the block that made `blocksCompleted` (1-based). */
    fun indexFor(blocksCompleted: Int): Int = (blocksCompleted - 1).mod(TOTAL)

    /** Market Day visits happen on every fifth sticker — a shop, never a countdown. */
    fun isMarketDay(stickerIndex: Int): Boolean = stickerIndex % EVERY == EVERY - 1

    /** Meadow theme of a sticker: 5 consecutive stickers per meadow. */
    fun meadow(stickerIndex: Int): Int = stickerIndex / EVERY
}
