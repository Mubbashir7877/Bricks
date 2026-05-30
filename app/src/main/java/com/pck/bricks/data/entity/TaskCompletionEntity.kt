package com.pck.bricks.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "task_completions",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["habitId"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = HabitTaskEntity::class,
            parentColumns = ["taskId"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("habitId"),
        Index("taskId"),
        Index("habitId", "taskId", "dateEpochDay", unique = true)
    ]
)
data class TaskCompletionEntity(
    @PrimaryKey val completionId: String,
    val habitId: String,
    val taskId: String,
    val dateEpochDay: Long,
    val isCompleted: Boolean,
    val completedAtEpochMillis: Long?
)
