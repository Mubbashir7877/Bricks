package com.pck.bricks.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pck.bricks.data.entity.HabitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {

    @Query("SELECT * FROM habits WHERE isActive = 1 ORDER BY createdAtEpochMillis ASC")
    fun observeActiveHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE habitId = :habitId")
    fun observeHabit(habitId: String): Flow<HabitEntity?>

    @Query("SELECT * FROM habits WHERE isActive = 1")
    suspend fun getActiveHabits(): List<HabitEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(habit: HabitEntity)

    @Query("UPDATE habits SET isActive = 0 WHERE habitId = :habitId")
    suspend fun softDelete(habitId: String)

    @Query("UPDATE habits SET imagePath = :imagePath WHERE habitId = :habitId")
    suspend fun updateImagePath(habitId: String, imagePath: String?)

    @Query("UPDATE habits SET soundPath = :soundPath WHERE habitId = :habitId")
    suspend fun updateSoundPath(habitId: String, soundPath: String?)

    @Query(
        "UPDATE habits SET name = :name, scheduleType = :scheduleType, " +
        "selectedWeekdaysCsv = :weekdaysCsv, reminderTimeMinutes = :reminderMins " +
        "WHERE habitId = :habitId"
    )
    suspend fun updateHabitDetails(habitId: String, name: String, scheduleType: String, weekdaysCsv: String, reminderMins: Int)
}
