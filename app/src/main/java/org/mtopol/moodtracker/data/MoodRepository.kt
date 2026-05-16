package org.mtopol.moodtracker.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.mtopol.moodtracker.domain.anxietyScore
import org.mtopol.moodtracker.domain.depressionScore
import org.mtopol.moodtracker.domain.isComplete
import java.time.LocalDate

/**
 * A stored day mapped into the domain. Rows are now persisted on every choice,
 * so a row may be partial: [isComplete] is the derived "this day counts" flag,
 * and [anxiety]/[depression] are running scores (equal to the final score once
 * [isComplete]). Scores and completeness are derived here, never stored.
 */
data class DayRecord(
    val date: LocalDate,
    val answers: List<Int>,
    val isComplete: Boolean,
    val anxiety: Int,
    val depression: Int,
)

private fun MoodEntry.toDayRecord(): DayRecord {
    val a = answers()
    return DayRecord(
        date = LocalDate.ofEpochDay(epochDay),
        answers = a,
        isComplete = isComplete(a),
        anxiety = anxietyScore(a),
        depression = depressionScore(a),
    )
}

/**
 * The single boundary between Room and the rest of the app. Converts
 * [LocalDate] ↔ epoch-day and [MoodEntry] ↔ [DayRecord]. Read APIs return
 * Flows so the UI updates automatically after any save.
 */
class MoodRepository(private val dao: MoodDao) {

    suspend fun getDay(date: LocalDate): DayRecord? =
        dao.getByDay(date.toEpochDay())?.toDayRecord()

    fun observeRange(startEpochDay: Long, endEpochDay: Long): Flow<List<DayRecord>> =
        dao.observeRange(startEpochDay, endEpochDay)
            .map { rows -> rows.map(MoodEntry::toDayRecord) }

    fun observeEarliestEpochDay(): Flow<Long?> = dao.observeMinEpochDay()

    suspend fun upsert(date: LocalDate, answers: List<Int>) =
        dao.upsert(moodEntryOf(date.toEpochDay(), answers))

    /** All stored days as the portable backup model (oldest first). */
    suspend fun exportDays(): List<BackupDay> =
        dao.getAll().map { BackupDay(LocalDate.ofEpochDay(it.epochDay), it.answers()) }

    /**
     * Restores a backup. A bulk upsert keyed by epoch-day means a day present in
     * both the file and the DB is overwritten by the file, while days only on
     * the device are untouched — correct for both "fresh phone" and "restore
     * over existing". Returns the number of days written.
     */
    suspend fun importDays(days: List<BackupDay>): Int {
        dao.upsertAll(days.map { moodEntryOf(it.date.toEpochDay(), it.answers) })
        return days.size
    }
}
