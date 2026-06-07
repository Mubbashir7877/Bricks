package com.pck.bricks.features.notifications.workers

import android.content.Context
import android.media.MediaPlayer
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pck.bricks.BricksApp
import com.pck.bricks.R
import com.pck.bricks.features.notifications.NotificationBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.time.LocalDate
import kotlin.coroutines.resume

class ReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val habitName = inputData.getString(KEY_HABIT_NAME) ?: return Result.failure()
        val habitId   = inputData.getString(KEY_HABIT_ID)   ?: return Result.failure()
        val soundPath = inputData.getString(KEY_SOUND_PATH)
        val app = applicationContext as? BricksApp ?: return Result.failure()

        val progress = app.habitRepository.getProgressOnce(habitId)
        if (progress?.lastCompletedDate == LocalDate.now()) return Result.success()

        app.notificationBuilder.showReminder(
            habitName = habitName,
            notifId = NotificationBuilder.NOTIF_BASE_REMINDER + habitId.hashCode(),
            habitId = habitId
        )
        playSound(soundPath)
        return Result.success()
    }

    private suspend fun playSound(soundPath: String?) = withContext(Dispatchers.IO) {
        runCatching {
            val mp = MediaPlayer()
            suspendCancellableCoroutine { cont ->
                mp.setOnCompletionListener { mp.release(); cont.resume(Unit) }
                mp.setOnErrorListener { _, _, _ -> mp.release(); cont.resume(Unit); true }
                try {
                    if (soundPath != null) {
                        mp.setDataSource(soundPath)
                    } else {
                        val afd = applicationContext.resources.openRawResourceFd(R.raw.default_notification)
                        mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                        afd.close()
                    }
                    mp.prepare()
                    mp.start()
                } catch (e: Exception) {
                    mp.release()
                    cont.resume(Unit)
                }
                cont.invokeOnCancellation { runCatching { mp.stop(); mp.release() } }
            }
        }
    }

    companion object {
        const val KEY_HABIT_ID   = "habitId"
        const val KEY_HABIT_NAME = "habitName"
        const val KEY_SOUND_PATH = "soundPath"
    }
}
