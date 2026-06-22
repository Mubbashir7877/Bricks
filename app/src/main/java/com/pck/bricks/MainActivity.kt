package com.pck.bricks

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pck.bricks.core.navigation.Screen
import com.pck.bricks.features.creation.CreateHabitScreen
import com.pck.bricks.features.habit.AdaptiveHabitPanel
import com.pck.bricks.features.library.LibraryScreen
import com.pck.bricks.features.notifications.NotificationBuilder
import com.pck.bricks.ui.theme.BricksTheme

class MainActivity : ComponentActivity() {

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* result not critical */ }

    private val pendingHabitId = mutableStateOf<String?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingHabitId.value = intent.getStringExtra(NotificationBuilder.EXTRA_HABIT_ID)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingHabitId.value = intent.getStringExtra(NotificationBuilder.EXTRA_HABIT_ID)
        enableEdgeToEdge()
        setContent {
            BricksTheme {
                var showNotifRationale by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    ) {
                        if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                            showNotifRationale = true
                        } else {
                            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                if (showNotifRationale) {
                    AlertDialog(
                        onDismissRequest = { showNotifRationale = false },
                        title = { Text("Stay on track") },
                        text = {
                            Text(
                                "Bricks sends a daily reminder for each habit so you never " +
                                    "forget to log your progress. Allow notifications to get " +
                                    "the most out of the app."
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                showNotifRationale = false
                                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }) { Text("Allow") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showNotifRationale = false }) { Text("Not now") }
                        }
                    )
                }

                val navController = rememberNavController()
                val habitIdToOpen by pendingHabitId

                // Keep last habit ID alive during the slide-out exit animation
                var panelHabitId by remember { mutableStateOf<String?>(null) }
                LaunchedEffect(habitIdToOpen) {
                    if (habitIdToOpen != null) panelHabitId = habitIdToOpen
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Library.route
                    ) {
                        composable(Screen.Library.route) {
                            LibraryScreen(navController = navController)
                        }
                        composable(Screen.CreateHabit.route) {
                            CreateHabitScreen(navController = navController)
                        }
                    }

                    // Global overlay — shown when a notification or widget tap carries a habit ID
                    AnimatedVisibility(
                        visible = habitIdToOpen != null,
                        enter = slideInHorizontally(animationSpec = tween(350)) { it } + fadeIn(tween(200)),
                        exit  = slideOutHorizontally(animationSpec = tween(300)) { it } + fadeOut(tween(200))
                    ) {
                        panelHabitId?.let { id ->
                            AdaptiveHabitPanel(
                                habitId   = id,
                                onDismiss = { pendingHabitId.value = null }
                            )
                        }
                    }
                }
            }
        }
    }
}
