package com.pck.bricks.features.notifications.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pck.bricks.BricksApp
import com.pck.bricks.features.notifications.NotificationBuilder

class ReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val habitName = inputData.getString(KEY_HABIT_NAME) ?: return Result.failure()
        val habitId   = inputData.getString(KEY_HABIT_ID)   ?: return Result.failure()
        val app = applicationContext as? BricksApp ?: return Result.failure()
        app.notificationBuilder.showReminder(
            habitName = habitName,
            notifId = NotificationBuilder.NOTIF_BASE_REMINDER + habitId.hashCode()
        )
        return Result.success()
    }

    companion object {
        const val KEY_HABIT_ID   = "habitId"
        const val KEY_HABIT_NAME = "habitName"
    }
}
