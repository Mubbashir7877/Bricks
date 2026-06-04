package com.pck.bricks.features.creation

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.pck.bricks.BricksApp
import com.pck.bricks.core.model.CreateHabitInput
import com.pck.bricks.core.model.ScheduleType
import com.pck.bricks.data.repository.HabitRepository
import com.pck.bricks.features.notifications.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.UUID

data class CreateHabitFormState(
    val name: String = "",
    val scheduleType: ScheduleType = ScheduleType.DAILY,
    val selectedWeekdays: Set<DayOfWeek> = emptySet(),
    val reminderHour: Int = 8,
    val reminderMinute: Int = 0,
    val tasks: List<String> = listOf(""),
    val selectedImageUri: Uri? = null,
    val nameError: String? = null,
    val weekdaysError: String? = null,
    val tasksError: String? = null,
    val isSaving: Boolean = false
)

sealed class CreateHabitEvent {
    data class Created(val habitId: String) : CreateHabitEvent()
}

class CreateHabitViewModel(
    private val application: Application,
    private val habitRepository: HabitRepository,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {

    private val _formState = MutableStateFlow(CreateHabitFormState())
    val formState: StateFlow<CreateHabitFormState> = _formState.asStateFlow()

    private val _events = Channel<CreateHabitEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onNameChange(value: String) {
        _formState.update { it.copy(name = value, nameError = null) }
    }

    fun onScheduleTypeChange(type: ScheduleType) {
        _formState.update { it.copy(scheduleType = type, weekdaysError = null) }
    }

    fun onWeekdayToggle(day: DayOfWeek) {
        _formState.update { state ->
            val updated = if (day in state.selectedWeekdays)
                state.selectedWeekdays - day
            else
                state.selectedWeekdays + day
            state.copy(selectedWeekdays = updated, weekdaysError = null)
        }
    }

    fun onTimeChange(hour: Int, minute: Int) {
        _formState.update { it.copy(reminderHour = hour, reminderMinute = minute) }
    }

    fun onTaskChange(index: Int, value: String) {
        _formState.update { state ->
            val tasks = state.tasks.toMutableList().also { it[index] = value }
            state.copy(tasks = tasks, tasksError = null)
        }
    }

    fun onAddTask() {
        _formState.update { it.copy(tasks = it.tasks + "") }
    }

    fun onRemoveTask(index: Int) {
        _formState.update { state ->
            if (state.tasks.size <= 1) return@update state
            state.copy(tasks = state.tasks.filterIndexed { i, _ -> i != index })
        }
    }

    fun onImageSelected(uri: Uri) {
        _formState.update { it.copy(selectedImageUri = uri) }
    }

    fun onRemoveImage() {
        _formState.update { it.copy(selectedImageUri = null) }
    }

    fun onSubmit() {
        val state = _formState.value
        if (!validateSync(state)) return

        viewModelScope.launch {
            _formState.update { it.copy(isSaving = true) }

            // Uniqueness check (spec §6.1: duplicate active names not allowed)
            val existingNames = habitRepository.getActiveHabitsOnce()
                .map { it.name.trim().lowercase() }
            if (state.name.trim().lowercase() in existingNames) {
                _formState.update { it.copy(isSaving = false, nameError = "A habit with this name already exists") }
                return@launch
            }

            val imagePath = state.selectedImageUri?.let { copyImageToPrivateStorage(it) }

            val input = CreateHabitInput(
                name = state.name.trim(),
                scheduleType = state.scheduleType,
                selectedWeekdays = state.selectedWeekdays,
                reminderTime = LocalTime.of(state.reminderHour, state.reminderMinute),
                tasks = state.tasks.filter { it.isNotBlank() },
                imagePath = imagePath
            )

            val result = habitRepository.createHabit(input)
            _formState.update { it.copy(isSaving = false) }

            result.onSuccess { habitId ->
                val habit = habitRepository.getActiveHabitsOnce().find { it.habitId == habitId }
                if (habit != null) reminderScheduler.scheduleHabitReminder(habit)
                _events.send(CreateHabitEvent.Created(habitId))
            }
            result.onFailure {
                _formState.update { s -> s.copy(nameError = "Failed to save habit") }
            }
        }
    }

    private fun validateSync(state: CreateHabitFormState): Boolean {
        var valid = true
        if (state.name.isBlank()) {
            _formState.update { it.copy(nameError = "Name is required") }
            valid = false
        }
        if (state.scheduleType == ScheduleType.SPECIFIC_WEEKDAYS && state.selectedWeekdays.isEmpty()) {
            _formState.update { it.copy(weekdaysError = "Select at least one day") }
            valid = false
        }
        val nonEmptyTasks = state.tasks.count { it.isNotBlank() }
        if (nonEmptyTasks == 0) {
            _formState.update { it.copy(tasksError = "Add at least one task") }
            valid = false
        }
        return valid
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

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as BricksApp
                CreateHabitViewModel(
                    application = app,
                    habitRepository = app.habitRepository,
                    reminderScheduler = app.reminderScheduler
                )
            }
        }
    }
}
