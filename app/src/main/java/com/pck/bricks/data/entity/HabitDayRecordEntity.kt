package com.pck.bricks.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "habit_day_records",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["habitId"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("habitId"),
        Index("habitId", "dateEpochDay", unique = true)
    ]
)
data class HabitDayRecordEntity(
    @PrimaryKey val recordId: String,
    val habitId: String,
    val dateEpochDay: Long,
    val wasScheduled: Boolean,
    val wasCompleted: Boolean,
    val wasMissed: Boolean,
    val wasProcessed: Boolean,
    val brickIndex: Int?,
    val isGap: Boolean,
    val createdAtEpochMillis: Long
)
