package kg.freedge

import androidx.compose.ui.window.ComposeUIViewController
import kg.freedge.app.AppDependencies
import kg.freedge.app.FreedgeApp
import kg.freedge.core.platform.ConnectivityMonitor
import kg.freedge.core.platform.Haptics
import kg.freedge.core.platform.ImageStorage
import kg.freedge.core.platform.ShareController
import kg.freedge.data.db.buildRoomDatabase
import kg.freedge.data.preferences.OnboardingPreferences
import kg.freedge.data.preferences.buildDataStore
import kg.freedge.data.repo.ScanRepository
import kg.freedge.shared.FreedgeSharedClient
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    val db = buildRoomDatabase()
    val imageStorage = ImageStorage()
    val deps = AppDependencies(
        sharedClient = FreedgeSharedClient(),
        scanRepository = ScanRepository(db, imageStorage),
        onboardingPrefs = OnboardingPreferences(buildDataStore()),
        haptics = Haptics(),
        connectivity = ConnectivityMonitor(),
        share = ShareController(),
        imageStorage = imageStorage
    )

    return ComposeUIViewController {
        FreedgeApp(deps)
    }
}
