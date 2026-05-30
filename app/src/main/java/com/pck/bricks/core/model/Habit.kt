package com.pck.bricks.core.model

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime

data class Habit(
    val habitId: String,
    val name: String,
    val scheduleType: ScheduleType,
    val selectedWeekdays: Set<DayOfWeek>,
    val reminderTime: LocalTime,
    val imagePath: String?,
    val createdAt: Instant,
    val isActive: Boolean
)
