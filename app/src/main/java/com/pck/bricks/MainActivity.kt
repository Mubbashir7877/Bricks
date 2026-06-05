package com.pck.bricks

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pck.bricks.core.navigation.Screen
import com.pck.bricks.features.creation.CreateHabitScreen
import com.pck.bricks.features.habit.HabitViewScreen
import com.pck.bricks.features.library.LibraryScreen
import com.pck.bricks.ui.theme.BricksTheme

class MainActivity : ComponentActivity() {

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* result not critical */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                    composable(
                        route = Screen.HabitView.route,
                        arguments = listOf(
                            navArgument(Screen.HabitView.ARG_HABIT_ID) { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val habitId = backStackEntry.arguments
                            ?.getString(Screen.HabitView.ARG_HABIT_ID)
                            ?: return@composable
                        HabitViewScreen(habitId = habitId, navController = navController)
                    }
                }
            }
        }
    }
}
