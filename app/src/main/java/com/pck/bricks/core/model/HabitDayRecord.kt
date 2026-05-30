package com.pck.bricks.core.model

import java.time.Instant
import java.time.LocalDate

data class HabitDayRecord(
    val recordId: String,
    val habitId: String,
    val date: LocalDate,
    val wasScheduled: Boolean,
    val wasCompleted: Boolean,
    val wasMissed: Boolean,
    val wasProcessed: Boolean,
    val brickIndex: Int?,
    val isGap: Boolean,
    val createdAt: Instant
)
