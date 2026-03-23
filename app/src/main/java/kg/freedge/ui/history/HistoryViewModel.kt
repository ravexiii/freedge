package kg.freedge.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kg.freedge.analytics.AnalyticsManager
import kg.freedge.data.db.FreedgeDatabase
import kg.freedge.data.db.ScanEntity
import kg.freedge.data.repo.ScanRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ScanRepository(FreedgeDatabase.getInstance(application))
    private val analytics = AnalyticsManager(application)

    val scans: StateFlow<List<ScanEntity>> = repository.getAllScans()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        analytics.logHistoryOpened()
    }

    fun deleteScan(scan: ScanEntity) {
        viewModelScope.launch {
            repository.deleteScan(scan)
        }
    }

    fun onScanViewed() {
        analytics.logScanViewed()
    }
}
