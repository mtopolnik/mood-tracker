package org.mtopol.moodtracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.mtopol.moodtracker.domain.QUESTION_COUNT

/**
 * One questionnaire per day. The primary key is the day's epoch-day
 * ([java.time.LocalDate.toEpochDay]) so range queries and ordering are cheap
 * integer comparisons and there is exactly one row per calendar day (editing
 * is a clean upsert). Answers are 12 flat 0..3 columns; category scores are
 * derived in the domain layer, never stored.
 *
 * [note] is an optional free-form remark on the day. It is nullable (added in
 * schema v2 as a NULL-defaulting column, so every pre-v2 row reads back as a
 * day with no note) and plays no part in scoring or completeness.
 */
@Entity(tableName = "mood_entry")
data class CloudyEntry(
    @PrimaryKey val epochDay: Long,
    val q1: Int, val q2: Int, val q3: Int, val q4: Int, val q5: Int, val q6: Int,
    val q7: Int, val q8: Int, val q9: Int, val q10: Int, val q11: Int, val q12: Int,
    val note: String? = null,
)

fun CloudyEntry.answers(): List<Int> =
    listOf(q1, q2, q3, q4, q5, q6, q7, q8, q9, q10, q11, q12)

fun cloudyEntryOf(epochDay: Long, a: List<Int>, note: String? = null): CloudyEntry {
    require(a.size == QUESTION_COUNT) { "Expected $QUESTION_COUNT answers, got ${a.size}" }
    return CloudyEntry(
        epochDay,
        a[0], a[1], a[2], a[3], a[4], a[5],
        a[6], a[7], a[8], a[9], a[10], a[11],
        note,
    )
}
