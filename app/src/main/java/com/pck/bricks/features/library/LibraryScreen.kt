package com.pck.bricks.features.library

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.pck.bricks.core.model.TierType
import com.pck.bricks.core.navigation.Screen
import com.pck.bricks.features.wall.BrickLayoutCalculator
import java.io.File

private val bronzeColor = Color(0xFFB85C3C)
private val silverColor = Color(0xFF787878)
private val goldColor   = Color(0xFFC9A227)

private val brickRed    = Color(0xFFB85C3C)
private val mortarDark  = Color(0xFF2D1810)

private fun tierColor(tier: TierType?): Color = when (tier) {
    TierType.BRONZE -> bronzeColor
    TierType.SILVER -> silverColor
    TierType.GOLD   -> goldColor
    null            -> Color.Gray
}

// Spec §9.1: visual labels are "Habit", "Routine", "LifeStyle"
private fun tierLabel(tier: TierType?): String = when (tier) {
    TierType.BRONZE -> "Habit"
    TierType.SILVER -> "Routine"
    TierType.GOLD   -> "LifeStyle"
    null            -> ""
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    navController: NavController,
    viewModel: LibraryViewModel = viewModel(factory = LibraryViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Bricks", fontWeight = FontWeight.Bold) })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate(Screen.CreateHabit.route) }) {
                Icon(Icons.Default.Add, contentDescription = "Create habit")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                uiState.habits.isEmpty() -> EmptyLibrary(
                    modifier = Modifier.align(Alignment.Center),
                    onCreateClick = { navController.navigate(Screen.CreateHabit.route) }
                )
                else -> LazyVerticalGrid(
                    // Spec §15.2: 3 cards across
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.habits, key = { it.habit.habitId }) { item ->
                        HabitCard(
                            item = item,
                            onClick = {
                                navController.navigate(Screen.HabitView.createRoute(item.habit.habitId))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HabitCard(item: LibraryCardItem, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Preview area — faded image or brick-pattern canvas (spec §15.2)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(88.dp)
            ) {
                if (item.habit.imagePath != null) {
                    AsyncImage(
                        model = File(item.habit.imagePath),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(0.55f)
                    )
                } else {
                    BrickPatternCanvas(modifier = Modifier.fillMaxSize())
                }

                // Completion status badge (spec §15.2: today's completion status)
                when {
                    item.isCompletedToday -> Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Completed today",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(18.dp)
                    )
                    item.isScheduledToday -> Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Due today",
                        tint = Color(0xFFFF9800),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(18.dp)
                    )
                }
            }

            // Info section
            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                Text(
                    text = item.habit.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                val tier = item.progress?.currentTier
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tierLabel(tier),
                        style = MaterialTheme.typography.labelSmall,
                        color = tierColor(tier),
                        fontWeight = FontWeight.Medium
                    )
                    if (item.progress != null) {
                        val total = BrickLayoutCalculator().brickCountForTier(item.progress.currentTier)
                        Text(
                            text = "${item.progress.completedBrickCount}/$total",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// Painted brick-wall pattern used as card background when no image is set (spec §11.3, §15.2)
@Composable
private fun BrickPatternCanvas(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        // mortar background
        drawRect(color = mortarDark)

        val mortar = 2.dp.toPx()
        val brickW = 32.dp.toPx()
        val brickH = 14.dp.toPx()
        val rowH = brickH + mortar
        val rows = (size.height / rowH).toInt() + 2

        repeat(rows) { row ->
            val y = row * rowH
            // Spec §11.2: bricks laid right-to-left — alternate-row offset mirrors a real wall
            val offsetX = if (row % 2 == 0) 0f else brickW / 2f
            var x = -offsetX
            while (x < size.width + brickW) {
                drawRect(
                    color = brickRed,
                    topLeft = Offset(x, y),
                    size = Size(brickW - mortar, brickH - mortar)
                )
                x += brickW
            }
        }
    }
}

@Composable
private fun EmptyLibrary(modifier: Modifier = Modifier, onCreateClick: () -> Unit) {
    Column(modifier = modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("No habits yet", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "Tap + to create your first habit and start building your wall.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
