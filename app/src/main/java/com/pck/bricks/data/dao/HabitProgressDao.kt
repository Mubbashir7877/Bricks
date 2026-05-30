package com.pck.bricks.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pck.bricks.data.entity.HabitProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitProgressDao {

    @Query("SELECT * FROM habit_progress WHERE habitId = :habitId")
    fun observeProgress(habitId: String): Flow<HabitProgressEntity?>

    @Query("SELECT * FROM habit_progress WHERE habitId = :habitId")
    suspend fun getProgress(habitId: String): HabitProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(progress: HabitProgressEntity)

    @Update
    suspend fun update(progress: HabitProgressEntity)
}
