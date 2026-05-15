package org.mtopol.moodtracker.domain

import org.mtopol.moodtracker.R

/**
 * String resource ids for the [QUESTION_COUNT] questionnaire items, in fixed
 * display order. Indices 0..5 are the first group, 6..11 the second — but the
 * grouping is never surfaced as a label (see [isFirstOfSecondGroup]).
 */
val QUESTION_LABELS: List<Int> = listOf(
    R.string.question_1, R.string.question_2, R.string.question_3,
    R.string.question_4, R.string.question_5, R.string.question_6,
    R.string.question_7, R.string.question_8, R.string.question_9,
    R.string.question_10, R.string.question_11, R.string.question_12,
)
