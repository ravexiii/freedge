package kg.freedge.ui.history

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kg.freedge.data.db.FreedgeDatabase
import kg.freedge.data.db.ScanEntity
import kg.freedge.data.repo.ScanRepository
import kg.freedge.ui.main.ResultScreen
import kotlinx.coroutines.launch

@Composable
fun ScanDetailScreen(
    scanId: Long,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { ScanRepository(FreedgeDatabase.getInstance(context)) }
    var scan by remember { mutableStateOf<ScanEntity?>(null) }
    var loaded by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    LaunchedEffect(scanId) {
        scope.launch {
            scan = repository.getScanById(scanId)
            loaded = true
        }
    }

    when {
        !loaded -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        scan != null -> {
            ResultScreen(
                result = scan!!.result,
                error = null,
                imageBytes = scan!!.imageBytes,
                isLoading = false,
                onRetry = onBack,
                retryLabel = "Назад"
            )
        }
        else -> onBack()
    }
}
