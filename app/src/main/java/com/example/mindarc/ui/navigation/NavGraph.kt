package com.example.mindarc.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.mindarc.ui.screen.ActivitySelectionScreen
import com.example.mindarc.ui.screen.AppProvidedReadingScreen
import com.example.mindarc.ui.screen.AppSelectionScreen
import com.example.mindarc.ui.screen.HomeScreen
import com.example.mindarc.ui.screen.LockWarningScreen
import com.example.mindarc.ui.screen.PermissionsScreen
import com.example.mindarc.ui.screen.ProgressScreen
import com.example.mindarc.ui.screen.BreathingActivityScreen
import com.example.mindarc.ui.screen.AuthScreen
import com.example.mindarc.ui.screen.PlankHoldActivityScreen
import com.example.mindarc.ui.screen.PushupsActivityScreen
import com.example.mindarc.ui.screen.SquatsActivityScreen
import com.example.mindarc.ui.screen.ReadingActivityScreen
import com.example.mindarc.ui.screen.PongLevelSelectionScreen
import com.example.mindarc.ui.screen.PongGameScreen
import com.example.mindarc.ui.screen.SpeedDialChallengeScreen
import com.example.mindarc.ui.screen.StepsWalkedActivityScreen
import com.example.mindarc.ui.screen.TraceToEarnScreen
import com.example.mindarc.ui.screen.UserProvidedReadingScreen
import com.example.mindarc.ui.screen.OnboardingQuestionnaireScreen

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Auth : Screen("auth")
    data object Home : Screen("home")
    data object Permissions : Screen("permissions")
    data object AppSelection : Screen("app_selection")
    data object ActivitySelection : Screen("activity_selection")
    data object PushupsActivity : Screen("pushups_activity")
    data object SquatsActivity : Screen("squats_activity")
    data object PlankHoldActivity : Screen("plank_hold_activity")
    data object StepsWalkedActivity : Screen("steps_walked_activity")
    data object BreathingActivity : Screen("breathing_activity")
    data object ReadingActivity : Screen("reading_activity")
    data object AppProvidedReading : Screen("app_provided_reading")
    data object UserProvidedReading : Screen("user_provided_reading")
    data object SpeedDialChallenge : Screen("speed_dial_challenge")
    data object PongLevelSelection : Screen("pong_level_selection")
    data object PongGame : Screen("pong_game/{difficulty}") {
        fun createRoute(difficulty: String) = "pong_game/$difficulty"
    }
    data object TraceToEarn : Screen("trace_to_earn")
    data object Progress : Screen("progress")
    data object LockWarning : Screen("lock_warning/{packageName}") {
        fun createRoute(packageName: String) = "lock_warning/$packageName"
    }
}

@Composable
fun NavGraph(navController: NavHostController, startDestination: String) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.Onboarding.route) {
            OnboardingQuestionnaireScreen(navController = navController)
        }
        composable(Screen.Auth.route) {
            AuthScreen(navController = navController)
        }
        composable(Screen.Permissions.route) {
            PermissionsScreen(navController)
        }
        composable(Screen.Home.route) {
            HomeScreen(navController = navController)
        }
        composable(Screen.AppSelection.route) {
            AppSelectionScreen(navController = navController)
        }
        composable(Screen.ActivitySelection.route) {
            ActivitySelectionScreen(navController = navController)
        }
        composable(Screen.PushupsActivity.route) {
            PushupsActivityScreen(navController = navController)
        }
        composable(Screen.SquatsActivity.route) {
            SquatsActivityScreen(navController = navController)
        }
        composable(Screen.PlankHoldActivity.route) {
            PlankHoldActivityScreen(navController = navController)
        }
        composable(Screen.StepsWalkedActivity.route) {
            StepsWalkedActivityScreen(navController = navController)
        }
        composable(Screen.BreathingActivity.route) {
            BreathingActivityScreen(navController = navController)
        }
        composable(Screen.ReadingActivity.route) {
            ReadingActivityScreen(navController = navController)
        }
        composable(Screen.AppProvidedReading.route) {
            AppProvidedReadingScreen(navController = navController)
        }
        composable(Screen.UserProvidedReading.route) {
            UserProvidedReadingScreen(navController = navController)
        }
        composable(Screen.SpeedDialChallenge.route) {
            SpeedDialChallengeScreen(navController = navController)
        }
        composable(Screen.TraceToEarn.route) {
            TraceToEarnScreen(navController = navController)
        }
        composable(Screen.PongLevelSelection.route) {
            PongLevelSelectionScreen(navController = navController)
        }
        composable(
            route = Screen.PongGame.route,
            arguments = listOf(navArgument("difficulty") { type = NavType.StringType })
        ) { backStackEntry ->
            val difficulty = backStackEntry.arguments?.getString("difficulty") ?: "easy"
            PongGameScreen(difficulty = difficulty, navController = navController)
        }
        composable(Screen.Progress.route) {
            ProgressScreen(navController = navController)
        }
        composable(
            route = Screen.LockWarning.route,
            arguments = listOf(navArgument("packageName") { type = NavType.StringType })
        ) { backStackEntry ->
            val packageName = backStackEntry.arguments?.getString("packageName") ?: ""
            LockWarningScreen(navController = navController, packageName = packageName)
        }
    }
}
