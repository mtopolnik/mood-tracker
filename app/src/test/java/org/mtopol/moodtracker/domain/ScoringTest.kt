package org.mtopol.moodtracker.domain

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScoringTest {

    private fun answers(vararg v: Int) = v.toList()

    @Test
    fun anxietyIsSumOfFirstSix() {
        val a = answers(1, 2, 3, 0, 1, 2, /* depression */ 3, 3, 3, 3, 3, 3)
        assertEquals(9, anxietyScore(a))
    }

    @Test
    fun depressionIsSumOfLastSix() {
        val a = answers(0, 0, 0, 0, 0, 0, /* depression */ 3, 2, 1, 0, 2, 1)
        assertEquals(9, depressionScore(a))
    }

    @Test
    fun maximaAreEighteen() {
        val all3 = List(QUESTION_COUNT) { 3 }
        assertEquals(MAX_GROUP_SCORE, anxietyScore(all3))
        assertEquals(MAX_GROUP_SCORE, depressionScore(all3))
        assertEquals(18, MAX_GROUP_SCORE)
    }

    @Test
    fun minimaAreZero() {
        val all0 = List(QUESTION_COUNT) { 0 }
        assertEquals(0, anxietyScore(all0))
        assertEquals(0, depressionScore(all0))
    }

    @Test
    fun partialScoreTreatsUnansweredAsZero() {
        // Only a few items answered; the rest still UNANSWERED (-1).
        val partial = MutableList(QUESTION_COUNT) { UNANSWERED }.also {
            it[0] = 2; it[1] = 3 // anxiety group
            it[6] = 1            // depression group
        }
        assertEquals(5, anxietyScore(partial))
        assertEquals(1, depressionScore(partial))
        assertFalse(isComplete(partial))
    }

    @Test
    fun completeOnlyWhenAllAnswered() {
        assertTrue(isComplete(List(QUESTION_COUNT) { 0 }))
        assertTrue(isComplete(List(QUESTION_COUNT) { 3 }))
        assertTrue(isComplete(answers(0, 1, 2, 3, 0, 1, 2, 3, 0, 1, 2, 3)))
    }

    @Test
    fun incompleteWithUnansweredOrOutOfRange() {
        val withUnanswered = MutableList(QUESTION_COUNT) { 1 }.also { it[5] = UNANSWERED }
        assertFalse(isComplete(withUnanswered))

        val outOfRangeHigh = MutableList(QUESTION_COUNT) { 1 }.also { it[0] = 4 }
        assertFalse(isComplete(outOfRangeHigh))

        val outOfRangeLow = MutableList(QUESTION_COUNT) { 1 }.also { it[11] = -2 }
        assertFalse(isComplete(outOfRangeLow))

        assertFalse(isComplete(List(QUESTION_COUNT - 1) { 0 }))
    }

    @Test
    fun groupGapIsBeforeIndexSix() {
        assertTrue(isFirstOfSecondGroup(6))
        (0..11).filter { it != 6 }.forEach { assertFalse(isFirstOfSecondGroup(it)) }
    }
}
