package kg.freedge

import androidx.compose.ui.window.ComposeUIViewController
import kg.freedge.app.AppDependencies
import kg.freedge.app.FreedgeApp
import kg.freedge.core.platform.ConnectivityMonitor
import kg.freedge.core.platform.Haptics
import kg.freedge.core.platform.ImageCompressor
import kg.freedge.core.platform.ImageStorage
import kg.freedge.core.platform.ShareController
import kg.freedge.data.db.buildRoomDatabase
import kg.freedge.data.preferences.OnboardingPreferences
import kg.freedge.data.preferences.buildDataStore
import kg.freedge.data.repo.ScanRepository
import kg.freedge.shared.FreedgeSharedClient
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController {
    FreedgeApp(IosAppDependencies.instance)
}

// Built once per process. Room and DataStore both reject multiple instances over the
// same on-disk file, and nw_path_monitor leaks if started repeatedly — so this object
// guarantees a single set of platform handles even if ComposeUIViewController is
// instantiated multiple times by SwiftUI.
private object IosAppDependencies {
    val instance: AppDependencies by lazy {
        val imageStorage = ImageStorage()
        AppDependencies(
            sharedClient = FreedgeSharedClient(),
            scanRepository = ScanRepository(buildRoomDatabase(), imageStorage),
            onboardingPrefs = OnboardingPreferences(buildDataStore()),
            haptics = Haptics(),
            connectivity = ConnectivityMonitor(),
            share = ShareController(),
            imageStorage = imageStorage,
            imageCompressor = ImageCompressor()
        )
    }
}
