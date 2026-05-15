package org.mtopol.moodtracker.domain

import java.time.LocalDate
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DateRangeTest {

    private val today = LocalDate.of(2026, 5, 15)
    private val todayEpoch = today.toEpochDay()

    @Test
    fun rangesWithNoDataEndAtTodayAndUseCalendarMath() {
        assertEquals(
            EpochDayRange(today.minusWeeks(1).toEpochDay(), todayEpoch),
            resolveRange(ChartRange.WEEK, today, earliestEpochDay = null),
        )
        assertEquals(
            EpochDayRange(today.minusMonths(1).toEpochDay(), todayEpoch),
            resolveRange(ChartRange.MONTH, today, null),
        )
        assertEquals(
            EpochDayRange(today.minusMonths(3).toEpochDay(), todayEpoch),
            resolveRange(ChartRange.THREE_MONTHS, today, null),
        )
        assertEquals(
            EpochDayRange(today.minusMonths(6).toEpochDay(), todayEpoch),
            resolveRange(ChartRange.SIX_MONTHS, today, null),
        )
        assertEquals(
            EpochDayRange(today.minusYears(1).toEpochDay(), todayEpoch),
            resolveRange(ChartRange.YEAR, today, null),
        )
    }

    @Test
    fun allWithNoDataIsASingleDay() {
        assertEquals(
            EpochDayRange(todayEpoch, todayEpoch),
            resolveRange(ChartRange.ALL, today, null),
        )
    }

    @Test
    fun allStartsAtEarliestEntry() {
        val earliest = today.minusDays(40).toEpochDay()
        assertEquals(
            EpochDayRange(earliest, todayEpoch),
            resolveRange(ChartRange.ALL, today, earliest),
        )
    }

    @Test
    fun startIsClampedToEarliestSoTheAxisIsNeverEmptyOnTheLeft() {
        // Earliest entry is more recent than a 1-year window start.
        val earliest = today.minusDays(10).toEpochDay()
        val result = resolveRange(ChartRange.YEAR, today, earliest)
        assertEquals(EpochDayRange(earliest, todayEpoch), result)
    }

    @Test
    fun windowIsAlwaysValid() {
        ChartRange.entries.forEach { range ->
            val r = resolveRange(range, today, earliestEpochDay = todayEpoch)
            assertTrue(r.startEpochDay <= r.endEpochDay, "invalid window for $range")
        }
    }
}
