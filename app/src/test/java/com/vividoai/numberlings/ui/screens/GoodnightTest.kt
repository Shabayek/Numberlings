package com.vividoai.numberlings.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GoodnightTest {

    @Test
    fun `the goodnight says why sleep matters`() {
        assertTrue(
            "the message should mention rest or sleep",
            Goodnight.MESSAGE.contains("rest") || Goodnight.MESSAGE.contains("sleep")
        )
        assertTrue(
            "the message should mention the brain keeping what was practised",
            Goodnight.MESSAGE.contains("brain")
        )
    }

    @Test
    fun `the goodnight teaches what a phone is for`() {
        assertTrue(
            "the message should say the phone is for learning",
            Goodnight.MESSAGE.contains("classroom")
        )
    }

    @Test
    fun `both ways out are offered`() {
        assertEquals("Exit", Goodnight.EXIT)
        assertEquals("Learn something new", Goodnight.LEARN)
        assertTrue(Goodnight.SPOKEN.isNotBlank())
        assertTrue(Goodnight.HEADLINE.isNotBlank())
    }
}
