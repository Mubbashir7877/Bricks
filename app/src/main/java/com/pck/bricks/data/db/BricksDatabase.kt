package com.pck.bricks.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.pck.bricks.data.dao.HabitDao
import com.pck.bricks.data.dao.HabitDayRecordDao
import com.pck.bricks.data.dao.HabitProgressDao
import com.pck.bricks.data.dao.HabitTaskDao
import com.pck.bricks.data.dao.TaskCompletionDao
import com.pck.bricks.data.entity.HabitDayRecordEntity
import com.pck.bricks.data.entity.HabitEntity
import com.pck.bricks.data.entity.HabitProgressEntity
import com.pck.bricks.data.entity.HabitTaskEntity
import com.pck.bricks.data.entity.TaskCompletionEntity

@Database(
    entities = [
        HabitEntity::class,
        HabitTaskEntity::class,
        HabitProgressEntity::class,
        HabitDayRecordEntity::class,
        TaskCompletionEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class BricksDatabase : RoomDatabase() {

    abstract fun habitDao(): HabitDao
    abstract fun habitTaskDao(): HabitTaskDao
    abstract fun habitProgressDao(): HabitProgressDao
    abstract fun habitDayRecordDao(): HabitDayRecordDao
    abstract fun taskCompletionDao(): TaskCompletionDao

    companion object {
        @Volatile private var INSTANCE: BricksDatabase? = null

        fun getInstance(context: Context): BricksDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    BricksDatabase::class.java,
                    "bricks.db"
                ).build().also { INSTANCE = it }
            }
    }
}
