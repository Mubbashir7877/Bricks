package com.pck.bricks.features.habit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pck.bricks.core.model.ScheduleType
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditHabitDialog(
    form: EditHabitFormState,
    onDismiss: () -> Unit,
    onNameChange: (String) -> Unit,
    onScheduleTypeChange: (ScheduleType) -> Unit,
    onWeekdayToggle: (DayOfWeek) -> Unit,
    onTimeChange: (Int, Int) -> Unit,
    onTaskChange: (Int, String) -> Unit,
    onAddTask: () -> Unit,
    onRemoveTask: (Int) -> Unit,
    onSave: () -> Unit
) {
    var showTimePicker by remember { mutableStateOf(false) }

    if (showTimePicker) {
        EditTimePickerDialog(
            initialHour = form.reminderHour,
            initialMinute = form.reminderMinute,
            onConfirm = { h, m -> onTimeChange(h, m); showTimePicker = false },
            onDismiss = { showTimePicker = false }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp
        ) {
            Column {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Edit Habit",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                HorizontalDivider()

                // Scrollable form
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Name
                    OutlinedTextField(
                        value = form.name,
                        onValueChange = onNameChange,
                        label = { Text("Habit name") },
                        isError = form.nameError != null,
                        supportingText = form.nameError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Schedule type
                    Column {
                        Text("Schedule", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = form.scheduleType == ScheduleType.DAILY,
                                onClick = { onScheduleTypeChange(ScheduleType.DAILY) },
                                label = { Text("Daily") }
                            )
                            FilterChip(
                                selected = form.scheduleType == ScheduleType.SPECIFIC_WEEKDAYS,
                                onClick = { onScheduleTypeChange(ScheduleType.SPECIFIC_WEEKDAYS) },
                                label = { Text("Specific days") }
                            )
                        }
                    }

                    // Weekday selector
                    if (form.scheduleType == ScheduleType.SPECIFIC_WEEKDAYS) {
                        Column {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                DayOfWeek.values().forEach { day ->
                                    FilterChip(
                                        selected = day in form.selectedWeekdays,
                                        onClick = { onWeekdayToggle(day) },
                                        label = { Text(day.getDisplayName(TextStyle.SHORT, Locale.getDefault())) }
                                    )
                                }
                            }
                            form.weekdaysError?.let {
                                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    // Reminder time
                    Column {
                        Text("Daily reminder", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(6.dp))
                        OutlinedButton(onClick = { showTimePicker = true }) {
                            Text(formatEditTime(form.reminderHour, form.reminderMinute))
                        }
                    }

                    // Tasks
                    Column {
                        Text("Tasks", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(6.dp))
                        form.tasks.forEachIndexed { index, task ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = task,
                                    onValueChange = { onTaskChange(index, it) },
                                    label = { Text("Task ${index + 1}") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                if (form.tasks.size > 1) {
                                    IconButton(onClick = { onRemoveTask(index) }) {
                                        Icon(Icons.Default.Close, contentDescription = "Remove task")
                                    }
                                }
                            }
                        }
                        form.tasksError?.let {
                            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(Modifier.height(4.dp))
                        TextButton(onClick = onAddTask) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Text("  Add task")
                        }
                    }
                }

                // Footer save button
                HorizontalDivider()
                Button(
                    onClick = onSave,
                    enabled = !form.isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text(if (form.isSaving) "Saving…" else "Save Changes")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberTimePickerState(initialHour = initialHour, initialMinute = initialMinute, is24Hour = false)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        text = { TimePicker(state = state) }
    )
}

private fun formatEditTime(hour: Int, minute: Int): String {
    val amPm = if (hour < 12) "AM" else "PM"
    val displayHour = when { hour == 0 -> 12; hour > 12 -> hour - 12; else -> hour }
    return "%d:%02d %s".format(displayHour, minute, amPm)
}
