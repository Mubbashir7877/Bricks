package com.pck.bricks.features.habit

import android.app.Application
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.pck.bricks.BricksApp
import com.pck.bricks.core.model.Habit
import com.pck.bricks.core.model.HabitProgress
import com.pck.bricks.core.model.HabitTask
import com.pck.bricks.core.model.ScheduleType
import com.pck.bricks.core.time.ScheduledDayCalculator
import com.pck.bricks.data.repository.HabitRepository
import com.pck.bricks.features.notifications.ReminderScheduler
import com.pck.bricks.features.rollover.TierTransitionEngine
import com.pck.bricks.features.wall.BrickLayoutCalculator
import com.pck.bricks.features.wall.BrickProgressCalculator
import com.pck.bricks.features.wall.WallRenderModel
import com.pck.bricks.features.wall.WallRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

enum class HabitScreenState { Checklist, CompletedLocked, NotScheduled }

data class EditHabitFormState(
    val name: String = "",
    val scheduleType: ScheduleType = ScheduleType.DAILY,
    val selectedWeekdays: Set<DayOfWeek> = emptySet(),
    val reminderHour: Int = 8,
    val reminderMinute: Int = 0,
    val tasks: List<String> = listOf(""),
    val nameError: String? = null,
    val weekdaysError: String? = null,
    val tasksError: String? = null,
    val isSaving: Boolean = false
)

data class HabitViewUiState(
    val habit: Habit? = null,
    val tasks: List<HabitTask> = emptyList(),
    val completedTaskIds: Set<String> = emptySet(),
    val progress: HabitProgress? = null,
    val wallRenderModel: WallRenderModel? = null,
    val screenState: HabitScreenState = HabitScreenState.Checklist,
    val isLoading: Boolean = true
)

