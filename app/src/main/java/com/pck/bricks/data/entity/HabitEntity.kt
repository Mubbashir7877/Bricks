package com.pck.bricks.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey val habitId: String,
    val name: String,
    val scheduleType: String,
    val selectedWeekdaysCsv: String,
    val reminderTimeMinutes: Int,
    val imagePath: String?,
    val soundPath: String?,
    val createdAtEpochMillis: Long,
    val isActive: Boolean
)
