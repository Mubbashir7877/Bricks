package com.pck.bricks.features.notifications.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pck.bricks.BricksApp
import com.pck.bricks.features.notifications.NotificationBuilder

class MidnightRolloverWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? BricksApp ?: return Result.failure()
        val rolloverResult = app.dailyRolloverProcessor.processPreviousScheduledDays()

        val notifBuilder = app.notificationBuilder
        rolloverResult.habitResults
            .flatMap { it.notifications }
            .forEachIndexed { index, notif ->
                notifBuilder.showMissedDayNotice(
                    habitName = notif.habitName,
                    result = notif.policyResult,
                    notifId = NotificationBuilder.NOTIF_BASE_MISSED + index
                )
            }

        return Result.success()
    }
}
