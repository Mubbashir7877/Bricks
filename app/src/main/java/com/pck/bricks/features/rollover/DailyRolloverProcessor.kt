package com.pck.bricks.features.rollover

import com.pck.bricks.core.model.HabitDayRecord
import com.pck.bricks.core.time.LocalDateProvider
import com.pck.bricks.core.time.ScheduledDayCalculator
import com.pck.bricks.data.repository.HabitRepository
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class MissedDayNotification(
    val habitName: String,
    val policyResult: MissPolicyResult
)

data class HabitRolloverResult(
    val habitId: String,
    val processedDates: List<LocalDate>,
    val missedDates: List<LocalDate>,
    val notifications: List<MissedDayNotification> = emptyList()
)

data class RolloverResult(val habitResults: List<HabitRolloverResult>)

class DailyRolloverProcessor(
    private val habitRepository: HabitRepository,
    private val scheduledDayCalculator: ScheduledDayCalculator,
    private val missedDayPolicyEngine: MissedDayPolicyEngine,
    private val tierTransitionEngine: TierTransitionEngine,
    private val dateProvider: LocalDateProvider
) {

    suspend fun processPreviousScheduledDays(): RolloverResult {
        val today = dateProvider.today()
        val habits = habitRepository.getActiveHabitsOnce()
        val results = habits.map { habit ->
            processHabitForDate(habit.habitId, today)
        }
        return RolloverResult(results)
    }

    suspend fun processHabitForDate(habitId: String, today: LocalDate): HabitRolloverResult {
        val processed = mutableListOf<LocalDate>()
        val missed = mutableListOf<LocalDate>()
        val notifications = mutableListOf<MissedDayNotification>()

        val habit = habitRepository.getActiveHabitsOnce().firstOrNull { it.habitId == habitId }
            ?: return HabitRolloverResult(habitId, processed, missed)

        val progress = habitRepository.getProgressOnce(habitId)
            ?: return HabitRolloverResult(habitId, processed, missed)

        val startDate = progress.lastProcessedDate?.plusDays(1) ?: progress.currentWallStartDate
        val scheduledDays = scheduledDayCalculator.scheduledDaysBetween(habit, startDate, today)

        var currentProgress = progress

        for (date in scheduledDays) {
            val existingRecord = habitRepository.getDayRecord(habitId, date)

            when {
                existingRecord?.wasProcessed == true -> {
                    // Already handled — skip
                }
                existingRecord?.wasCompleted == true -> {
                    habitRepository.upsertDayRecord(existingRecord.copy(wasProcessed = true))
                    currentProgress = currentProgress.copy(lastProcessedDate = date)
                    processed += date
                }
                else -> {
                    val gapIndex = tierTransitionEngine.nextBrickIndex(currentProgress)
                    habitRepository.upsertDayRecord(
                        HabitDayRecord(
                            recordId = existingRecord?.recordId ?: UUID.randomUUID().toString(),
                            habitId = habitId,
                            date = date,
                            wasScheduled = true,
                            wasCompleted = false,
                            wasMissed = true,
                            wasProcessed = true,
                            brickIndex = gapIndex,
                            isGap = true,
                            createdAt = Instant.now()
                        )
                    )

                    val policyResult = missedDayPolicyEngine.applyMiss(
                        currentProgress,
                        habit.scheduleType,
                        date
                    )
                    currentProgress = when (policyResult) {
                        is MissPolicyResult.AddGap    -> policyResult.updatedProgress
                        is MissPolicyResult.ResetTier -> policyResult.updatedProgress
                        is MissPolicyResult.RevertTier -> policyResult.updatedProgress
                    }
                    notifications += MissedDayNotification(habit.name, policyResult)
                    missed += date
                }
            }
        }

        if (currentProgress != progress) {
            habitRepository.saveProgress(currentProgress)
        }

        return HabitRolloverResult(habitId, processed, missed, notifications)
    }
}
