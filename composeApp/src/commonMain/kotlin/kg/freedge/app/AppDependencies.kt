package kg.freedge.app

import androidx.compose.runtime.staticCompositionLocalOf
import kg.freedge.core.platform.ConnectivityMonitor
import kg.freedge.core.platform.Haptics
import kg.freedge.core.platform.ImageStorage
import kg.freedge.core.platform.ShareController
import kg.freedge.data.preferences.OnboardingPreferences
import kg.freedge.data.repo.ScanRepository
import kg.freedge.shared.FreedgeSharedClient

class AppDependencies(
    val sharedClient: FreedgeSharedClient,
    val scanRepository: ScanRepository,
    val onboardingPrefs: OnboardingPreferences,
    val haptics: Haptics,
    val connectivity: ConnectivityMonitor,
    val share: ShareController,
    val imageStorage: ImageStorage
)

val LocalAppDeps = staticCompositionLocalOf<AppDependencies> {
    error("AppDependencies not provided")
}
