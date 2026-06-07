package com.pck.bricks.features.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.pck.bricks.MainActivity
import com.pck.bricks.R
import com.pck.bricks.features.rollover.MissPolicyResult

class NotificationBuilder(private val context: Context) {

    private val manager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createChannels()
    }

    fun buildReminder(habitName: String, habitId: String): Notification {
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_HABIT_ID, habitId)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            habitId.hashCode(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(context, CHANNEL_REMINDER)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Time to build a brick!")
            .setContentText("Complete your tasks for \"$habitName\"")
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    fun buildMissedDayNotice(habitName: String, result: MissPolicyResult): Notification {
        val body = when (result) {
            is MissPolicyResult.AddGap ->
                "A gap was added to your wall — don't miss again!"
            is MissPolicyResult.ResetTier ->
                "Your ${result.tier.name.lowercase().replaceFirstChar { it.uppercase() }} wall was reset."
            is MissPolicyResult.RevertTier ->
                "You reverted from ${result.fromTier.name.lowercase()} to ${result.toTier.name.lowercase()}."
        }
        return NotificationCompat.Builder(context, CHANNEL_MISSED)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Missed: $habitName")
            .setContentText(body)
            .setAutoCancel(true)
            .build()
    }

    fun showReminder(habitName: String, notifId: Int, habitId: String) {
        manager.notify(notifId, buildReminder(habitName, habitId))
    }

    fun showMissedDayNotice(habitName: String, result: MissPolicyResult, notifId: Int) {
        manager.notify(notifId, buildMissedDayNotice(habitName, result))
    }

    private fun createChannels() {
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_REMINDER, "Habit Reminders", NotificationManager.IMPORTANCE_HIGH)
                .apply {
                    description = "Daily reminders to complete your habit tasks"
                    setSound(null, null)
                }
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_MISSED, "Missed Day Alerts", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Alerts when you miss a scheduled habit day" }
        )
    }

    companion object {
        const val CHANNEL_REMINDER = "bricks_reminder"
        const val CHANNEL_MISSED   = "bricks_missed"
        const val NOTIF_BASE_MISSED = 2000
        const val NOTIF_BASE_REMINDER = 3000
        const val EXTRA_HABIT_ID = "bricks_habit_id"
    }
}
