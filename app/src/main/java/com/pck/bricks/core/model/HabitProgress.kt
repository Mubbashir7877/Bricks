package com.pck.bricks.core.model

import java.time.LocalDate

data class HabitProgress(
    val progressId: String,
    val habitId: String,
    val currentTier: TierType,
    val totalBricksRequired: Int,
    val completedBrickCount: Int,
    val missedGapCount: Int,
    val consecutiveMissedScheduledDays: Int,
    val currentWallStartDate: LocalDate,
    val lastProcessedDate: LocalDate?,
    val lastCompletedDate: LocalDate?,
    val tierStatus: TierStatus
)
