package kg.freedge.ui.navigation

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kg.freedge.data.preferences.OnboardingPreferences
import kg.freedge.ui.history.HistoryScreen
import kg.freedge.ui.history.ScanDetailScreen
import kg.freedge.ui.main.MainScreen
import kg.freedge.ui.onboarding.OnboardingScreen

@Composable
fun AppNavGraph() {
    val context = LocalContext.current
    val onboardingCompleted by remember { OnboardingPreferences(context).isCompleted }
        .collectAsState(initial = null)

    // Ждём пока прочитаем DataStore (null = ещё не прочитано)
    val startDestination = when (onboardingCompleted) {
        true -> "camera"
        false -> "onboarding"
        null -> return  // показываем пустой экран пока загружается
    }

    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = startDestination) {
        composable("onboarding") {
            OnboardingScreen(
                onComplete = {
                    navController.navigate("camera") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }
        composable("camera") {
            MainScreen(
                onNavigateToHistory = { navController.navigate("history") }
            )
        }
        composable("history") {
            HistoryScreen(
                onBack = { navController.popBackStack() },
                onScanClick = { scanId -> navController.navigate("scan_detail/$scanId") }
            )
        }
        composable(
            route = "scan_detail/{scanId}",
            arguments = listOf(navArgument("scanId") { type = NavType.LongType })
        ) { backStackEntry ->
            val scanId = backStackEntry.arguments?.getLong("scanId") ?: return@composable
            ScanDetailScreen(
                scanId = scanId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
