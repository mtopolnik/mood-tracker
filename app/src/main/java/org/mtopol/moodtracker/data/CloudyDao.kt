package org.mtopol.moodtracker.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CloudyDao {

    /** Insert today's (or a past day's) answers, replacing any existing row. */
    @Upsert
    suspend fun upsert(entry: CloudyEntry)

    /** Every stored day, oldest first — the source for a full export. */
    @Query("SELECT * FROM mood_entry ORDER BY epochDay ASC")
    suspend fun getAll(): List<CloudyEntry>

    /** Bulk upsert used by import/restore; same-day rows are replaced. */
    @Upsert
    suspend fun upsertAll(entries: List<CloudyEntry>)

    @Query("SELECT * FROM mood_entry WHERE epochDay = :epochDay LIMIT 1")
    suspend fun getByDay(epochDay: Long): CloudyEntry?

    /** Ascending, inclusive range. Emits again whenever the table changes. */
    @Query("SELECT * FROM mood_entry WHERE epochDay BETWEEN :startEpochDay AND :endEpochDay ORDER BY epochDay ASC")
    fun observeRange(startEpochDay: Long, endEpochDay: Long): Flow<List<CloudyEntry>>

    /** Oldest stored day, or null when the table is empty. */
    @Query("SELECT MIN(epochDay) FROM mood_entry")
    fun observeMinEpochDay(): Flow<Long?>
}
