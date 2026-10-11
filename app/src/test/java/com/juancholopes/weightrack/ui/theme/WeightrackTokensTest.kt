package com.juancholopes.weightrack.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class WeightrackTokensTest {

    // S41-R02, S41-R09, S41-R20
    @Test
    fun weightrackTokens_fromDirection_carryItsSpacingNumbersAndMotion() {
        val direction = VisualDirection.Scoreboard

        val tokens = weightrackTokens(direction, reducedMotion = false)

        assertSame(direction, tokens.direction)
        assertEquals(direction.spacing, tokens.spacing)
        assertEquals(numberStyles(direction.numbers), tokens.numbers)
        assertEquals(direction.motion, tokens.motion)
    }

    // S41-R10
    @Test
    fun weightrackTokens_withReducedMotion_useTheReducedMotionOfTheDirection() {
        val tokens = weightrackTokens(VisualDirection.Expressive, reducedMotion = true)

        assertEquals(VisualDirection.Expressive.motion.forReducedMotion(), tokens.motion)
    }
}
