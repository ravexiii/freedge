package kg.freedge.feature.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kg.freedge.core.platform.AppConfig
import kg.freedge.core.platform.ConnectivityMonitor
import kg.freedge.core.platform.Haptics
import kg.freedge.data.repo.ScanRepository
import kg.freedge.shared.FreedgeSharedClient
import kg.freedge.shared.RecipeImage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class MainState(
    val isLoading: Boolean = false,
    val result: String? = null,
    val error: String? = null,
    val imageBytes: ByteArray? = null,
    val recipeImages: List<RecipeImage> = emptyList(),
    val isLoadingRecipeImages: Boolean = false
)

class MainViewModel(
    private val sharedClient: FreedgeSharedClient,
    private val scanRepository: ScanRepository,
    private val connectivity: ConnectivityMonitor,
    private val haptics: Haptics
) : ViewModel() {

    private val _state = MutableStateFlow(MainState())
    val state: StateFlow<MainState> = _state

    fun onImageCaptured(bytes: ByteArray) {
        _state.value = _state.value.copy(imageBytes = bytes)
        analyzeImage(bytes)
    }

    private fun analyzeImage(bytes: ByteArray) {
        viewModelScope.launch {
            if (!connectivity.isConnected()) {
                _state.value = _state.value.copy(
                    isLoading = false, error = MainErrorMessages.noInternet
                )
                haptics.performError()
                return@launch
            }

            val groqKey = AppConfig.groqApiKey
            if (groqKey.isBlank()) {
                _state.value = _state.value.copy(
                    isLoading = false, error = MainErrorMessages.missingApiKey
                )
                return@launch
            }

            _state.value = _state.value.copy(isLoading = true, error = null)

            try {
                val analysis = sharedClient.analyzeImage(
                    imageBytes = bytes,
                    groqApiKey = groqKey,
                    languageCode = currentLanguageCode()
                )
                haptics.performSuccess()
                _state.value = _state.value.copy(isLoading = false, result = analysis.displayText)
                scanRepository.saveScan(bytes, analysis.displayText)

                val pexelsKey = AppConfig.pexelsApiKey
                if (pexelsKey.isNotBlank() && analysis.imageQueries.isNotEmpty()) {
                    _state.value = _state.value.copy(isLoadingRecipeImages = true)
                    try {
                        val images = sharedClient.searchRecipeImages(analysis.imageQueries, pexelsKey)
                        _state.value = _state.value.copy(isLoadingRecipeImages = false, recipeImages = images)
                    } catch (_: Throwable) {
                        _state.value = _state.value.copy(isLoadingRecipeImages = false)
                    }
                }
            } catch (e: Throwable) {
                haptics.performError()
                _state.value = _state.value.copy(
                    isLoading = false, error = MainErrorMessages.fromException(e)
                )
            }
        }
    }

    fun onCaptureError(message: String) {
        haptics.performError()
        _state.value = _state.value.copy(isLoading = false, error = message)
    }

    fun clearError() { _state.value = _state.value.copy(error = null) }

    fun reset() { _state.value = MainState() }
}
