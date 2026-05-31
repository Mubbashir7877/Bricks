package com.pck.bricks.core.navigation

sealed class Screen(val route: String) {
    object Library : Screen("library")
    object CreateHabit : Screen("create_habit")
    object HabitView : Screen("habit/{habitId}") {
        const val ARG_HABIT_ID = "habitId"
        fun createRoute(habitId: String) = "habit/$habitId"
    }
}
