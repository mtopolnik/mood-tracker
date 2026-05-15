package org.mtopol.moodtracker.reminder

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.mtopol.moodtracker.data.MoodDatabase
import org.mtopol.moodtracker.data.MoodRepository
import java.time.LocalDate

/**
 * Runs once per scheduled evening: posts the reminder only if today is not
 * yet complete (no row, or a partially answered one), then re-arms itself for
 * the next day. Fully offline.
 */
class ReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val repo = MoodRepository(MoodDatabase.get(applicationContext).moodDao())
        if (repo.getDay(LocalDate.now())?.isComplete != true) {
            Notifications.show(applicationContext)
        }
        // Self-perpetuate: enqueue tomorrow's check (REPLACE keeps a single job).
        ReminderScheduler(applicationContext).ensureScheduled()
        return Result.success()
    }
}
