package kg.freedge.core.platform

import android.content.Intent
import androidx.core.content.FileProvider
import kg.freedge.AppContextHolder
import java.io.File

actual class ShareController actual constructor() {

    actual fun shareText(text: String, imageBytes: ByteArray?) {
        val ctx = AppContextHolder.context
        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }

        imageBytes?.let { bytes ->
            runCatching {
                val file = File(ctx.cacheDir, "freedge_share_${System.currentTimeMillis()}.jpg")
                file.writeBytes(bytes)
                val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.provider", file)
                intent.putExtra(Intent.EXTRA_STREAM, uri)
                intent.type = "image/jpeg"
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }

        ctx.startActivity(Intent.createChooser(intent, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
