package com.pck.bricks.features.rollover

import com.pck.bricks.core.model.HabitProgress
import com.pck.bricks.core.model.ScheduleType
import com.pck.bricks.core.model.TierStatus
import com.pck.bricks.core.model.TierType
import java.time.LocalDate

class MissedDayPolicyEngine {

    /**
     * Applies one missed scheduled day to the given progress and returns what happened.
     *
     * Bronze daily  : up to 2 consecutive misses allowed (gaps); 3rd consecutive → reset.
     * Bronze weekday: any single miss → reset immediately.
     *
     * Silver daily  : first ever miss → gap; after a prior gap, 3+ consecutive → revert to Bronze;
     *                 any second non-consecutive miss → reset Silver.
     * Silver weekday: any miss → reset Silver immediately.
     *
     * Gold daily    : any miss → revert to Silver.
     * Gold weekday  : any miss → revert to Silver.
     */
    fun applyMiss(
        progress: HabitProgress,
        scheduleType: ScheduleType,
        today: LocalDate
    ): MissPolicyResult {
        return when (progress.currentTier) {
            TierType.BRONZE -> applyBronzeMiss(progress, scheduleType, today)
            TierType.SILVER -> applySilverMiss(progress, scheduleType, today)
            TierType.GOLD -> applyGoldMiss(progress, today)
        }
    }

    private fun applyBronzeMiss(
        progress: HabitProgress,
        scheduleType: ScheduleType,
        today: LocalDate
    ): MissPolicyResult {
        if (scheduleType == ScheduleType.SPECIFIC_WEEKDAYS) {
            return MissPolicyResult.ResetTier(resetBronze(progress, today), TierType.BRONZE)
        }

        // Daily: up to 2 consecutive misses allowed
        val newConsecutive = progress.consecutiveMissedScheduledDays + 1
        return if (newConsecutive <= 2) {
            MissPolicyResult.AddGap(
                progress.copy(
                    missedGapCount = progress.missedGapCount + 1,
                    consecutiveMissedScheduledDays = newConsecutive,
                    lastProcessedDate = today
                )
            )
        } else {
            MissPolicyResult.ResetTier(resetBronze(progress, today), TierType.BRONZE)
        }
    }

    private fun applySilverMiss(
        progress: HabitProgress,
        scheduleType: ScheduleType,
        today: LocalDate
    ): MissPolicyResult {
        if (scheduleType == ScheduleType.SPECIFIC_WEEKDAYS) {
            return MissPolicyResult.ResetTier(resetSilver(progress, today), TierType.SILVER)
        }

        // Daily Silver
        return if (progress.missedGapCount == 0) {
            // First ever miss in this Silver wall: create gap
            MissPolicyResult.AddGap(
                progress.copy(
                    missedGapCount = 1,
                    consecutiveMissedScheduledDays = 1,
                    lastProcessedDate = today
                )
            )
        } else {
            // Already had a prior gap
            val newConsecutive = progress.consecutiveMissedScheduledDays + 1
            if (newConsecutive >= 3) {
                // 3+ consecutive misses after a prior gap → revert to Bronze
                MissPolicyResult.RevertTier(
                    revertToBronze(progress, today),
                    fromTier = TierType.SILVER,
                    toTier = TierType.BRONZE
                )
            } else {
                // Second miss (non-consecutive or 2nd consecutive) → reset Silver
                MissPolicyResult.ResetTier(resetSilver(progress, today), TierType.SILVER)
            }
        }
    }

    private fun applyGoldMiss(progress: HabitProgress, today: LocalDate): MissPolicyResult {
        // Any miss in Gold reverts to Silver
        return MissPolicyResult.RevertTier(
            revertToSilver(progress, today),
            fromTier = TierType.GOLD,
            toTier = TierType.SILVER
        )
    }

    private fun resetBronze(progress: HabitProgress, today: LocalDate): HabitProgress =
        progress.copy(
            currentTier = TierType.BRONZE,
            totalBricksRequired = 30,
            completedBrickCount = 0,
            missedGapCount = 0,
            consecutiveMissedScheduledDays = 0,
            currentWallStartDate = today,
            lastProcessedDate = today,
            tierStatus = TierStatus.ACTIVE
        )

    private fun resetSilver(progress: HabitProgress, today: LocalDate): HabitProgress =
        progress.copy(
            currentTier = TierType.SILVER,
            totalBricksRequired = 60,
            completedBrickCount = 30,
            missedGapCount = 0,
            consecutiveMissedScheduledDays = 0,
            currentWallStartDate = today,
            lastProcessedDate = today,
            tierStatus = TierStatus.ACTIVE
        )

    private fun revertToBronze(progress: HabitProgress, today: LocalDate): HabitProgress =
        progress.copy(
            currentTier = TierType.BRONZE,
            totalBricksRequired = 30,
            completedBrickCount = 0,
            missedGapCount = 0,
            consecutiveMissedScheduledDays = 0,
            currentWallStartDate = today,
            lastProcessedDate = today,
            tierStatus = TierStatus.ACTIVE
        )

    private fun revertToSilver(progress: HabitProgress, today: LocalDate): HabitProgress =
        progress.copy(
            currentTier = TierType.SILVER,
            totalBricksRequired = 60,
            completedBrickCount = 30,
            missedGapCount = 0,
            consecutiveMissedScheduledDays = 0,
            currentWallStartDate = today,
            lastProcessedDate = today,
            tierStatus = TierStatus.ACTIVE
        )
}
