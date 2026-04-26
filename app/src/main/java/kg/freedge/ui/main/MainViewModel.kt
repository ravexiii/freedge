package kg.freedge.ui.main

import android.app.Application
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kg.freedge.app.R
import kg.freedge.app.BuildConfig
import kg.freedge.analytics.AnalyticsManager
import kg.freedge.data.db.FreedgeDatabase
import kg.freedge.data.repo.FridgeRepository
import kg.freedge.data.repo.ScanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class MainState(
    val isLoading: Boolean = false,
    val result: String? = null,
    val error: String? = null,
    val imageBytes: ByteArray? = null,
    val orientationDegrees: Int? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FridgeRepository()
    private val scanRepository = ScanRepository(FreedgeDatabase.getInstance(application), application)
    private val analytics = AnalyticsManager(application)

    private val _state = MutableStateFlow(MainState())
    val state: StateFlow<MainState> = _state

    fun onImageCaptured(bytes: ByteArray, orientationDegrees: Int) {
        analytics.logScanStarted()
        _state.value = _state.value.copy(
            imageBytes = bytes,
            orientationDegrees = orientationDegrees
        )
        analyzeImage(bytes)
    }

    private fun analyzeImage(bytes: ByteArray) {
        viewModelScope.launch {
            if (!isNetworkAvailable()) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = getApplication<Application>().getString(R.string.error_no_internet)
                )
                analytics.logScanError("no_network")
                return@launch
            }

            val apiKey = BuildConfig.GROQ_API_KEY
            if (apiKey.isBlank()) {
                analytics.logScanError("missing_api_key")
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Добавьте GROQ_API_KEY в local.properties в корне проекта"
                )
                return@launch
            }

            _state.value = _state.value.copy(isLoading = true, error = null)

            repository.analyzeImage(bytes, apiKey)
                .onSuccess { result ->
                    analytics.logScanSuccess()
                    _state.value = _state.value.copy(isLoading = false, result = result)
                    saveScan(bytes, result)
                }
                .onFailure { e ->
                    analytics.logScanError(e.javaClass.simpleName)
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = e.message
                            ?: getApplication<Application>().getString(R.string.error_generic)
                    )
                }
        }
    }

    private fun saveScan(bytes: ByteArray, result: String) {
        viewModelScope.launch {
            scanRepository.saveScan(bytes, result)
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun reset() {
        _state.value = MainState()
    }

    fun onCaptureError(message: String) {
        analytics.logScanError("capture_error")
        _state.value = _state.value.copy(isLoading = false, error = message)
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = getApplication<Application>()
            .getSystemService(ConnectivityManager::class.java) ?: return false
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
