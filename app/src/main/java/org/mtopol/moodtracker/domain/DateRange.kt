package org.mtopol.moodtracker.domain

import java.time.LocalDate

/** Selectable time spans for the Trends chart. [label] is shown on the chip. */
enum class ChartRange(val label: String) {
    WEEK("1W"),
    MONTH("1M"),
    THREE_MONTHS("3M"),
    SIX_MONTHS("6M"),
    YEAR("1Y"),
    ALL("All"),
}

/** Inclusive epoch-day window. */
data class EpochDayRange(val startEpochDay: Long, val endEpochDay: Long)

/**
 * Resolves [range] to an inclusive window ending at [today]. [earliestEpochDay]
 * is the oldest stored entry (or `null` if there is none); it clamps the start
 * so the axis never extends before the first real data. The window is always
 * valid (start ≤ end), even with no data.
 */
fun resolveRange(
    range: ChartRange,
    today: LocalDate,
    earliestEpochDay: Long?,
): EpochDayRange {
    val end = today.toEpochDay()
    val rawStart = when (range) {
        ChartRange.WEEK -> today.minusWeeks(1)
        ChartRange.MONTH -> today.minusMonths(1)
        ChartRange.THREE_MONTHS -> today.minusMonths(3)
        ChartRange.SIX_MONTHS -> today.minusMonths(6)
        ChartRange.YEAR -> today.minusYears(1)
        ChartRange.ALL -> LocalDate.ofEpochDay(earliestEpochDay ?: end)
    }.toEpochDay()

    val start = if (earliestEpochDay != null) maxOf(rawStart, earliestEpochDay) else rawStart
    return EpochDayRange(minOf(start, end), end)
}
