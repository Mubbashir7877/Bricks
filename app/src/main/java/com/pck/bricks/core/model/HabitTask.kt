package com.pck.bricks.core.model

data class HabitTask(
    val taskId: String,
    val habitId: String,
    val taskName: String,
    val sortOrder: Int
)
