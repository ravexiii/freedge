package kg.freedge.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kg.freedge.BuildConfig
import kg.freedge.data.repo.FridgeRepository
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

class MainViewModel : ViewModel() {

    private val repository = FridgeRepository()

    private val _state = MutableStateFlow(MainState())
    val state: StateFlow<MainState> = _state

    fun onImageCaptured(bytes: ByteArray, orientationDegrees: Int) {
        _state.value = _state.value.copy(
            imageBytes = bytes,
            orientationDegrees = orientationDegrees
        )
        analyzeImage(bytes)
    }

    private fun analyzeImage(bytes: ByteArray) {
        viewModelScope.launch {
            val apiKey = BuildConfig.GROQ_API_KEY
            if (apiKey.isBlank()) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Добавьте GROQ_API_KEY в local.properties в корне проекта"
                )
                return@launch
            }

            _state.value = _state.value.copy(isLoading = true, error = null)

            repository.analyzeImage(bytes, apiKey)
                .onSuccess { result ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        result = result
                    )
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = e.message ?: "Ошибка"
                    )
                }
        }
    }

    fun reset() {
        _state.value = MainState()
    }

    fun onCaptureError(message: String) {
        _state.value = _state.value.copy(isLoading = false, error = message)
    }
}