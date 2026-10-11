package com.juancholopes.weightrack.ui.theme

import androidx.compose.animation.core.SpringSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionTest {

    private fun MotionTokens.springs(): List<SpringToken> =
        listOf(fastSpatial, defaultSpatial, slowSpatial, fastEffects, defaultEffects, slowEffects)

    // S41-R09: copies ExpressiveMotionTokens of material3 1.4.0 until MotionScheme is public.
    @Test
    fun expressive_copiesTheMaterialExpressiveSpringTokens() {
        val expected = listOf(
            SpringToken(0.6f, 800f), SpringToken(0.8f, 380f), SpringToken(0.8f, 200f),
            SpringToken(1.0f, 3800f), SpringToken(1.0f, 1600f), SpringToken(1.0f, 800f),
        )

        assertEquals(expected, MotionTokens.Expressive.springs())
    }

    // S41-R09: copies StandardMotionTokens of material3 1.4.0.
    @Test
    fun standard_copiesTheMaterialStandardSpringTokens() {
        val expected = listOf(
            SpringToken(0.9f, 1400f), SpringToken(0.9f, 700f), SpringToken(0.9f, 300f),
            SpringToken(1.0f, 3800f), SpringToken(1.0f, 1600f), SpringToken(1.0f, 800f),
        )

        assertEquals(expected, MotionTokens.Standard.springs())
    }

    // S41-R10
    @Test
    fun withoutBounce_setsEveryDampingRatioToAtLeastOneAndKeepsStiffness() {
        val motion = MotionTokens.Expressive.withoutBounce()

        motion.springs().zip(MotionTokens.Expressive.springs()).forEach { (spring, original) ->
            assertEquals(1f, spring.dampingRatio)
            assertEquals(original.stiffness, spring.stiffness)
        }
        assertFalse(motion.reducedMotion)
    }

    // S41-R10: reduced motion removes bounce and tells callers to drop offsets, scale, axis animation and loops.
    @Test
    fun forReducedMotion_removesBounceAndFlagsReducedMotion() {
        val motion = MotionTokens.Expressive.forReducedMotion()

        assertTrue(motion.reducedMotion)
        assertTrue(motion.springs().all { it.dampingRatio >= 1f })
        assertFalse(MotionTokens.Expressive.reducedMotion)
    }

    // S41-R09
    @Test
    fun springToken_spec_buildsASpringWithTheTokenValues() {
        val spec = SpringToken(dampingRatio = 0.6f, stiffness = 800f).spec<Float>()

        assertEquals(SpringSpec<Float>(dampingRatio = 0.6f, stiffness = 800f), spec)
    }
}
