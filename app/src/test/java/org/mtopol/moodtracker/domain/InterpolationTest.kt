package org.mtopol.moodtracker.domain

import kotlin.math.abs
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InterpolationTest {

    private fun assertValue(expected: Float, point: DailyPoint) {
        val actual = point.value
        assertTrue(actual != null && abs(actual - expected) < 1e-4f, "day ${point.epochDay}: expected $expected, got $actual")
    }

    @Test
    fun emptyInputYieldsAllNull() {
        val series = interpolateDaily(emptyList(), 0, 4)
        assertEquals(5, series.size)
        series.forEach {
            assertNull(it.value)
            assertFalse(it.isReal)
        }
    }

    @Test
    fun singlePointIsTheOnlyNonNullDay() {
        val series = interpolateDaily(listOf(2L to 7), 0, 4)
        assertEquals(5, series.size)
        assertNull(series[0].value)
        assertNull(series[1].value)
        assertValue(7f, series[2])
        assertTrue(series[2].isReal)
        assertNull(series[3].value)
        assertNull(series[4].value)
    }

    @Test
    fun linearSlopeBetweenTwoPoints() {
        val series = interpolateDaily(listOf(0L to 0, 4L to 8), 0, 4)
        assertValue(0f, series[0]); assertTrue(series[0].isReal)
        assertValue(2f, series[1]); assertFalse(series[1].isReal)
        assertValue(4f, series[2])
        assertValue(6f, series[3])
        assertValue(8f, series[4]); assertTrue(series[4].isReal)
    }

    @Test
    fun noExtrapolationBeforeFirstOrAfterLast() {
        val series = interpolateDaily(listOf(2L to 10, 4L to 6), 0, 6)
        assertNull(series[0].value) // before first
        assertNull(series[1].value)
        assertValue(10f, series[2])
        assertValue(8f, series[3]) // midpoint of 10..6
        assertValue(6f, series[4])
        assertNull(series[5].value) // after last
        assertNull(series[6].value)
    }

    @Test
    fun pointsOutsideRangeAreClipped() {
        val series = interpolateDaily(listOf(-5L to 1, 1L to 4, 99L to 9), 0, 3)
        assertEquals(4, series.size)
        assertNull(series[0].value)        // day 0, before first in-range point (day 1)
        assertValue(4f, series[1])         // day 1 real
        assertNull(series[2].value)        // day 2, after last in-range point
        assertNull(series[3].value)
    }

    @Test
    fun multipleGapsInterpolateWithinCorrectBracket() {
        // (0,0) (2,4) (5,10)
        val series = interpolateDaily(listOf(0L to 0, 2L to 4, 5L to 10), 0, 5)
        assertValue(0f, series[0]); assertTrue(series[0].isReal)
        assertValue(2f, series[1])                 // between (0,0)-(2,4)
        assertValue(4f, series[2]); assertTrue(series[2].isReal)
        assertValue(6f, series[3])                 // between (2,4)-(5,10)
        assertValue(8f, series[4])
        assertValue(10f, series[5]); assertTrue(series[5].isReal)
    }

    @Test
    fun invertedRangeYieldsEmpty() {
        assertTrue(interpolateDaily(listOf(1L to 1), 5, 1).isEmpty())
    }
}
