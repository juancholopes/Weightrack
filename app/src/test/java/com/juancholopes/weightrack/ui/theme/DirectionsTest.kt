package com.juancholopes.weightrack.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class DirectionsTest {

    // S41-R20
    @Test
    fun defaultDirection_isExpressive() {
        assertSame(VisualDirection.Expressive, DefaultDirection)
    }

    // S41-R20
    @Test
    fun presets_areExpressiveScoreboardAndNotebook() {
        val presets = VisualDirection.presets

        assertEquals(
            listOf(VisualDirection.Expressive, VisualDirection.Scoreboard, VisualDirection.Notebook),
            presets,
        )
        assertEquals(3, presets.map { it.name }.distinct().size)
    }

    // S41-R09: whatever the values, each spacing scale goes from small to large.
    @Test
    fun presets_spacingScales_areStrictlyIncreasing() {
        VisualDirection.presets.forEach { direction ->
            val scale = with(direction.spacing) { listOf(xxs, xs, s, m, l, xl, xxl, xxxl) }

            assertTrue("${direction.name}: $scale", scale.zipWithNext().all { (a, b) -> a < b })
        }
    }
}
