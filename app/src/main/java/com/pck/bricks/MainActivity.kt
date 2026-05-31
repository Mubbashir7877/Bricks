package com.pck.bricks

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        enableEdgeToEdge()
        setContent {
            BricksTheme {
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
