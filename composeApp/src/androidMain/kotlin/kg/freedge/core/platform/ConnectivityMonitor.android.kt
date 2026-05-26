package kg.freedge.core.platform

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kg.freedge.AppContextHolder

actual class ConnectivityMonitor actual constructor() {
    actual fun isConnected(): Boolean {
        val cm = AppContextHolder.context.getSystemService(ConnectivityManager::class.java) ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
