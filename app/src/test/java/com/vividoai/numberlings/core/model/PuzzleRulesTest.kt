package com.vividoai.numberlings.core.model

import com.vividoai.numberlings.core.data.GrownUps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The rules the pictures and the album must keep, checked in code so they
 * cannot drift apart again.
 */
class PuzzleRulesTest {

    @Test
    fun `every count the picture can be asked for fits on the paper`() {
        for (count in 1..PuzzleLimits.MAX_COUNTED) {
            val layout = CountLayout.of(count)
            assertTrue(
                "count $count does not fit in ${layout.rows} x ${layout.perRow}",
                layout.rows * layout.perRow >= count
            )
            assertTrue("the layout must use at least one row", layout.rows >= 1)
            assertEquals("the layout describes the count asked for", count, layout.count)
        }
    }

    @Test
    fun `the album hands out the next sticker that is missing, never a repeat`() {
        val owned = mutableSetOf<Int>()
        val order = mutableListOf<Int>()
        repeat(StickerAlbum.TOTAL) {
            val next = StickerAlbum.nextUnowned(owned)
            assertTrue("expected a sticker to be missing", next >= 0)
            assertTrue("sticker $next was handed out twice", next !in owned)
            owned.add(next)
            order.add(next)
        }
        assertEquals("the album fills in order", (0 until StickerAlbum.TOTAL).toList(), order)
        assertEquals("a complete album has nothing left", -1, StickerAlbum.nextUnowned(owned))
    }

    @Test
    fun `market day comes on every fifth block`() {
        assertTrue(StickerAlbum.isMarketDay(5))
        assertTrue(StickerAlbum.isMarketDay(10))
        assertTrue(StickerAlbum.isMarketDay(65))
        assertTrue(!StickerAlbum.isMarketDay(4))
        assertTrue(!StickerAlbum.isMarketDay(6))
    }

    @Test
    fun `a wrong tap costs far less than a right one gains`() {
        val confident = SkillScore(SkillKind.COUNT, 0.8)
        val afterWrong = confident.plus(false)
        val afterRight = confident.plus(true)
        assertTrue(
            "one wrong tap must not cost more than ten points",
            (confident.mastery - afterWrong.mastery) <= 0.10
        )
        assertTrue("a right answer must move mastery up", afterRight.mastery > confident.mastery)
    }

    @Test
    fun `the voice pace stays inside the warm range the speaker supports`() {
        assertEquals(0.5f, GrownUps(voiceSpeed = 5).rateFloat, 0.001f)
        assertEquals(0.9f, GrownUps(voiceSpeed = 9).rateFloat, 0.001f)
        assertEquals(1.5f, GrownUps(voiceSpeed = 15).rateFloat, 0.001f)
    }
}
