package com.pck.bricks.features.rollover

import com.pck.bricks.core.model.HabitProgress
import com.pck.bricks.core.model.TierStatus
import com.pck.bricks.core.model.TierType

class TierTransitionEngine {

    fun canFortify(progress: HabitProgress): Boolean =
        progress.tierStatus == TierStatus.COMPLETED && progress.currentTier != TierType.GOLD

    fun fortify(progress: HabitProgress): HabitProgress {
        val nextTier = when (progress.currentTier) {
            TierType.BRONZE -> TierType.SILVER
            TierType.SILVER -> TierType.GOLD
            TierType.GOLD -> return progress
        }
        return progress.copy(
            currentTier = nextTier,
            totalBricksRequired = bricksForTier(nextTier),
            missedGapCount = 0,
            consecutiveMissedScheduledDays = 0,
            tierStatus = TierStatus.ACTIVE
        )
    }

    fun completeTierIfReady(progress: HabitProgress): HabitProgress {
        val slotsFilled = progress.completedBrickCount + progress.missedGapCount
        return if (slotsFilled >= progress.totalBricksRequired && progress.tierStatus == TierStatus.ACTIVE) {
            progress.copy(tierStatus = TierStatus.COMPLETED)
        } else {
            progress
        }
    }

    fun nextBrickIndex(progress: HabitProgress): Int =
        progress.completedBrickCount + progress.missedGapCount

    fun isWallComplete(progress: HabitProgress): Boolean =
        progress.completedBrickCount + progress.missedGapCount >= progress.totalBricksRequired

    private fun bricksForTier(tier: TierType): Int = when (tier) {
        TierType.BRONZE -> 30
        TierType.SILVER -> 60
        TierType.GOLD -> 90
    }
}
