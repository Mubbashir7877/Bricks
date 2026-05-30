package com.pck.bricks.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "habit_progress",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["habitId"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("habitId", unique = true)]
)
data class HabitProgressEntity(
    @PrimaryKey val progressId: String,
    val habitId: String,
    val currentTier: String,
    val totalBricksRequired: Int,
    val completedBrickCount: Int,
    val missedGapCount: Int,
    val consecutiveMissedScheduledDays: Int,
    val currentWallStartDateEpochDay: Long,
    val lastProcessedDateEpochDay: Long?,
    val lastCompletedDateEpochDay: Long?,
    val tierStatus: String
)
