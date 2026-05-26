package kg.freedge.core.platform

expect class ConnectivityMonitor() {
    fun isConnected(): Boolean
}
