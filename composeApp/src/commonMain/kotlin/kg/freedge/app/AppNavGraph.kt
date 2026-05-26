package kg.freedge.app

import androidx.compose.runtime.*
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kg.freedge.feature.history.HistoryScreen
import kg.freedge.feature.main.MainScreen
import kg.freedge.feature.onboarding.OnboardingScreen
import kg.freedge.feature.scandetail.ScanDetailScreen

@Composable
fun AppNavGraph() {
    val deps = LocalAppDeps.current
    val onboardingCompleted by deps.onboardingPrefs.isCompleted.collectAsState(initial = null)

    val startDestination = when (onboardingCompleted) {
        true -> "camera"
        false -> "onboarding"
        null -> return
    }

    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = startDestination) {
        composable("onboarding") {
            OnboardingScreen(
                onComplete = {
                    navController.navigate("camera") { popUpTo("onboarding") { inclusive = true } }
                }
            )
        }
        composable("camera") {
            MainScreen(onNavigateToHistory = { navController.navigate("history") })
        }
        composable("history") {
            HistoryScreen(
                onBack = { navController.popBackStack() },
                onScanClick = { navController.navigate("scan_detail/$it") }
            )
        }
        composable(
            "scan_detail/{scanId}",
            arguments = listOf(navArgument("scanId") { type = NavType.LongType })
        ) { backStack ->
            val scanId = backStack.arguments?.getLong("scanId") ?: return@composable
            ScanDetailScreen(scanId = scanId, onBack = { navController.popBackStack() })
        }
    }
}
