package com.pck.bricks.data.repository

import com.pck.bricks.core.model.CreateHabitInput
import com.pck.bricks.core.model.Habit
import com.pck.bricks.core.model.HabitDayRecord
import com.pck.bricks.core.model.HabitProgress
import com.pck.bricks.core.model.HabitTask
import com.pck.bricks.core.model.TaskCompletionRecord
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface HabitRepository {

    fun getActiveHabits(): Flow<List<Habit>>
    fun getHabit(habitId: String): Flow<Habit?>
    suspend fun createHabit(input: CreateHabitInput): Result<String>
    suspend fun softDeleteHabit(habitId: String): Result<Unit>

    fun getTasksForHabit(habitId: String): Flow<List<HabitTask>>
    suspend fun getTasksOnce(habitId: String): List<HabitTask>

    fun getProgress(habitId: String): Flow<HabitProgress?>
    suspend fun getProgressOnce(habitId: String): HabitProgress?
    suspend fun saveProgress(progress: HabitProgress)

    suspend fun getActiveHabitsOnce(): List<Habit>

    fun getTodayTaskCompletions(habitId: String, date: LocalDate): Flow<List<TaskCompletionRecord>>
    suspend fun setTaskCompleted(habitId: String, taskId: String, date: LocalDate): Result<Unit>

    suspend fun lockCompletedDay(habitId: String, date: LocalDate, brickIndex: Int): Result<Unit>
    suspend fun recordMissedDay(habitId: String, date: LocalDate): Result<Unit>
    suspend fun fortifyHabit(habitId: String): Result<Unit>

    suspend fun getDayRecord(habitId: String, date: LocalDate): HabitDayRecord?
    suspend fun upsertDayRecord(record: HabitDayRecord)
}
