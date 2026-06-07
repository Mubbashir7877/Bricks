package com.pck.bricks.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pck.bricks.data.entity.HabitTaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitTaskDao {

    @Query("SELECT * FROM habit_tasks WHERE habitId = :habitId ORDER BY sortOrder ASC")
    fun observeTasksForHabit(habitId: String): Flow<List<HabitTaskEntity>>

    @Query("SELECT * FROM habit_tasks WHERE habitId = :habitId ORDER BY sortOrder ASC")
    suspend fun getTasksForHabit(habitId: String): List<HabitTaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<HabitTaskEntity>)

    @Query("DELETE FROM habit_tasks WHERE habitId = :habitId")
    suspend fun deleteAllForHabit(habitId: String)
}
