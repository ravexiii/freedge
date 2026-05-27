package kg.freedge.core.platform

import platform.Network.nw_path_get_status
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_status_satisfied
import platform.darwin.dispatch_queue_create

actual class ConnectivityMonitor actual constructor() {

    // Optimistic default: nw_path_monitor delivers the real status asynchronously after start().
    // Seeding to true avoids a false "no internet" error if the user triggers a network call
    // before the first path update arrives; an actual offline state surfaces as a regular
    // network error from the HTTP client instead.
    @Volatile
    private var connected: Boolean = true

    private val monitor = nw_path_monitor_create()

    init {
        nw_path_monitor_set_update_handler(monitor) { path ->
            connected = nw_path_get_status(path) == nw_path_status_satisfied
        }
        nw_path_monitor_start(monitor, dispatch_queue_create("freedge.connectivity", null))
    }

    actual fun isConnected(): Boolean = connected
}
