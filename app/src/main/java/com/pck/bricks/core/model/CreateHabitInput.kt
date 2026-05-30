package com.pck.bricks.core.model

import java.time.DayOfWeek
import java.time.LocalTime

data class CreateHabitInput(
    val name: String,
    val scheduleType: ScheduleType,
    val selectedWeekdays: Set<DayOfWeek>,
    val reminderTime: LocalTime,
    val tasks: List<String>,
    val imagePath: String?
)
