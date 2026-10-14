package com.juancholopes.weightrack.feature.session.ui.logger

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ValueWindowTest {

    // SF02-A10 / SF02-R15, S42-A14: with 60 kg the window goes from 50 to 70 kg, 16 segments and 15 marks
    @Test
    fun weightWindow_isCenteredOnTheReference_with20KgAndStopsEvery1_25Kg() {
        val window = weightWindow(60_000)

        assertEquals(ValueWindow(start = 50_000, end = 70_000, step = 1_250), window)
        assertEquals(16, window.segments)
        assertEquals(15, window.marks)
    }

    // SF02-A10 / SF02-R15: with 62,25 kg the window is centered on the closest stop, 62,5 kg
    @Test
    fun weightWindow_centersOnTheClosestStop() {
        assertEquals(62_500, weightCenter(62_250))
        assertEquals(ValueWindow(52_500, 72_500, 1_250), weightWindow(62_250))
        assertEquals(60_000, weightCenter(60_500))
        assertEquals(61_250, weightCenter(60_700))
        assertEquals(ValueWindow(50_000, 70_000, 1_250), weightWindow(60_500))
    }

    // SF02-A10 / SF02-R15: the stops are multiples of 1,25 kg, so the window edges are too
    @Test
    fun weightWindow_alwaysStartsAndEndsOnAStop() {
        for (reference in 0..1_000_000 step 1_750) {
            val window = weightWindow(reference)
            assertEquals("start of $reference", 0, window.start % 1_250)
            assertEquals("end of $reference", 0, window.end % 1_250)
            assertTrue(reference in window.start - 625..window.end + 625)
        }
    }

    // SF02-A10 / SF02-R15: near 0 and near 1000 kg the window is clipped, not shifted
    @Test
    fun weightWindow_isClippedAtTheLimits() {
        assertEquals(ValueWindow(0, 10_000, 1_250), weightWindow(0))
        assertEquals(ValueWindow(0, 15_000, 1_250), weightWindow(5_000))
        assertEquals(ValueWindow(985_000, 1_000_000, 1_250), weightWindow(995_000))
        assertEquals(ValueWindow(990_000, 1_000_000, 1_250), weightWindow(1_000_000))
    }

    // SF02-A10 / SF02-R15: "+" gives 60,25 kg and "−" gives 59,75 kg; they never leave 0 to 1000 kg
    @Test
    fun stepWeight_moves250Grams_withinTheLimits() {
        assertEquals(60_250, stepWeight(60_000, +1))
        assertEquals(59_750, stepWeight(60_000, -1))
        assertEquals(0, stepWeight(0, -1))
        assertEquals(250, stepWeight(0, +1))
        assertEquals(1_000_000, stepWeight(1_000_000, +1))
        assertEquals(999_750, stepWeight(1_000_000, -1))
    }

    // SF02-A10 / SF02-R15: the slider only gives multiples of 1,25 kg
    @Test
    fun snap_givesTheClosestStopOfTheWindow() {
        val window = weightWindow(60_000)

        assertEquals(61_250, window.snap(61_000))
        assertEquals(60_000, window.snap(60_600))
        assertEquals(50_000, window.snap(40_000))
        assertEquals(70_000, window.snap(90_000))
        for (value in 50_000..70_000 step 100) {
            assertEquals("snap of $value", 0, window.snap(value) % 1_250)
        }
    }

    // SF02-A10 / SF02-R15, S42-A14: releasing the slider on 70 kg moves the window to 60 to 80 kg
    @Test
    fun weightWindow_afterReleasingOn70Kg_goesFrom60To80Kg() {
        val dragged = weightWindow(60_000).snap(70_000)

        assertEquals(70_000, dragged)
        assertEquals(ValueWindow(60_000, 80_000, 1_250), weightWindow(dragged))
    }

    // SF02-A10 / SF02-R15: the repetitions use the same window with 20 repetitions and stops of 1
    @Test
    fun repsWindow_isCenteredOnTheReference_andClippedFrom1To999() {
        assertEquals(ValueWindow(5, 25, 1), repsWindow(15))
        assertEquals(10, repsCenter(10))
        assertEquals(ValueWindow(1, 20, 1), repsWindow(10))
        assertEquals(ValueWindow(1, 11, 1), repsWindow(1))
        assertEquals(ValueWindow(989, 999, 1), repsWindow(999))
        assertEquals(20, repsWindow(15).segments)
    }

    // SF02-A10 / SF02-R15: the repetitions move by 1 within 1 to 999
    @Test
    fun stepReps_movesOne_withinTheLimits() {
        assertEquals(11, stepReps(10, +1))
        assertEquals(9, stepReps(10, -1))
        assertEquals(1, stepReps(1, -1))
        assertEquals(999, stepReps(999, +1))
    }

    // SF02-R15: a value is inside the window when it lies between its edges
    @Test
    fun contains_includesTheEdges() {
        val window = weightWindow(60_000)

        assertTrue(50_000 in window)
        assertTrue(70_000 in window)
        assertTrue(70_250 !in window)
    }
}
