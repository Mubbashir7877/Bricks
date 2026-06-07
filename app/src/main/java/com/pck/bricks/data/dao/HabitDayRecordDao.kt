package com.pck.bricks.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pck.bricks.data.entity.HabitDayRecordEntity

@Dao
interface HabitDayRecordDao {

    @Query("SELECT * FROM habit_day_records WHERE habitId = :habitId AND dateEpochDay = :dateEpochDay")
    suspend fun getRecord(habitId: String, dateEpochDay: Long): HabitDayRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: HabitDayRecordEntity)

    @Query("UPDATE habit_day_records SET wasProcessed = 1 WHERE habitId = :habitId AND dateEpochDay = :dateEpochDay")
    suspend fun markProcessed(habitId: String, dateEpochDay: Long)

    @Query(
        "UPDATE habit_day_records SET wasMissed = 1, wasProcessed = 1 " +
        "WHERE habitId = :habitId AND dateEpochDay = :dateEpochDay"
    )
    suspend fun markMissed(habitId: String, dateEpochDay: Long)

    @Query(
        "UPDATE habit_day_records SET wasCompleted = 1, wasProcessed = 1, brickIndex = :brickIndex " +
        "WHERE habitId = :habitId AND dateEpochDay = :dateEpochDay"
    )
    suspend fun markCompleted(habitId: String, dateEpochDay: Long, brickIndex: Int)

    @Query("DELETE FROM habit_day_records WHERE habitId = :habitId")
    suspend fun deleteAllForHabit(habitId: String)
}
