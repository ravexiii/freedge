package kg.freedge.feature.scandetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kg.freedge.data.db.ScanEntity
import kg.freedge.data.repo.ScanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ScanDetailState(
    val scan: ScanEntity? = null,
    val imageBytes: ByteArray? = null
)

class ScanDetailViewModel(
    private val repository: ScanRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ScanDetailState())
    val state: StateFlow<ScanDetailState> = _state

    fun load(scanId: Long) {
        viewModelScope.launch {
            val scan = repository.getScanById(scanId) ?: return@launch
            val bytes = repository.loadScanImage(scan)
            _state.value = ScanDetailState(scan = scan, imageBytes = bytes)
        }
    }

    fun deleteScan(onDeleted: () -> Unit) {
        val scan = _state.value.scan ?: return
        viewModelScope.launch {
            repository.deleteScan(scan)
            onDeleted()
        }
    }
}
