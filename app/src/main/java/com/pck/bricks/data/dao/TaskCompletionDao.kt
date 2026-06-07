package com.pck.bricks.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pck.bricks.data.entity.TaskCompletionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskCompletionDao {

    @Query(
        "SELECT * FROM task_completions WHERE habitId = :habitId AND dateEpochDay = :dateEpochDay"
    )
    fun observeCompletionsForDay(habitId: String, dateEpochDay: Long): Flow<List<TaskCompletionEntity>>

    @Query(
        "SELECT * FROM task_completions WHERE habitId = :habitId AND dateEpochDay = :dateEpochDay"
    )
    suspend fun getCompletionsForDay(habitId: String, dateEpochDay: Long): List<TaskCompletionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: TaskCompletionEntity)

    @Query("DELETE FROM task_completions WHERE habitId = :habitId")
    suspend fun deleteAllForHabit(habitId: String)
}
