package kg.freedge

import android.app.Application
import kg.freedge.app.AppDependencies
import kg.freedge.core.platform.ConnectivityMonitor
import kg.freedge.core.platform.Haptics
import kg.freedge.core.platform.ImageStorage
import kg.freedge.core.platform.ShareController
import kg.freedge.data.db.buildRoomDatabase
import kg.freedge.data.preferences.OnboardingPreferences
import kg.freedge.data.preferences.buildDataStore
import kg.freedge.data.repo.ScanRepository
import kg.freedge.shared.FreedgeSharedClient

class FreedgeApplication : Application() {

    lateinit var deps: AppDependencies
        private set

    override fun onCreate() {
        super.onCreate()
        AppContextHolder.context = applicationContext

        val db = buildRoomDatabase()
        val imageStorage = ImageStorage()
        deps = AppDependencies(
            sharedClient = FreedgeSharedClient(),
            scanRepository = ScanRepository(db, imageStorage),
            onboardingPrefs = OnboardingPreferences(buildDataStore()),
            haptics = Haptics(),
            connectivity = ConnectivityMonitor(),
            share = ShareController(),
            imageStorage = imageStorage
        )
    }
}
