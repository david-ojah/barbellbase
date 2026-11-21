package com.david.barbellbase.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.david.barbellbase.ui.exercises.ExerciseDetailScreen
import com.david.barbellbase.ui.exercises.ExerciseLibraryScreen
import com.david.barbellbase.ui.habits.HabitsScreen
import com.david.barbellbase.ui.history.HistoryScreen
import com.david.barbellbase.ui.history.SessionDetailScreen
import com.david.barbellbase.ui.home.HomeScreen
import com.david.barbellbase.ui.progress.ProgressScreen
import com.david.barbellbase.ui.reminders.RemindersScreen
import com.david.barbellbase.ui.routines.DayEditorScreen
import com.david.barbellbase.ui.routines.RoutineDetailScreen
import com.david.barbellbase.ui.routines.RoutineTemplatesScreen
import com.david.barbellbase.ui.routines.RoutinesScreen
import com.david.barbellbase.ui.settings.SettingsScreen
import com.david.barbellbase.ui.workout.WorkoutScreen

object Routes {
    const val HOME = "home"
    const val PROGRESS = "progress"
    const val HABITS = "habits"
    const val REMINDERS = "reminders"

    const val ROUTINES = "routines"
    const val ROUTINE_TEMPLATES = "routine-templates"
    const val ROUTINE = "routine/{routineId}"
    const val DAY = "day/{dayId}"
    const val HISTORY = "history"
    const val SESSION = "session/{sessionId}"
    const val EXERCISES = "exercises"
    const val EXERCISE = "exercise/{exerciseId}"
    const val WORKOUT = "workout/{sessionId}"
    const val SETTINGS = "settings"

    fun routine(id: String) = "routine/$id"
    fun day(id: String) = "day/$id"
    fun session(id: String) = "session/$id"
    fun exercise(id: String) = "exercise/$id"
    fun workout(id: String) = "workout/$id"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, "Home", Icons.Default.Home),
    Tab(Routes.PROGRESS, "Progress", Icons.Default.Insights),
    Tab(Routes.HABITS, "Habits", Icons.Default.WaterDrop),
    Tab(Routes.REMINDERS, "Reminders", Icons.Default.NotificationsActive),
)

@Composable
fun BarbellBaseRoot() {
    val navController = rememberNavController()
    val current by navController.currentBackStackEntryAsState()
    val route = current?.destination?.route
    val showBottomBar = tabs.any { it.route == route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onOpenWorkout = { id -> navController.navigate(Routes.workout(id)) },
                    onOpenRoutines = { navController.navigate(Routes.ROUTINES) },
                    onOpenSession = { id -> navController.navigate(Routes.session(id)) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    onOpenHistory = { navController.navigate(Routes.HISTORY) },
                    onOpenExercises = { navController.navigate(Routes.EXERCISES) },
                )
            }

            composable(Routes.PROGRESS) { ProgressScreen() }

            composable(Routes.HABITS) { HabitsScreen() }

            composable(Routes.REMINDERS) { RemindersScreen() }

            composable(Routes.ROUTINES) {
                RoutinesScreen(
                    onOpenRoutine = { id -> navController.navigate(Routes.routine(id)) },
                    onBrowseTemplates = { navController.navigate(Routes.ROUTINE_TEMPLATES) },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.ROUTINE_TEMPLATES) {
                RoutineTemplatesScreen(
                    onBack = { navController.popBackStack() },
                    onImported = { id ->
                        navController.navigate(Routes.routine(id)) {
                            popUpTo(Routes.ROUTINE_TEMPLATES) { inclusive = true }
                        }
                    },
                )
            }

            composable(
                route = Routes.ROUTINE,
                arguments = listOf(navArgument("routineId") { type = NavType.StringType }),
            ) {
                RoutineDetailScreen(
                    onOpenDay = { id -> navController.navigate(Routes.day(id)) },
                    onStartDay = { sessionId -> navController.navigate(Routes.workout(sessionId)) },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(
                route = Routes.DAY,
                arguments = listOf(navArgument("dayId") { type = NavType.StringType }),
            ) {
                DayEditorScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.HISTORY) {
                HistoryScreen(
                    onOpenSession = { id -> navController.navigate(Routes.session(id)) },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(
                route = Routes.SESSION,
                arguments = listOf(navArgument("sessionId") { type = NavType.StringType }),
            ) {
                SessionDetailScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.EXERCISES) {
                ExerciseLibraryScreen(
                    onOpenExercise = { id -> navController.navigate(Routes.exercise(id)) },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(
                route = Routes.EXERCISE,
                arguments = listOf(navArgument("exerciseId") { type = NavType.StringType }),
            ) {
                ExerciseDetailScreen(onBack = { navController.popBackStack() })
            }

            composable(
                route = Routes.WORKOUT,
                arguments = listOf(navArgument("sessionId") { type = NavType.StringType }),
            ) {
                WorkoutScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
