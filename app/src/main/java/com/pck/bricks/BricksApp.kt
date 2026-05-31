package com.pck.bricks

import android.app.Application
import com.pck.bricks.core.time.ScheduledDayCalculator
import com.pck.bricks.core.time.SystemLocalDateProvider
import com.pck.bricks.data.db.BricksDatabase
import com.pck.bricks.data.repository.HabitRepository
import com.pck.bricks.data.repository.LocalHabitRepository
import com.pck.bricks.features.notifications.MissedDayScheduler
import com.pck.bricks.features.notifications.NotificationBuilder
import com.pck.bricks.features.notifications.ReminderScheduler
import com.pck.bricks.features.notifications.WorkManagerReminderScheduler
import com.pck.bricks.features.rollover.DailyRolloverProcessor
import com.pck.bricks.features.rollover.MissedDayPolicyEngine
import com.pck.bricks.features.rollover.TierTransitionEngine
import com.pck.bricks.features.wall.BrickProgressCalculator
import com.pck.bricks.features.wall.WallRenderer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BricksApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: BricksDatabase by lazy { BricksDatabase.getInstance(this) }

    val habitRepository: HabitRepository by lazy {
        LocalHabitRepository(
            habitDao = database.habitDao(),
            habitTaskDao = database.habitTaskDao(),
            habitProgressDao = database.habitProgressDao(),
            habitDayRecordDao = database.habitDayRecordDao(),
            taskCompletionDao = database.taskCompletionDao()
        )
    }

    val scheduledDayCalculator: ScheduledDayCalculator by lazy { ScheduledDayCalculator() }
    val tierTransitionEngine: TierTransitionEngine by lazy { TierTransitionEngine() }
    val brickProgressCalculator: BrickProgressCalculator by lazy { BrickProgressCalculator() }
    val wallRenderer: WallRenderer by lazy { WallRenderer() }
    val notificationBuilder: NotificationBuilder by lazy { NotificationBuilder(this) }
    val reminderScheduler: ReminderScheduler by lazy { WorkManagerReminderScheduler(this) }

    val dailyRolloverProcessor: DailyRolloverProcessor by lazy {
        DailyRolloverProcessor(
            habitRepository = habitRepository,
            scheduledDayCalculator = scheduledDayCalculator,
            missedDayPolicyEngine = MissedDayPolicyEngine(),
            tierTransitionEngine = tierTransitionEngine,
            dateProvider = SystemLocalDateProvider()
        )
    }

    val missedDayScheduler: MissedDayScheduler by lazy { MissedDayScheduler(this) }

    override fun onCreate() {
        super.onCreate()
        missedDayScheduler.scheduleMidnightProcessing()
        // Reconcile any missed days that elapsed while the app was closed
        applicationScope.launch {
            dailyRolloverProcessor.processPreviousScheduledDays()
        }
    }
}
