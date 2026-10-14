package com.juancholopes.weightrack.feature.session.ui.logger

import com.juancholopes.weightrack.feature.session.domain.REPS_MAX
import com.juancholopes.weightrack.feature.session.domain.WEIGHT_MAX_GRAMS
import com.juancholopes.weightrack.feature.session.domain.WEIGHT_STEP_GRAMS

/*
 * The pure logic of the weight and repetitions control (SF02-R15, S42-R18, Spec 42 "Control del peso y las
 * repeticiones"), tested on the JVM. The window and stop sizes are adjustable tokens, not rules (S41-R19).
 */

/** The weight slider covers 20 kg (Spec 42, D7). */
const val WEIGHT_WINDOW_SPAN_GRAMS = 20_000

/** The weight slider stops every 1,25 kg: the gross adjustment with the usual plates (Spec 42, D7). */
const val WEIGHT_STOP_GRAMS = 1_250

/** The repetitions slider covers 20 repetitions. */
const val REPS_WINDOW_SPAN = 20

/** The repetitions slider stops on every repetition. */
const val REPS_STOP = 1

const val REPS_MIN = 1

/** Where the weight window is centered while the weight is not fixed (Spec 42, "Sin valor propuesto"): 20 kg. */
const val WEIGHT_DEFAULT_REFERENCE_GRAMS = 20_000

/** Where the repetitions window is centered while the repetitions are not fixed: 10. */
const val REPS_DEFAULT_REFERENCE = 10

/**
 * The range the slider shows: from [start] to [end], with a stop every [step]. [segments] is the number of
 * intervals (16 for a 20 kg window) and [marks] the number of intermediate marks, which is the `steps` of a
 * Material `Slider`. The values are grams or repetitions.
 */
data class ValueWindow(val start: Int, val end: Int, val step: Int) {

    val segments: Int get() = (end - start) / step

    val marks: Int get() = (segments - 1).coerceAtLeast(0)

    operator fun contains(value: Int): Boolean = value in start..end

    /** The stop of the window closest to [value]; a value outside the window gives the nearest edge. */
    fun snap(value: Int): Int {
        val clamped = value.coerceIn(start, end)
        val stops = (clamped - start + step / 2) / step
        return (start + stops * step).coerceAtMost(end)
    }
}

/**
 * The window of the weight slider (S42-R18): 20 kg centered on the stop of 1250 g closest to [referenceGrams] and
 * clipped to 0..1 000 000 g, so near the limits it is shorter, never shifted. The edges are always stops.
 */
fun weightWindow(referenceGrams: Int): ValueWindow {
    val half = WEIGHT_WINDOW_SPAN_GRAMS / 2
    val center = weightCenter(referenceGrams)
    return ValueWindow(
        start = (center - half).coerceAtLeast(0),
        end = (center + half).coerceAtMost(WEIGHT_MAX_GRAMS),
        step = WEIGHT_STOP_GRAMS,
    )
}

/** The stop of 1250 g closest to [referenceGrams] (ties go up), inside 0..1 000 000 g. */
fun weightCenter(referenceGrams: Int): Int {
    val clamped = referenceGrams.coerceIn(0, WEIGHT_MAX_GRAMS)
    return ((clamped + WEIGHT_STOP_GRAMS / 2) / WEIGHT_STOP_GRAMS * WEIGHT_STOP_GRAMS).coerceAtMost(WEIGHT_MAX_GRAMS)
}

/** The window of the repetitions slider: 20 repetitions centered on [reference], clipped to 1..999. */
fun repsWindow(reference: Int): ValueWindow {
    val half = REPS_WINDOW_SPAN / 2
    val center = repsCenter(reference)
    return ValueWindow(
        start = (center - half).coerceAtLeast(REPS_MIN),
        end = (center + half).coerceAtMost(REPS_MAX),
        step = REPS_STOP,
    )
}

/** [reference] inside 1..999. */
fun repsCenter(reference: Int): Int = reference.coerceIn(REPS_MIN, REPS_MAX)

/** The "−" and "+" buttons of the weight: [direction] times 0,25 kg, never outside 0..1 000 000 g (S42-R19). */
fun stepWeight(grams: Int, direction: Int): Int =
    (grams + direction * WEIGHT_STEP_GRAMS).coerceIn(0, WEIGHT_MAX_GRAMS)

/** The "−" and "+" buttons of the repetitions: [direction] repetitions, never outside 1..999. */
fun stepReps(reps: Int, direction: Int): Int = (reps + direction).coerceIn(REPS_MIN, REPS_MAX)