class HabitViewModel(
    private val habitId: String,
    private val application: Application,
    private val habitRepository: HabitRepository,
    private val tierTransitionEngine: TierTransitionEngine,
    private val scheduledDayCalculator: ScheduledDayCalculator,
    private val brickProgressCalculator: BrickProgressCalculator,
    private val wallRenderer: WallRenderer,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {

    private val today: LocalDate = LocalDate.now()
    private val _animatingBrickIndex = MutableStateFlow<Int?>(null)

    private val _soundPickError = MutableStateFlow<String?>(null)
    val soundPickError: StateFlow<String?> = _soundPickError.asStateFlow()

    private val _editFormState = MutableStateFlow<EditHabitFormState?>(null)
    val editFormState: StateFlow<EditHabitFormState?> = _editFormState.asStateFlow()

    fun clearSoundError() { _soundPickError.value = null }

    // ── Edit habit ──────────────────────────────────────────────────────────

    fun openEditForm() {
        val habit = uiState.value.habit ?: return
        _editFormState.value = EditHabitFormState(
            name = habit.name,
            scheduleType = habit.scheduleType,
            selectedWeekdays = habit.selectedWeekdays,
            reminderHour = habit.reminderTime.hour,
            reminderMinute = habit.reminderTime.minute,
            tasks = uiState.value.tasks.map { it.taskName }.ifEmpty { listOf("") }
        )
    }

    fun closeEditForm() { _editFormState.value = null }

    fun onEditNameChange(v: String) =
        _editFormState.update { it?.copy(name = v, nameError = null) }

    fun onEditScheduleTypeChange(t: ScheduleType) =
        _editFormState.update { it?.copy(scheduleType = t, weekdaysError = null) }

    fun onEditWeekdayToggle(day: DayOfWeek) = _editFormState.update { s ->
        s?.let {
            val days = if (day in it.selectedWeekdays) it.selectedWeekdays - day else it.selectedWeekdays + day
            it.copy(selectedWeekdays = days, weekdaysError = null)
        }
    }

    fun onEditTimeChange(hour: Int, minute: Int) =
        _editFormState.update { it?.copy(reminderHour = hour, reminderMinute = minute) }

    fun onEditTaskChange(index: Int, value: String) = _editFormState.update { s ->
        s?.let { it.copy(tasks = it.tasks.toMutableList().also { l -> l[index] = value }, tasksError = null) }
    }

    fun onEditAddTask() = _editFormState.update { it?.copy(tasks = it.tasks + "") }

    fun onEditRemoveTask(index: Int) = _editFormState.update { s ->
        s?.let { if (it.tasks.size <= 1) it else it.copy(tasks = it.tasks.filterIndexed { i, _ -> i != index }) }
    }

    fun onSaveEdit(onDone: () -> Unit) {
        val form = _editFormState.value ?: return
        if (!validateEditForm(form)) return
        viewModelScope.launch {
            _editFormState.update { it?.copy(isSaving = true) }
            val trimmedName = form.name.trim()
            val duplicate = habitRepository.getActiveHabitsOnce()
                .any { it.habitId != habitId && it.name.trim().equals(trimmedName, ignoreCase = true) }
            if (duplicate) {
                _editFormState.update { it?.copy(isSaving = false, nameError = "A habit with this name already exists") }
                return@launch
            }
            val reminderTime = LocalTime.of(form.reminderHour, form.reminderMinute)
            habitRepository.updateHabit(
                habitId = habitId,
                name = trimmedName,
                scheduleType = form.scheduleType,
                selectedWeekdays = form.selectedWeekdays,
                reminderTime = reminderTime,
                tasks = form.tasks.filter { it.isNotBlank() }
            )
            uiState.value.habit?.let { existing ->
                reminderScheduler.scheduleHabitReminder(
                    existing.copy(
                        name = trimmedName,
                        scheduleType = form.scheduleType,
                        selectedWeekdays = form.selectedWeekdays,
                        reminderTime = reminderTime
                    )
                )
            }
            _editFormState.update { it?.copy(isSaving = false) }
            onDone()
        }
    }

    private fun validateEditForm(form: EditHabitFormState): Boolean {
        var valid = true
        if (form.name.isBlank()) {
            _editFormState.update { it?.copy(nameError = "Name is required") }
            valid = false
        }
        if (form.scheduleType == ScheduleType.SPECIFIC_WEEKDAYS && form.selectedWeekdays.isEmpty()) {
            _editFormState.update { it?.copy(weekdaysError = "Select at least one day") }
            valid = false
        }
        if (form.tasks.none { it.isNotBlank() }) {
            _editFormState.update { it?.copy(tasksError = "Add at least one task") }
            valid = false
        }
        return valid
    }

    // ── Reset progress ──────────────────────────────────────────────────────

    fun onResetConfirmed() {
        viewModelScope.launch { habitRepository.resetHabitProgress(habitId) }
    }

    // Combine _animatingBrickIndex with a refresh counter so lockDay() can fire both atomically
    private val _refresh = MutableStateFlow(0)
    private val _meta = combine(_refresh, _animatingBrickIndex) { _, animIndex -> animIndex }

    val uiState: StateFlow<HabitViewUiState> = combine(
        habitRepository.getHabit(habitId),
        habitRepository.getTasksForHabit(habitId),
        habitRepository.getTodayTaskCompletions(habitId, today),
        habitRepository.getProgress(habitId),
        _meta
    ) { habit, tasks, completions, progress, animIndex ->
        if (habit == null || progress == null) return@combine HabitViewUiState(isLoading = false)

        val completedIds = completions.filter { it.isCompleted }.map { it.taskId }.toSet()
        val isCompletedToday = progress.lastCompletedDate == today
        val isScheduled = scheduledDayCalculator.isScheduled(habit, today)

        val layout = BrickLayoutCalculator().calculateLayout(progress.currentTier)
        val wallModel = wallRenderer.renderWall(progress, layout, newlyAddedIndex = animIndex, imagePath = habit.imagePath)

        HabitViewUiState(
            habit = habit,
            tasks = tasks,
            completedTaskIds = completedIds,
            progress = progress,
            wallRenderModel = wallModel,
            screenState = when {
                isCompletedToday -> HabitScreenState.CompletedLocked
                !isScheduled     -> HabitScreenState.NotScheduled
                else             -> HabitScreenState.Checklist
            },
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HabitViewUiState()
    )

    fun onTaskChecked(taskId: String) {
        if (uiState.value.screenState != HabitScreenState.Checklist) return
        viewModelScope.launch {
            habitRepository.setTaskCompleted(habitId, taskId, today)
            val tasks = habitRepository.getTasksOnce(habitId)
            val freshCompletions = habitRepository.getTodayTaskCompletionsOnce(habitId, today)
            val completedIds = freshCompletions.filter { it.isCompleted }.map { it.taskId }.toSet()
            if (tasks.isNotEmpty() && tasks.all { it.taskId in completedIds }) {
                lockDay()
            }
        }
    }

    fun onFortifyClicked() {
        viewModelScope.launch { habitRepository.fortifyHabit(habitId) }
    }

    fun onDeleteConfirmed(onDeleted: () -> Unit) {
        viewModelScope.launch {
            habitRepository.softDeleteHabit(habitId)
            reminderScheduler.cancelHabitReminder(habitId)
            onDeleted()
        }
    }

    fun onImageSelected(uri: Uri) {
        viewModelScope.launch {
            val oldPath = uiState.value.habit?.imagePath
            val newPath = copyImageToPrivateStorage(uri) ?: return@launch
            oldPath?.let { runCatching { File(it).delete() } }
            habitRepository.updateHabitImage(habitId, newPath)
        }
    }

    fun onSoundSelected(uri: Uri) {
        viewModelScope.launch {
            val durationMs = getSoundDurationMs(uri)
            if (durationMs == null || durationMs > 30_000L) {
                _soundPickError.value = "Sound must be 30 seconds or less"
                return@launch
            }
            val oldPath = uiState.value.habit?.soundPath
            val newPath = copySoundToPrivateStorage(uri) ?: return@launch
            oldPath?.let { runCatching { File(it).delete() } }
            habitRepository.updateHabitSound(habitId, newPath)
        }
    }

    fun onSoundRemoved() {
        viewModelScope.launch {
            val oldPath = uiState.value.habit?.soundPath ?: return@launch
            runCatching { File(oldPath).delete() }
            habitRepository.updateHabitSound(habitId, null)
        }
    }

    private suspend fun getSoundDurationMs(uri: Uri): Long? = withContext(Dispatchers.IO) {
        runCatching {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(application, uri)
            val ms = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            retriever.release()
            ms
        }.getOrNull()
    }

    private suspend fun copySoundToPrivateStorage(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(application.filesDir, "habit_sounds").also { it.mkdirs() }
            val dest = File(dir, "sound_${UUID.randomUUID()}.mp3")
            application.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            }
            dest.absolutePath
        }.getOrNull()
    }

    private suspend fun copyImageToPrivateStorage(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(application.filesDir, "habit_images").also { it.mkdirs() }
            val dest = File(dir, "habit_${UUID.randomUUID()}.jpg")
            application.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            }
            dest.absolutePath
        }.getOrNull()
    }

    private suspend fun lockDay() {
        val progress = habitRepository.getProgressOnce(habitId) ?: return
        val brickIndex = brickProgressCalculator.nextBrickIndex(progress)
        habitRepository.lockCompletedDay(habitId, today, brickIndex)
        val updated = progress.copy(
            completedBrickCount = progress.completedBrickCount + 1,
            consecutiveMissedScheduledDays = 0,
            lastCompletedDate = today,
            lastProcessedDate = today
        )
        habitRepository.saveProgress(tierTransitionEngine.completeTierIfReady(updated))
        _animatingBrickIndex.value = brickIndex
        _refresh.update { it + 1 }
    }

    companion object {
        fun factory(habitId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as BricksApp
                HabitViewModel(
                    habitId = habitId,
                    application = app,
                    habitRepository = app.habitRepository,
                    tierTransitionEngine = app.tierTransitionEngine,
                    scheduledDayCalculator = app.scheduledDayCalculator,
                    brickProgressCalculator = app.brickProgressCalculator,
                    wallRenderer = app.wallRenderer,
                    reminderScheduler = app.reminderScheduler
                )
            }
        }
    }
}
