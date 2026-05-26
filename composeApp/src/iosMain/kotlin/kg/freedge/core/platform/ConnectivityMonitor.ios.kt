package kg.freedge.core.platform

import platform.Network.nw_path_get_status
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_status_satisfied
import platform.Network.nw_path_get_status
import platform.darwin.dispatch_queue_create

actual class ConnectivityMonitor actual constructor() {

    @Volatile
    private var connected: Boolean = true

    init {
        val monitor = nw_path_monitor_create()
        nw_path_monitor_set_update_handler(monitor) { path ->
            connected = nw_path_get_status(path) == nw_path_status_satisfied
        }
        nw_path_monitor_start(monitor, dispatch_queue_create("connectivity", null))
    }

    actual fun isConnected(): Boolean = connected
}
