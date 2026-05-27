package kg.freedge.feature.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kg.freedge.app.rememberUiStrings
import kg.freedge.core.platform.ShareController

/**
 * Copy-to-clipboard + native share row.
 *
 * [text] is the recipe body. A localised "Generated in Freedge" footer is appended
 * to shared text but NOT to clipboard text, matching the legacy behaviour.
 */
@Composable
fun ShareCopyRow(
    text: String,
    imageBytes: ByteArray?,
    share: ShareController,
    modifier: Modifier = Modifier
) {
    val strings = rememberUiStrings()
    val clipboard = LocalClipboardManager.current
    val shareFooter = "\n\n${strings.generatedInFreedge}"

    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(
            onClick = { clipboard.setText(AnnotatedString(text)) },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(strings.copy, textAlign = TextAlign.Center)
        }
        Button(
            onClick = { share.shareText(text + shareFooter, imageBytes) },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        ) {
            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(strings.share, textAlign = TextAlign.Center)
        }
    }
}
