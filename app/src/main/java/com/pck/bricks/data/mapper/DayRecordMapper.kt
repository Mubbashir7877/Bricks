package com.pck.bricks.data.mapper

import com.pck.bricks.core.model.HabitDayRecord
import com.pck.bricks.core.model.TaskCompletionRecord
import com.pck.bricks.data.entity.HabitDayRecordEntity
import com.pck.bricks.data.entity.TaskCompletionEntity
import java.time.Instant
import java.time.LocalDate

object DayRecordMapper {

    fun toDomain(entity: HabitDayRecordEntity): HabitDayRecord = HabitDayRecord(
        recordId = entity.recordId,
        habitId = entity.habitId,
        date = LocalDate.ofEpochDay(entity.dateEpochDay),
        wasScheduled = entity.wasScheduled,
        wasCompleted = entity.wasCompleted,
        wasMissed = entity.wasMissed,
        wasProcessed = entity.wasProcessed,
        brickIndex = entity.brickIndex,
        isGap = entity.isGap,
        createdAt = Instant.ofEpochMilli(entity.createdAtEpochMillis)
    )

    fun toEntity(domain: HabitDayRecord): HabitDayRecordEntity = HabitDayRecordEntity(
        recordId = domain.recordId,
        habitId = domain.habitId,
        dateEpochDay = domain.date.toEpochDay(),
        wasScheduled = domain.wasScheduled,
        wasCompleted = domain.wasCompleted,
        wasMissed = domain.wasMissed,
        wasProcessed = domain.wasProcessed,
        brickIndex = domain.brickIndex,
        isGap = domain.isGap,
        createdAtEpochMillis = domain.createdAt.toEpochMilli()
    )

    fun completionToDomain(entity: TaskCompletionEntity): TaskCompletionRecord = TaskCompletionRecord(
        completionId = entity.completionId,
        habitId = entity.habitId,
        taskId = entity.taskId,
        date = LocalDate.ofEpochDay(entity.dateEpochDay),
        isCompleted = entity.isCompleted,
        completedAt = entity.completedAtEpochMillis?.let { Instant.ofEpochMilli(it) }
    )

    fun completionToEntity(domain: TaskCompletionRecord): TaskCompletionEntity = TaskCompletionEntity(
        completionId = domain.completionId,
        habitId = domain.habitId,
        taskId = domain.taskId,
        dateEpochDay = domain.date.toEpochDay(),
        isCompleted = domain.isCompleted,
        completedAtEpochMillis = domain.completedAt?.toEpochMilli()
    )
}
