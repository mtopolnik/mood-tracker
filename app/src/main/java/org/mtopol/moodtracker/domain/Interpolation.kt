package org.mtopol.moodtracker.domain

/**
 * One day on the chart's X axis. [value] is `null` where there is no data —
 * i.e. before the first or after the last real entry within the range. A day
 * that comes from a saved entry has [isReal] = true; a linearly interpolated
 * day has [isReal] = false.
 */
data class DailyPoint(
    val epochDay: Long,
    val value: Float?,
    val isReal: Boolean,
)

/**
 * Builds a per-day series spanning `[rangeStartEpochDay, rangeEndEpochDay]`
 * (inclusive) from sparse real [points] (epochDay → score).
 *
 * Rules:
 *  - Points outside the range are ignored; the rest are sorted ascending.
 *  - A day matching a real point gets that score ([isReal] = true).
 *  - A day strictly between two real points is linearly interpolated.
 *  - Days before the first / after the last real point are left `null`
 *    (no extrapolation), so the rendered line starts at the first real day
 *    and ends at the last.
 *  - A single real point yields exactly one non-null day (renders as a lone
 *    marker); empty input yields an all-`null` series.
 *
 * Pure (no Android / framework imports) — directly unit-testable.
 */
fun interpolateDaily(
    points: List<Pair<Long, Int>>,
    rangeStartEpochDay: Long,
    rangeEndEpochDay: Long,
): List<DailyPoint> {
    if (rangeEndEpochDay < rangeStartEpochDay) return emptyList()

    val inRange = points
        .filter { it.first in rangeStartEpochDay..rangeEndEpochDay }
        .sortedBy { it.first }

    val size = (rangeEndEpochDay - rangeStartEpochDay + 1).toInt()
    val result = ArrayList<DailyPoint>(size)

    if (inRange.isEmpty()) {
        var d = rangeStartEpochDay
        while (d <= rangeEndEpochDay) {
            result.add(DailyPoint(d, null, false))
            d++
        }
        return result
    }

    val firstReal = inRange.first().first
    val lastReal = inRange.last().first
    val byDay = HashMap<Long, Int>(inRange.size * 2)
    for ((day, score) in inRange) byDay[day] = score

    var seg = 0 // lower bracket index: inRange[seg].first < d < inRange[seg + 1].first
    var d = rangeStartEpochDay
    while (d <= rangeEndEpochDay) {
        val score = byDay[d]
        when {
            d < firstReal || d > lastReal -> result.add(DailyPoint(d, null, false))
            score != null -> result.add(DailyPoint(d, score.toFloat(), true))
            else -> {
                while (seg < inRange.size - 1 && inRange[seg + 1].first <= d) seg++
                val (x0, y0) = inRange[seg]
                val (x1, y1) = inRange[seg + 1]
                val t = (d - x0).toFloat() / (x1 - x0).toFloat()
                result.add(DailyPoint(d, y0 + (y1 - y0) * t, false))
            }
        }
        d++
    }
    return result
}
