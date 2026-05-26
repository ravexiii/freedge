package kg.freedge.feature.scandetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.mikepenz.markdown.m3.Markdown
import kg.freedge.app.LocalAppDeps
import kg.freedge.feature.main.isRussian

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanDetailScreen(scanId: Long, onBack: () -> Unit) {
    val deps = LocalAppDeps.current
    val vm = viewModel<ScanDetailViewModel> { ScanDetailViewModel(deps.scanRepository) }
    val state by vm.state.collectAsState()
    val isRu = isRussian()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(scanId) { vm.load(scanId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isRu) "История" else "History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Outlined.Delete, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                .padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            state.imageBytes?.let { bytes ->
                AsyncImage(
                    model = bytes,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp, max = 360.dp).clip(RoundedCornerShape(12.dp))
                )
            }

            state.scan?.let { scan ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Markdown(content = scan.result)
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(if (isRu) "Удалить скан?" else "Delete scan?") },
            text = { Text(if (isRu) "Фото и рецепт будут удалены." else "Photo and recipe will be deleted.") },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; vm.deleteScan(onBack) }) {
                    Text(if (isRu) "Удалить" else "Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(if (isRu) "Отмена" else "Cancel") }
            }
        )
    }
}
