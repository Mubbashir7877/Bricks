package com.pck.bricks.features.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.pck.bricks.core.model.TierStatus
import com.pck.bricks.core.model.TierType
import com.pck.bricks.core.navigation.Screen
import com.pck.bricks.features.habit.HabitDetailPane
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

// Returns the tier whose border color should frame the card.
// No border only at the very start of Bronze. Once a tier is completed the border
// persists as the "previous tier" border until the next tier is also completed.
private fun activeBorderTier(tier: TierType?, status: TierStatus?): TierType? {
    if (tier == null || status == null) return null
    return when (tier) {
        TierType.BRONZE -> if (status == TierStatus.COMPLETED) TierType.BRONZE else null
        TierType.SILVER -> if (status == TierStatus.COMPLETED) TierType.SILVER else TierType.BRONZE
        TierType.GOLD   -> if (status == TierStatus.COMPLETED) TierType.GOLD else TierType.SILVER
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    navController: NavController,
    viewModel: LibraryViewModel = viewModel(factory = LibraryViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    // Close panel if all habits disappear (e.g. last habit deleted while panel open)
    LaunchedEffect(uiState.habits.size) {
        if (uiState.habits.isEmpty()) selectedIndex = null
    }

    BackHandler(enabled = selectedIndex != null) { selectedIndex = null }

    Box(modifier = Modifier.fillMaxSize()) {
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
                        itemsIndexed(uiState.habits, key = { _, it -> it.habit.habitId }) { index, item ->
                            HabitCard(
                                item = item,
                                onClick = { selectedIndex = index }
                            )
                        }
                    }
                }
            }
        }

        // Habit detail overlay panel — shown above Scaffold (including TopAppBar)
        val idx = selectedIndex
        if (idx != null && uiState.habits.isNotEmpty()) {
            HabitOverlayPanel(
                habits = uiState.habits,
                initialIndex = idx.coerceIn(0, uiState.habits.lastIndex),
                onDismiss = { selectedIndex = null }
            )
        }
    }
}

@Composable
private fun HabitOverlayPanel(
    habits: List<LibraryCardItem>,
    initialIndex: Int,
    onDismiss: () -> Unit
) {
    val pagerState = rememberPagerState(initialPage = initialIndex) { habits.size }

    // Scrim — tapping the 10% margins on either side dismisses the panel
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
    ) {
        // 80% panel — centered; HorizontalPager inside naturally consumes pointer events
        // so taps/swipes on the panel do not propagate to the scrim behind it
        Surface(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.82f)
                .align(Alignment.Center),
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 8.dp
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                HabitDetailPane(
                    habitId = habits[page].habit.habitId,
                    onDismiss = onDismiss
                )
            }
        }
    }
}

@Composable
private fun HabitCard(item: LibraryCardItem, onClick: () -> Unit) {
    val borderTier = activeBorderTier(item.progress?.currentTier, item.progress?.tierStatus)
    val cardShape = MaterialTheme.shapes.medium
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (borderTier != null)
                    Modifier.border(2.dp, tierColor(borderTier), cardShape)
                else
                    Modifier
            ),
        shape = cardShape,
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
