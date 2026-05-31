package com.pck.bricks.features.wall

import com.pck.bricks.core.model.HabitProgress
import com.pck.bricks.core.model.TierType

class BrickProgressCalculator {

    fun nextBrickIndex(progress: HabitProgress): Int =
        progress.completedBrickCount + progress.missedGapCount

    fun isWallComplete(progress: HabitProgress): Boolean =
        progress.completedBrickCount + progress.missedGapCount >= progress.totalBricksRequired

    fun visibleDividerAlpha(tier: TierType): Float = when (tier) {
        TierType.BRONZE -> 0f
        TierType.SILVER -> 0.4f
        TierType.GOLD -> 0.8f
    }
}
