package com.pck.bricks.features.library

import com.pck.bricks.core.model.Habit
import com.pck.bricks.core.model.HabitProgress

data class LibraryCardItem(
    val habit: Habit,
    val progress: HabitProgress?,
    val isScheduledToday: Boolean,
    val isCompletedToday: Boolean
)
