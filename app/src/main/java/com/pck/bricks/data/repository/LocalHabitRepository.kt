package com.pck.bricks.data.repository

import com.pck.bricks.core.model.CreateHabitInput
import com.pck.bricks.core.model.Habit
import com.pck.bricks.core.model.HabitDayRecord
import com.pck.bricks.core.model.HabitProgress
import com.pck.bricks.core.model.HabitTask
import com.pck.bricks.core.model.TaskCompletionRecord
import com.pck.bricks.core.model.TierStatus
import com.pck.bricks.core.model.TierType
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
import com.pck.bricks.data.mapper.DayRecordMapper
import com.pck.bricks.data.mapper.HabitMapper
import com.pck.bricks.data.mapper.ProgressMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class LocalHabitRepository(
    private val habitDao: HabitDao,
    private val habitTaskDao: HabitTaskDao,
    private val habitProgressDao: HabitProgressDao,
    private val habitDayRecordDao: HabitDayRecordDao,
    private val taskCompletionDao: TaskCompletionDao
) : HabitRepository {

    override fun getActiveHabits(): Flow<List<Habit>> =
        habitDao.observeActiveHabits().map { list -> list.map(HabitMapper::toDomain) }

    override fun getHabit(habitId: String): Flow<Habit?> =
        habitDao.observeHabit(habitId).map { it?.let(HabitMapper::toDomain) }

    override suspend fun createHabit(input: CreateHabitInput): Result<String> = runCatching {
        val habitId = UUID.randomUUID().toString()
        val now = Instant.now()
        val today = LocalDate.now()

        habitDao.insert(
            HabitEntity(
                habitId = habitId,
                name = input.name.trim(),
                scheduleType = input.scheduleType.name,
                selectedWeekdaysCsv = input.selectedWeekdays.joinToString(",") { it.value.toString() },
                reminderTimeMinutes = input.reminderTime.toSecondOfDay() / 60,
                imagePath = input.imagePath,
                createdAtEpochMillis = now.toEpochMilli(),
                isActive = true
            )
        )

        habitTaskDao.insertAll(
            input.tasks.mapIndexed { index, name ->
                HabitTaskEntity(
                    taskId = UUID.randomUUID().toString(),
                    habitId = habitId,
                    taskName = name.trim(),
                    sortOrder = index
                )
            }
        )

        habitProgressDao.insert(
            HabitProgressEntity(
                progressId = UUID.randomUUID().toString(),
                habitId = habitId,
                currentTier = TierType.BRONZE.name,
                totalBricksRequired = 30,
                completedBrickCount = 0,
                missedGapCount = 0,
                consecutiveMissedScheduledDays = 0,
                currentWallStartDateEpochDay = today.toEpochDay(),
                lastProcessedDateEpochDay = null,
                lastCompletedDateEpochDay = null,
                tierStatus = TierStatus.ACTIVE.name
            )
        )

        habitId
    }

    override suspend fun softDeleteHabit(habitId: String): Result<Unit> = runCatching {
        habitDao.softDelete(habitId)
    }

    override fun getTasksForHabit(habitId: String): Flow<List<HabitTask>> =
        habitTaskDao.observeTasksForHabit(habitId).map { list -> list.map(HabitMapper::taskToDomain) }

    override suspend fun getTasksOnce(habitId: String): List<HabitTask> =
        habitTaskDao.getTasksForHabit(habitId).map(HabitMapper::taskToDomain)

    override fun getProgress(habitId: String): Flow<HabitProgress?> =
        habitProgressDao.observeProgress(habitId).map { it?.let(ProgressMapper::toDomain) }

    override fun getAllActiveProgress(): Flow<List<HabitProgress>> =
        habitProgressDao.observeAllActiveProgress().map { list -> list.map(ProgressMapper::toDomain) }

    override suspend fun getProgressOnce(habitId: String): HabitProgress? =
        habitProgressDao.getProgress(habitId)?.let(ProgressMapper::toDomain)

    override suspend fun saveProgress(progress: HabitProgress) {
        habitProgressDao.update(ProgressMapper.toEntity(progress))
    }

    override suspend fun getActiveHabitsOnce(): List<Habit> =
        habitDao.getActiveHabits().map(HabitMapper::toDomain)

    override fun getTodayTaskCompletions(habitId: String, date: LocalDate): Flow<List<TaskCompletionRecord>> =
        taskCompletionDao.observeCompletionsForDay(habitId, date.toEpochDay())
            .map { list -> list.map(DayRecordMapper::completionToDomain) }

    override suspend fun getTodayTaskCompletionsOnce(habitId: String, date: LocalDate): List<TaskCompletionRecord> =
        taskCompletionDao.getCompletionsForDay(habitId, date.toEpochDay()).map(DayRecordMapper::completionToDomain)

    override suspend fun setTaskCompleted(
        habitId: String,
        taskId: String,
        date: LocalDate
    ): Result<Unit> = runCatching {
        taskCompletionDao.upsert(
            TaskCompletionEntity(
                completionId = UUID.randomUUID().toString(),
                habitId = habitId,
                taskId = taskId,
                dateEpochDay = date.toEpochDay(),
                isCompleted = true,
                completedAtEpochMillis = Instant.now().toEpochMilli()
            )
        )
    }

    override suspend fun lockCompletedDay(
        habitId: String,
        date: LocalDate,
        brickIndex: Int
    ): Result<Unit> = runCatching {
        val epochDay = date.toEpochDay()
        val existing = habitDayRecordDao.getRecord(habitId, epochDay)
        if (existing == null) {
            habitDayRecordDao.upsert(
                HabitDayRecordEntity(
                    recordId = UUID.randomUUID().toString(),
                    habitId = habitId,
                    dateEpochDay = epochDay,
                    wasScheduled = true,
                    wasCompleted = true,
                    wasMissed = false,
                    wasProcessed = true,
                    brickIndex = brickIndex,
                    isGap = false,
                    createdAtEpochMillis = Instant.now().toEpochMilli()
                )
            )
        } else {
            habitDayRecordDao.markCompleted(habitId, epochDay, brickIndex)
        }
    }

    override suspend fun recordMissedDay(habitId: String, date: LocalDate): Result<Unit> = runCatching {
        val epochDay = date.toEpochDay()
        val existing = habitDayRecordDao.getRecord(habitId, epochDay)
        if (existing == null) {
            habitDayRecordDao.upsert(
                HabitDayRecordEntity(
                    recordId = UUID.randomUUID().toString(),
                    habitId = habitId,
                    dateEpochDay = epochDay,
                    wasScheduled = true,
                    wasCompleted = false,
                    wasMissed = true,
                    wasProcessed = true,
                    brickIndex = null,
                    isGap = true,
                    createdAtEpochMillis = Instant.now().toEpochMilli()
                )
            )
        } else {
            habitDayRecordDao.markMissed(habitId, epochDay)
        }
    }

    override suspend fun fortifyHabit(habitId: String): Result<Unit> = runCatching {
        val entity = habitProgressDao.getProgress(habitId) ?: return@runCatching
        val progress = ProgressMapper.toDomain(entity)
        if (progress.tierStatus != TierStatus.COMPLETED) return@runCatching
        val nextTier = when (progress.currentTier) {
            TierType.BRONZE -> TierType.SILVER
            TierType.SILVER -> TierType.GOLD
            TierType.GOLD -> return@runCatching
        }
        val fortified = progress.copy(
            currentTier = nextTier,
            totalBricksRequired = bricksForTier(nextTier),
            missedGapCount = 0,
            consecutiveMissedScheduledDays = 0,
            tierStatus = TierStatus.ACTIVE
        )
        habitProgressDao.update(ProgressMapper.toEntity(fortified))
    }

    override suspend fun getDayRecord(habitId: String, date: LocalDate): HabitDayRecord? =
        habitDayRecordDao.getRecord(habitId, date.toEpochDay())?.let(DayRecordMapper::toDomain)

    override suspend fun upsertDayRecord(record: HabitDayRecord) {
        habitDayRecordDao.upsert(DayRecordMapper.toEntity(record))
    }

    override suspend fun updateHabitImage(habitId: String, imagePath: String?) {
        habitDao.updateImagePath(habitId, imagePath)
    }

    private fun bricksForTier(tier: TierType): Int = when (tier) {
        TierType.BRONZE -> 30
        TierType.SILVER -> 60
        TierType.GOLD -> 90
    }
}
