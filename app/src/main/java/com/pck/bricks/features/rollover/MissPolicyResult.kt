package com.pck.bricks.features.rollover

import com.pck.bricks.core.model.HabitProgress
import com.pck.bricks.core.model.TierType

sealed class MissPolicyResult {
    data class AddGap(val updatedProgress: HabitProgress) : MissPolicyResult()
    data class ResetTier(val updatedProgress: HabitProgress, val tier: TierType) : MissPolicyResult()
    data class RevertTier(
        val updatedProgress: HabitProgress,
        val fromTier: TierType,
        val toTier: TierType
    ) : MissPolicyResult()
}
