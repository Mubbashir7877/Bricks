package com.pck.bricks.features.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.pck.bricks.features.notifications.workers.MidnightRolloverWorker
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

class MissedDayScheduler(private val context: Context) {

    fun scheduleMidnightProcessing() {
        val now = LocalDateTime.now()
        val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay()
        val initialDelayMillis = ChronoUnit.MILLIS.between(now, nextMidnight)

        val request = PeriodicWorkRequestBuilder<MidnightRolloverWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
            .addTag(TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            TAG,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    companion object {
        private const val TAG = "midnight_rollover"
    }
}
