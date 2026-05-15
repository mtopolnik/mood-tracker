package org.mtopol.moodtracker.domain

/**
 * Pure scoring logic. No Android dependencies so it is directly unit-testable.
 *
 * The questionnaire has [QUESTION_COUNT] items, each answered 0..[MAX_ANSWER].
 * Items are split into two equal groups of [GROUP_SIZE]: the first group's sum
 * is the anxiety score, the second group's sum is the depression score. Each
 * category therefore ranges 0..[MAX_GROUP_SCORE].
 */

const val QUESTION_COUNT = 12
const val GROUP_SIZE = 6
const val MAX_ANSWER = 3
const val MAX_GROUP_SCORE = GROUP_SIZE * MAX_ANSWER // 18
const val UNANSWERED = -1

/**
 * Sum of the first group. [UNANSWERED] (and any negative sentinel) counts as 0,
 * so this doubles as a *running* score for a partially answered day — it grows
 * as items are filled in and equals the final score once [isComplete].
 */
fun anxietyScore(answers: List<Int>): Int =
    (0 until GROUP_SIZE).sumOf { answers[it].coerceAtLeast(0) }

/** Sum of the second group, with the same partial-safe semantics as [anxietyScore]. */
fun depressionScore(answers: List<Int>): Int =
    (GROUP_SIZE until QUESTION_COUNT).sumOf { answers[it].coerceAtLeast(0) }

/** A day "counts" only when every item has a valid 0..[MAX_ANSWER] answer. */
fun isComplete(answers: List<Int>): Boolean =
    answers.size == QUESTION_COUNT && answers.all { it in 0..MAX_ANSWER }

/**
 * True for the first item of the second group. The UI puts a larger vertical
 * gap *before* this item — and nothing else (no label, no divider) — so the
 * two groups are visually separated without being named.
 */
fun isFirstOfSecondGroup(index: Int): Boolean = index == GROUP_SIZE
