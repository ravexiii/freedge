package kg.freedge.ui.main

import android.app.Application
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kg.freedge.analytics.AnalyticsManager
import kg.freedge.app.BuildConfig
import kg.freedge.data.db.FreedgeDatabase
import kg.freedge.data.repo.FreedgeErrorCode
import kg.freedge.data.repo.FreedgeException
import kg.freedge.data.repo.FridgeRepository
import kg.freedge.data.repo.RecipeImage
import kg.freedge.data.repo.RecipeImageQuery
import kg.freedge.data.repo.RecipeImageRepository
import kg.freedge.data.repo.ScanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class MainState(
    val isLoading: Boolean = false,
    val result: String? = null,
    val error: String? = null,
    val imageBytes: ByteArray? = null,
    val recipeImages: List<RecipeImage> = emptyList(),
    val isLoadingRecipeImages: Boolean = false,
    val orientationDegrees: Int? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FridgeRepository()
    private val recipeImageRepository = RecipeImageRepository()
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
                val e = FreedgeException(FreedgeErrorCode.Network)
                analytics.logScanError(MainErrorHandler.analyticsKey(e))
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = MainErrorHandler.userMessage(getApplication(), e)
                )
                return@launch
            }

            val apiKey = BuildConfig.GROQ_API_KEY
            if (apiKey.isBlank()) {
                val e = FreedgeException(FreedgeErrorCode.MissingGroqApiKey)
                analytics.logScanError(MainErrorHandler.analyticsKey(e))
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = MainErrorHandler.userMessage(getApplication(), e)
                )
                return@launch
            }

            _state.value = _state.value.copy(isLoading = true, error = null)

            repository.analyzeImage(bytes, apiKey)
                .onSuccess { analysis ->
                    analytics.logScanSuccess()
                    _state.value = _state.value.copy(
                        isLoading = false,
                        result = analysis.displayText
                    )
                    saveScan(bytes, analysis.displayText)
                    loadRecipeImages(analysis.imageQueries)
                }
                .onFailure { e ->
                    analytics.logScanError(MainErrorHandler.analyticsKey(e))
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = MainErrorHandler.userMessage(getApplication(), e)
                    )
                }
        }
    }

    private fun saveScan(bytes: ByteArray, result: String) {
        viewModelScope.launch {
            scanRepository.saveScan(bytes, result)
        }
    }

    private fun loadRecipeImages(imageQueries: List<RecipeImageQuery>) {
        val apiKey = BuildConfig.PEXELS_API_KEY
        if (apiKey.isBlank() || imageQueries.isEmpty()) return

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoadingRecipeImages = true)
            val images = recipeImageRepository.searchRecipeImages(imageQueries, apiKey)
            _state.value = _state.value.copy(
                isLoadingRecipeImages = false,
                recipeImages = images
            )
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
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

}
