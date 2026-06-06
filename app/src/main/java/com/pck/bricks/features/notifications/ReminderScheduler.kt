package com.pck.bricks.features.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.pck.bricks.core.model.Habit
import com.pck.bricks.features.notifications.workers.ReminderWorker
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

interface ReminderScheduler {
    fun scheduleHabitReminder(habit: Habit)
    fun cancelHabitReminder(habitId: String)
}

class WorkManagerReminderScheduler(private val context: Context) : ReminderScheduler {

    override fun scheduleHabitReminder(habit: Habit) {
        val now = LocalDateTime.now()
        val reminderToday = now.toLocalDate().atTime(habit.reminderTime)
        val nextReminder = if (reminderToday.isAfter(now)) reminderToday else reminderToday.plusDays(1)
        val initialDelayMillis = ChronoUnit.MILLIS.between(now, nextReminder)

        val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
            .setInputData(
                workDataOf(
                    ReminderWorker.KEY_HABIT_ID   to habit.habitId,
                    ReminderWorker.KEY_HABIT_NAME to habit.name,
                    ReminderWorker.KEY_SOUND_PATH to habit.soundPath
                )
            )
            .addTag(TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            uniqueName(habit.habitId),
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    override fun cancelHabitReminder(habitId: String) {
        WorkManager.getInstance(context).cancelUniqueWork(uniqueName(habitId))
    }

    private fun uniqueName(habitId: String) = "reminder_$habitId"

    companion object {
        private const val TAG = "habit_reminder"
    }
}
