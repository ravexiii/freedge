package kg.freedge.feature.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import kg.freedge.app.LocalAppDeps
import kg.freedge.data.db.ScanEntity
import kg.freedge.feature.main.isRussian

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    onScanClick: (Long) -> Unit
) {
    val deps = LocalAppDeps.current
    val vm = viewModel<HistoryViewModel> { HistoryViewModel(deps.scanRepository) }
    val scans by vm.scans.collectAsState()
    val isRu = isRussian()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isRu) "История" else "History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        if (scans.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("📷", fontSize = 48.sp)
                    Text(
                        if (isRu) "Пока нет сканов" else "No scans yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(scans, key = { it.id }) { scan ->
                    ScanItem(
                        scan = scan,
                        onClick = { onScanClick(scan.id) },
                        onDelete = { vm.deleteScan(scan) },
                        loadImage = { deps.scanRepository.loadScanImage(scan) },
                        isRu = isRu
                    )
                }
            }
        }
    }
}

@Composable
private fun ScanItem(
    scan: ScanEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    loadImage: suspend () -> ByteArray?,
    isRu: Boolean
) {
    var showDialog by remember { mutableStateOf(false) }
    val imageBytes by produceState<ByteArray?>(null, scan.imageFileName) {
        value = loadImage()
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (imageBytes != null) {
                AsyncImage(
                    model = imageBytes,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp))
                )
            } else {
                Box(
                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text("📷", fontSize = 24.sp)
                }
            }

            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(
                    formatDate(scan.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stripMarkdown(scan.result),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = { showDialog = true }) {
                Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (isRu) "Удалить скан?" else "Delete scan?") },
            text = { Text(if (isRu) "Фото и рецепт будут удалены из истории." else "Photo and recipe will be removed.") },
            confirmButton = {
                TextButton(onClick = { showDialog = false; onDelete() }) {
                    Text(if (isRu) "Удалить" else "Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text(if (isRu) "Отмена" else "Cancel") }
            }
        )
    }
}

expect fun formatDate(timestamp: Long): String
expect fun stripMarkdown(text: String): String
