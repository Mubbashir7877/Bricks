package com.pck.bricks

import android.app.Application
import com.pck.bricks.core.time.ScheduledDayCalculator
import com.pck.bricks.core.time.SystemLocalDateProvider
import com.pck.bricks.data.db.BricksDatabase
import com.pck.bricks.data.repository.HabitRepository
import com.pck.bricks.data.repository.LocalHabitRepository
import com.pck.bricks.features.rollover.DailyRolloverProcessor
import com.pck.bricks.features.rollover.MissedDayPolicyEngine
import com.pck.bricks.features.rollover.TierTransitionEngine

class BricksApp : Application() {

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

    val tierTransitionEngine: TierTransitionEngine by lazy { TierTransitionEngine() }

    val dailyRolloverProcessor: DailyRolloverProcessor by lazy {
        DailyRolloverProcessor(
            habitRepository = habitRepository,
            scheduledDayCalculator = ScheduledDayCalculator(),
            missedDayPolicyEngine = MissedDayPolicyEngine(),
            tierTransitionEngine = tierTransitionEngine,
            dateProvider = SystemLocalDateProvider()
        )
    }
}
