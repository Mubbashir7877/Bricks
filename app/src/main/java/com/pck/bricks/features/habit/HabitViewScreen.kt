package com.pck.bricks.features.habit

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.pck.bricks.core.model.HabitProgress
import com.pck.bricks.core.model.HabitTask
import com.pck.bricks.core.model.TierStatus
import com.pck.bricks.core.model.TierType
import com.pck.bricks.features.wall.BrickLayoutCalculator
import com.pck.bricks.features.wall.WallCanvas
import com.pck.bricks.features.wall.WallRenderModel

private val bronzeColor = Color(0xFFB85C3C)
private val silverColor = Color(0xFF787878)
private val goldColor   = Color(0xFFC9A227)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitViewScreen(
    habitId: String,
    navController: NavController,
    viewModel: HabitViewModel = viewModel(
        key = habitId,
        factory = HabitViewModel.factory(habitId)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let { viewModel.onImageSelected(it) } }

    if (showDeleteDialog && uiState.habit != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Habit") },
            text = {
                Text("Permanently delete \"${uiState.habit!!.name}\"? This cannot be undone.")
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onDeleteConfirmed { navController.popBackStack() }
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.habit?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete habit")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                uiState.habit == null -> Text(
                    "Habit not found",
                    modifier = Modifier.align(Alignment.Center)
                )
                else -> HabitContent(
                    uiState = uiState,
                    onTaskChecked = viewModel::onTaskChecked,
                    onFortify = viewModel::onFortifyClicked,
                    onPickImage = {
                        imagePickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun HabitContent(
    uiState: HabitViewUiState,
    onTaskChecked: (String) -> Unit,
    onFortify: () -> Unit,
    onPickImage: () -> Unit
) {
    val progress = uiState.progress ?: return
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        TierHeader(progress = progress)
        Spacer(Modifier.height(16.dp))

        when (uiState.screenState) {
            HabitScreenState.Checklist -> {
                uiState.wallRenderModel?.let { wallModel ->
                    WallWithImageButton(wallModel = wallModel, onPickImage = onPickImage)
                    Spacer(Modifier.height(16.dp))
                }
                TaskChecklist(
                    tasks = uiState.tasks,
                    completedIds = uiState.completedTaskIds,
                    onTaskChecked = onTaskChecked
                )
            }
            HabitScreenState.CompletedLocked -> {
                uiState.wallRenderModel?.let { wallModel ->
                    CompletedView(
                        progress = progress,
                        tasks = uiState.tasks,
                        wallModel = wallModel,
                        onFortify = onFortify,
                        onPickImage = onPickImage
                    )
                }
            }
            HabitScreenState.NotScheduled -> {
                Text(
                    "No session today — this habit isn't scheduled for ${java.time.LocalDate.now().dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }}.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                uiState.wallRenderModel?.let { wallModel ->
                    WallWithImageButton(wallModel = wallModel, onPickImage = onPickImage)
                }
            }
        }
    }
}

@Composable
private fun TierHeader(progress: HabitProgress) {
    val tierColor = when (progress.currentTier) {
        TierType.BRONZE -> bronzeColor
        TierType.SILVER -> silverColor
        TierType.GOLD   -> goldColor
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SuggestionChip(
            onClick = {},
            label = { Text(progress.currentTier.name) },
            colors = SuggestionChipDefaults.suggestionChipColors(containerColor = tierColor.copy(alpha = 0.15f)),
            border = SuggestionChipDefaults.suggestionChipBorder(enabled = true, borderColor = tierColor)
        )
        val total = BrickLayoutCalculator().brickCountForTier(progress.currentTier)
        Text(
            "${progress.completedBrickCount + progress.missedGapCount} / $total bricks",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TaskChecklist(
    tasks: List<HabitTask>,
    completedIds: Set<String>,
    onTaskChecked: (String) -> Unit
) {
    Text("Today's tasks", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(8.dp))
    tasks.forEach { task ->
        val checked = task.taskId in completedIds
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = { if (it && !checked) onTaskChecked(task.taskId) }
            )
            Text(
                text = task.taskName,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
private fun WallWithImageButton(
    wallModel: WallRenderModel,
    onPickImage: () -> Unit
) {
    Box {
        WallCanvas(wallModel = wallModel, modifier = Modifier.fillMaxWidth())
        IconButton(
            onClick = onPickImage,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(6.dp)
                .size(32.dp)
                .background(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = Icons.Default.AddPhotoAlternate,
                contentDescription = "Add or replace image",
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun CompletedView(
    progress: HabitProgress,
    tasks: List<HabitTask>,
    wallModel: WallRenderModel,
    onFortify: () -> Unit,
    onPickImage: () -> Unit
) {
    WallWithImageButton(wallModel = wallModel, onPickImage = onPickImage)
    Spacer(Modifier.height(16.dp))

    if (progress.tierStatus == TierStatus.COMPLETED) {
        when (progress.currentTier) {
            TierType.GOLD -> Text(
                "Wall complete — Gold tier achieved!",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = goldColor
            )
            else -> {
                Text(
                    "Wall complete — ready to fortify!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = onFortify, modifier = Modifier.fillMaxWidth()) {
                    Text("Fortify Wall")
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }

    Text(
        "Completed today",
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    tasks.forEach { task ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = true, onCheckedChange = null)
            Text(task.taskName, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 8.dp))
        }
    }
}
