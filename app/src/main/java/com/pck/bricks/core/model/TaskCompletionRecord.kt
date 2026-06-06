package com.pck.bricks.core.model

import java.time.Instant
import java.time.LocalDate

data class TaskCompletionRecord(
    val completionId: String,
    val habitId: String,
    val taskId: String,
    val date: LocalDate,
    val isCompleted: Boolean,
    val completedAt: Instant?
)