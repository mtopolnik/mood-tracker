package org.mtopol.moodtracker.reminder

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Schedules a single self-rescheduling [ReminderWorker] for the next 20:00
 * local time. Using a unique one-shot (REPLACE) gives precise evening timing;
 * the worker itself decides at fire time whether today is still incomplete.
 */
class ReminderScheduler(private val context: Context) {

    fun ensureScheduled() {
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(REMINDER_TIME)
        if (!next.isAfter(now)) next = next.plusDays(1)

        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(Duration.between(now, next))
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "daily-mood-reminder"
        private val REMINDER_TIME: LocalTime = LocalTime.of(20, 0)
    }
}
