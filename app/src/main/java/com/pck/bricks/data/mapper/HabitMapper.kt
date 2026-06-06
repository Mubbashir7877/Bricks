package com.pck.bricks.data.mapper

import com.pck.bricks.core.model.Habit
import com.pck.bricks.core.model.HabitTask
import com.pck.bricks.core.model.ScheduleType
import com.pck.bricks.data.entity.HabitEntity
import com.pck.bricks.data.entity.HabitTaskEntity
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime

object HabitMapper {

    fun toDomain(entity: HabitEntity): Habit = Habit(
        habitId = entity.habitId,
        name = entity.name,
        scheduleType = ScheduleType.valueOf(entity.scheduleType),
        selectedWeekdays = parseWeekdays(entity.selectedWeekdaysCsv),
        reminderTime = LocalTime.ofSecondOfDay(entity.reminderTimeMinutes * 60L),
        imagePath = entity.imagePath,
        soundPath = entity.soundPath,
        createdAt = Instant.ofEpochMilli(entity.createdAtEpochMillis),
        isActive = entity.isActive
    )

    fun toEntity(domain: Habit): HabitEntity = HabitEntity(
        habitId = domain.habitId,
        name = domain.name,
        scheduleType = domain.scheduleType.name,
        selectedWeekdaysCsv = domain.selectedWeekdays.joinToString(",") { it.value.toString() },
        reminderTimeMinutes = domain.reminderTime.toSecondOfDay() / 60,
        imagePath = domain.imagePath,
        soundPath = domain.soundPath,
        createdAtEpochMillis = domain.createdAt.toEpochMilli(),
        isActive = domain.isActive
    )

    fun taskToDomain(entity: HabitTaskEntity): HabitTask = HabitTask(
        taskId = entity.taskId,
        habitId = entity.habitId,
        taskName = entity.taskName,
        sortOrder = entity.sortOrder
    )

    fun taskToEntity(domain: HabitTask): HabitTaskEntity = HabitTaskEntity(
        taskId = domain.taskId,
        habitId = domain.habitId,
        taskName = domain.taskName,
        sortOrder = domain.sortOrder
    )

    private fun parseWeekdays(csv: String): Set<DayOfWeek> {
        if (csv.isBlank()) return emptySet()
        return csv.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .mapNotNull { runCatching { DayOfWeek.of(it) }.getOrNull() }
            .toSet()
    }
}
