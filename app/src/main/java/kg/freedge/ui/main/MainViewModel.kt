package kg.freedge.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kg.freedge.data.repo.FridgeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class MainState(
    val isLoading: Boolean = false,
    val result: String? = null,
    val error: String? = null,
    val imageBytes: ByteArray? = null
)

class MainViewModel : ViewModel() {

    private val repository = FridgeRepository()

    private val _state = MutableStateFlow(MainState())
    val state: StateFlow<MainState> = _state

    // TODO: Replace with your API key
    private val apiKey = "AIzaSyAHly6CDB0IXBu4jKUXe941KcbL_gIgrwk"

    fun onImageCaptured(bytes: ByteArray) {
        _state.value = _state.value.copy(imageBytes = bytes)
        analyzeImage(bytes)
    }

    private fun analyzeImage(bytes: ByteArray) {
        viewModelScope.launch {
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
}