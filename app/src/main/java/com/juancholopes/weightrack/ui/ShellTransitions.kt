package com.juancholopes.weightrack.ui

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.unit.IntOffset
import com.juancholopes.weightrack.ui.theme.MotionTokens

/*
 * Transitions of the shell (SF00-R06, table "Transiciones" of Spec 42). They read the springs of
 * `WeightrackTheme.motion`, so they follow the visual direction. With reduced motion (S41-R10) there is no
 * offset or scale: only fades with the fast effects spring.
 */

/** A screen travels a tenth of the width when it enters or leaves (shared X axis). */
private const val SLIDE_DIVISOR = 10

/** Going forward: the new screen comes from the right, the previous one leaves to the left and fades. */
internal fun forwardTransition(motion: MotionTokens): ContentTransform =
    if (motion.reducedMotion) {
        reducedMotionTransition(motion)
    } else {
        horizontalTransition(motion, enterFromRight = true)
    }

/** Going back: the inverse of [forwardTransition]. */
internal fun popTransition(motion: MotionTokens): ContentTransform =
    if (motion.reducedMotion) {
        reducedMotionTransition(motion)
    } else {
        horizontalTransition(motion, enterFromRight = false)
    }

/** Predictive back with reduced motion: a fade without the scale of the `NavDisplay` default. */
internal fun reducedMotionTransition(motion: MotionTokens): ContentTransform =
    fadeIn(motion.fastEffectsSpec()) togetherWith fadeOut(motion.fastEffectsSpec())

private fun horizontalTransition(motion: MotionTokens, enterFromRight: Boolean): ContentTransform {
    val direction = if (enterFromRight) 1 else -1
    val offsetSpec = motion.defaultSpatial.spec(IntOffset.VisibilityThreshold)
    val enter = slideInHorizontally(offsetSpec) { width -> direction * width / SLIDE_DIVISOR } +
        fadeIn(motion.defaultEffectsSpec())
    val exit = slideOutHorizontally(offsetSpec) { width -> -direction * width / SLIDE_DIVISOR } +
        fadeOut(motion.defaultEffectsSpec())
    return enter togetherWith exit
}
