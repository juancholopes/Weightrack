package com.juancholopes.weightrack.ui.theme

import android.animation.ValueAnimator
import android.os.Build
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/** A spring: damping ratio below 1 bounces, 1 does not. Higher stiffness is faster. */
@Immutable
data class SpringToken(val dampingRatio: Float, val stiffness: Float) {
    fun <T> spec(visibilityThreshold: T? = null): SpringSpec<T> =
        spring(dampingRatio = dampingRatio, stiffness = stiffness, visibilityThreshold = visibilityThreshold)
}

/**
 * Motion tokens, read with `WeightrackTheme.motion` (S41-R09).
 *
 * `MotionScheme` is internal in material3 1.4.0, so the springs copy its tokens (ExpressiveMotionTokens
 * and StandardMotionTokens). The `*Spec()` functions mirror the `MotionScheme` API, so the day
 * material3 1.5.0 is stable this file can read `MaterialTheme.motionScheme` without changing callers.
 *
 * - Spatial springs move things (position, scale). Effects springs change color or opacity, never bounce.
 * - [shortDurationMillis], [mediumDurationMillis] and [easing] are for tweens, such as crossfades.
 * - When [reducedMotion] is true (S41-R10), callers drop offsets, scale, font-axis animation and loops,
 *   and keep opacity. The springs no longer bounce.
 */
@Immutable
data class MotionTokens(
    val fastSpatial: SpringToken,
    val defaultSpatial: SpringToken,
    val slowSpatial: SpringToken,
    val fastEffects: SpringToken,
    val defaultEffects: SpringToken,
    val slowEffects: SpringToken,
    val shortDurationMillis: Int,
    val mediumDurationMillis: Int,
    val easing: Easing,
    val reducedMotion: Boolean = false,
) {
    fun <T> fastSpatialSpec(): FiniteAnimationSpec<T> = fastSpatial.spec()
    fun <T> defaultSpatialSpec(): FiniteAnimationSpec<T> = defaultSpatial.spec()
    fun <T> slowSpatialSpec(): FiniteAnimationSpec<T> = slowSpatial.spec()
    fun <T> fastEffectsSpec(): FiniteAnimationSpec<T> = fastEffects.spec()
    fun <T> defaultEffectsSpec(): FiniteAnimationSpec<T> = defaultEffects.spec()
    fun <T> slowEffectsSpec(): FiniteAnimationSpec<T> = slowEffects.spec()

    /** The same motion with critically damped springs: nothing bounces. */
    fun withoutBounce(): MotionTokens {
        fun SpringToken.critical() = copy(dampingRatio = maxOf(dampingRatio, 1f))
        return copy(
            fastSpatial = fastSpatial.critical(),
            defaultSpatial = defaultSpatial.critical(),
            slowSpatial = slowSpatial.critical(),
            fastEffects = fastEffects.critical(),
            defaultEffects = defaultEffects.critical(),
            slowEffects = slowEffects.critical(),
        )
    }

    /** The motion to use when the system asks for reduced motion (S41-R10). */
    fun forReducedMotion(): MotionTokens = withoutBounce().copy(reducedMotion = true)

    companion object {
        /** Material 3 easing curves (EasingTokens of material3 1.4.0). */
        val StandardEasing: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
        val StandardDecelerateEasing: Easing = CubicBezierEasing(0f, 0f, 0f, 1f)
        val EmphasizedDecelerateEasing: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

        /** Material 3 expressive motion scheme: springs with a bounded bounce. */
        val Expressive = MotionTokens(
            fastSpatial = SpringToken(dampingRatio = 0.6f, stiffness = 800f),
            defaultSpatial = SpringToken(dampingRatio = 0.8f, stiffness = 380f),
            slowSpatial = SpringToken(dampingRatio = 0.8f, stiffness = 200f),
            fastEffects = SpringToken(dampingRatio = 1f, stiffness = 3800f),
            defaultEffects = SpringToken(dampingRatio = 1f, stiffness = 1600f),
            slowEffects = SpringToken(dampingRatio = 1f, stiffness = 800f),
            shortDurationMillis = 250,
            mediumDurationMillis = 400,
            easing = EmphasizedDecelerateEasing,
        )

        /** Material 3 standard motion scheme: almost no bounce. */
        val Standard = MotionTokens(
            fastSpatial = SpringToken(dampingRatio = 0.9f, stiffness = 1400f),
            defaultSpatial = SpringToken(dampingRatio = 0.9f, stiffness = 700f),
            slowSpatial = SpringToken(dampingRatio = 0.9f, stiffness = 300f),
            fastEffects = SpringToken(dampingRatio = 1f, stiffness = 3800f),
            defaultEffects = SpringToken(dampingRatio = 1f, stiffness = 1600f),
            slowEffects = SpringToken(dampingRatio = 1f, stiffness = 800f),
            shortDurationMillis = 200,
            mediumDurationMillis = 300,
            easing = StandardEasing,
        )
    }
}

/**
 * True when the system asks for reduced motion (S41-R10): `ValueAnimator.areAnimatorsEnabled()` is false
 * when "Remove animations" (Accessibility) or battery saver turns animations off. On API 33+ it also
 * follows changes while the screen is shown.
 */
@Composable
fun rememberReducedMotion(): Boolean {
    var reducedMotion by remember { mutableStateOf(!ValueAnimator.areAnimatorsEnabled()) }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        DisposableEffect(Unit) {
            val listener = ValueAnimator.DurationScaleChangeListener { scale -> reducedMotion = scale == 0f }
            ValueAnimator.registerDurationScaleChangeListener(listener)
            onDispose { ValueAnimator.unregisterDurationScaleChangeListener(listener) }
        }
    }
    return reducedMotion
}
