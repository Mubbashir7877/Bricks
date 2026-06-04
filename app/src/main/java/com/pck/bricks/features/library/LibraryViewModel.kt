package com.pck.bricks.features.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.pck.bricks.BricksApp
import com.pck.bricks.core.time.ScheduledDayCalculator
import com.pck.bricks.data.repository.HabitRepository
import com.pck.bricks.features.rollover.DailyRolloverProcessor
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class LibraryUiState(
    val habits: List<LibraryCardItem> = emptyList(),
    val isLoading: Boolean = true
)

class LibraryViewModel(
    private val habitRepository: HabitRepository,
    private val rolloverProcessor: DailyRolloverProcessor,
    private val scheduledDayCalculator: ScheduledDayCalculator
) : ViewModel() {

    private val today = LocalDate.now()

    val uiState: StateFlow<LibraryUiState> = combine(
        habitRepository.getActiveHabits(),
        habitRepository.getAllActiveProgress()
    ) { habits, progresses ->
        val progressMap = progresses.associateBy { it.habitId }
        val items = habits.map { habit ->
            val progress = progressMap[habit.habitId]
            LibraryCardItem(
                habit = habit,
                progress = progress,
                isScheduledToday = scheduledDayCalculator.isScheduled(habit, today),
                isCompletedToday = progress?.lastCompletedDate == today
            )
        }
        LibraryUiState(habits = items, isLoading = false)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LibraryUiState()
    )

    init {
        viewModelScope.launch {
            runCatching { rolloverProcessor.processPreviousScheduledDays() }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as BricksApp
                LibraryViewModel(
                    habitRepository = app.habitRepository,
                    rolloverProcessor = app.dailyRolloverProcessor,
                    scheduledDayCalculator = app.scheduledDayCalculator
                )
            }
        }
    }
}
