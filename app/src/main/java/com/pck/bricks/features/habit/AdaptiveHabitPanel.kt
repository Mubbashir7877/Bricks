package com.pck.bricks.features.habit

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

/**
 * Shows the habit panel with adaptive sizing:
 *  - Partial (82 % width, rounded, scrim behind): tasks incomplete / not scheduled
 *  - Full screen (100 % width, no corners, scrim gone): all tasks complete
 *
 * Transitions between the two states are animated. On initial open the correct
 * size is applied instantly (no partial-flash for already-completed habits).
 */
@Composable
fun AdaptiveHabitPanel(
    habitId: String,
    onDismiss: () -> Unit
) {
    val viewModel: HabitViewModel = viewModel(
        key = habitId,
        factory = HabitViewModel.factory(habitId)
    )
    val uiState by viewModel.uiState.collectAsState()

    val isComplete = uiState.screenState == HabitScreenState.CompletedLocked

    var dataLoaded by remember { mutableStateOf(false) }
    val panelWidth = remember { Animatable(0.82f) }
    val cornerDp   = remember { Animatable(16f) }
    val scrimAlpha = remember { Animatable(0.45f) }

    val expandSpec = tween<Float>(450, easing = FastOutSlowInEasing)

    LaunchedEffect(uiState.screenState, uiState.isLoading) {
        if (!uiState.isLoading) {
            val targetWidth  = if (isComplete) 1f    else 0.82f
            val targetCorner = if (isComplete) 0f    else 16f
            val targetScrim  = if (isComplete) 0f    else 0.45f
            if (!dataLoaded) {
                // First load — jump to correct size instantly so there's no flash
                dataLoaded = true
                panelWidth.snapTo(targetWidth)
                cornerDp.snapTo(targetCorner)
                scrimAlpha.snapTo(targetScrim)
            } else {
                // User action (e.g. checking last task) — smooth animation
                launch { panelWidth.animateTo(targetWidth, expandSpec) }
                launch { cornerDp.animateTo(targetCorner, expandSpec) }
                launch { scrimAlpha.animateTo(targetScrim, tween(450)) }
            }
        }
    }

    BackHandler(onBack = onDismiss)

    Box(modifier = Modifier.fillMaxSize()) {
        // Scrim — only tappable (to dismiss) while in partial mode
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrimAlpha.value))
                .then(
                    if (scrimAlpha.value > 0.02f)
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onDismiss() }
                    else Modifier
                )
        )

        // Panel — animates between 82 % centered and 100 % full-screen
        Surface(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(panelWidth.value)
                .align(Alignment.Center),
            shape = RoundedCornerShape(cornerDp.value.dp),
            tonalElevation = 8.dp
        ) {
            HabitDetailPane(
                habitId      = habitId,
                onDismiss    = onDismiss,
                isFullScreen = panelWidth.value > 0.95f
            )
        }
    }
}
