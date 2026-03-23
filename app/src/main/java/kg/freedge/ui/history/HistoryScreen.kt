package kg.freedge.ui.history

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kg.freedge.data.db.ScanEntity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    onScanClick: (Long) -> Unit,
    viewModel: HistoryViewModel = viewModel()
) {
    val scans by viewModel.scans.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("История сканов") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (scans.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📷", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Нет сохранённых сканов", fontSize = 16.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(scans, key = { it.id }) { scan ->
                    ScanCard(
                        scan = scan,
                        onClick = {
                            viewModel.onScanViewed()
                            onScanClick(scan.id)
                        },
                        onDelete = { viewModel.deleteScan(scan) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ScanCard(
    scan: ScanEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val bitmap = remember(scan.id) {
        BitmapFactory.decodeByteArray(scan.imageBytes, 0, scan.imageBytes.size)
    }
    val dateStr = remember(scan.createdAt) {
        SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            .format(Date(scan.createdAt))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            bitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dateStr,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = scan.result,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 14.sp
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Удалить"
                )
            }
        }
    }
}
