package kg.freedge.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kg.freedge.data.db.ScanEntity
import kg.freedge.data.repo.ScanRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val repository: ScanRepository
) : ViewModel() {

    val scans: StateFlow<List<ScanEntity>> = repository.getAllScans()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun deleteScan(scan: ScanEntity) {
        viewModelScope.launch { repository.deleteScan(scan) }
    }
}
