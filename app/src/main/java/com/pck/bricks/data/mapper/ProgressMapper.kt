package com.pck.bricks.data.mapper

import com.pck.bricks.core.model.HabitProgress
import com.pck.bricks.core.model.TierStatus
import com.pck.bricks.core.model.TierType
import com.pck.bricks.data.entity.HabitProgressEntity
import java.time.LocalDate

object ProgressMapper {

    fun toDomain(entity: HabitProgressEntity): HabitProgress = HabitProgress(
        progressId = entity.progressId,
        habitId = entity.habitId,
        currentTier = TierType.valueOf(entity.currentTier),
        totalBricksRequired = entity.totalBricksRequired,
        completedBrickCount = entity.completedBrickCount,
        missedGapCount = entity.missedGapCount,
        consecutiveMissedScheduledDays = entity.consecutiveMissedScheduledDays,
        currentWallStartDate = LocalDate.ofEpochDay(entity.currentWallStartDateEpochDay),
        lastProcessedDate = entity.lastProcessedDateEpochDay?.let { LocalDate.ofEpochDay(it) },
        lastCompletedDate = entity.lastCompletedDateEpochDay?.let { LocalDate.ofEpochDay(it) },
        tierStatus = TierStatus.valueOf(entity.tierStatus)
    )

    fun toEntity(domain: HabitProgress): HabitProgressEntity = HabitProgressEntity(
        progressId = domain.progressId,
        habitId = domain.habitId,
        currentTier = domain.currentTier.name,
        totalBricksRequired = domain.totalBricksRequired,
        completedBrickCount = domain.completedBrickCount,
        missedGapCount = domain.missedGapCount,
        consecutiveMissedScheduledDays = domain.consecutiveMissedScheduledDays,
        currentWallStartDateEpochDay = domain.currentWallStartDate.toEpochDay(),
        lastProcessedDateEpochDay = domain.lastProcessedDate?.toEpochDay(),
        lastCompletedDateEpochDay = domain.lastCompletedDate?.toEpochDay(),
        tierStatus = domain.tierStatus.name
    )
}
