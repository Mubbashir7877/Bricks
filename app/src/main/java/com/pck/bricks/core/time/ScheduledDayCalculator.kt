package com.pck.bricks.core.time

import com.pck.bricks.core.model.Habit
import com.pck.bricks.core.model.ScheduleType
import java.time.LocalDate

class ScheduledDayCalculator {

    fun isScheduled(habit: Habit, date: LocalDate): Boolean = when (habit.scheduleType) {
        ScheduleType.DAILY -> true
        ScheduleType.SPECIFIC_WEEKDAYS -> habit.selectedWeekdays.contains(date.dayOfWeek)
    }

    fun scheduledDaysBetween(
        habit: Habit,
        fromInclusive: LocalDate,
        toExclusive: LocalDate
    ): List<LocalDate> {
        val days = mutableListOf<LocalDate>()
        var cursor = fromInclusive
        while (cursor.isBefore(toExclusive)) {
            if (isScheduled(habit, cursor)) days += cursor
            cursor = cursor.plusDays(1)
        }
        return days
    }
}
