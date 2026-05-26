package kg.freedge.feature.main

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@OptIn(ExperimentalPermissionsApi::class)
@Composable
actual fun CameraPreview(
    onImageCaptured: (ByteArray) -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier,
    triggerCapture: Boolean,
    onCaptureDone: () -> Unit
) {
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    val isRu = isRussian()

    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) cameraPermission.launchPermissionRequest()
    }

    if (!cameraPermission.status.isGranted) {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                Text("📷", fontSize = 52.sp)
                Spacer(Modifier.height(16.dp))
                Text(
                    if (isRu) "Нужен доступ к камере" else "Camera access required",
                    fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    if (isRu) "Чтобы сфотографировать холодильник" else "To photograph your fridge",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                Button(onClick = { cameraPermission.launchPermissionRequest() }) {
                    Text(if (isRu) "Разрешить" else "Allow")
                }
            }
        }
        return
    }

    CameraPreviewWithPermission(
        onImageCaptured = onImageCaptured,
        onError = onError,
        modifier = modifier,
        triggerCapture = triggerCapture,
        onCaptureDone = onCaptureDone
    )
}

@Composable
private fun CameraPreviewWithPermission(
    onImageCaptured: (ByteArray) -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier,
    triggerCapture: Boolean,
    onCaptureDone: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var providerRef by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    val executor = remember { Executors.newSingleThreadExecutor() }

    LaunchedEffect(triggerCapture) {
        if (!triggerCapture) return@LaunchedEffect
        val capture = imageCapture ?: run { onCaptureDone(); return@LaunchedEffect }
        val file = File(context.cacheDir, "cap_${System.currentTimeMillis()}.jpg")
        try {
            val bytes = suspendCancellableCoroutine<ByteArray> { cont ->
                val opts = ImageCapture.OutputFileOptions.Builder(file).build()
                capture.takePicture(opts, executor, object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        if (!cont.isActive) { file.delete(); return }
                        val raw = file.readBytes()
                        val deg = exifRotation(file)
                        file.delete()
                        cont.resume(rotateJpeg(raw, deg))
                    }
                    override fun onError(e: ImageCaptureException) {
                        if (!cont.isActive) { file.delete(); return }
                        file.delete()
                        cont.resumeWithException(e)
                    }
                })
                cont.invokeOnCancellation { file.delete() }
            }
            onCaptureDone()
            onImageCaptured(bytes)
        } catch (e: Exception) {
            file.delete()
            onCaptureDone()
            onError(e.message ?: "Capture failed")
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            providerRef?.unbindAll()
            executor.shutdown()
        }
    }

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                val future = ProcessCameraProvider.getInstance(ctx)
                future.addListener({
                    val provider = future.get()
                    val preview = Preview.Builder().build().also { it.setSurfaceProvider(surfaceProvider) }
                    val capture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()
                    imageCapture = capture
                    providerRef = provider
                    provider.unbindAll()
                    provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture)
                }, ContextCompat.getMainExecutor(ctx))
            }
        },
        modifier = modifier,
        onRelease = {
            providerRef?.unbindAll()
            providerRef = null
            imageCapture = null
        }
    )
}

private fun exifRotation(file: File): Int = try {
    when (ExifInterface(file.absolutePath).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90
        ExifInterface.ORIENTATION_ROTATE_180 -> 180
        ExifInterface.ORIENTATION_ROTATE_270 -> 270
        else -> 0
    }
} catch (_: Exception) { 0 }

private fun rotateJpeg(bytes: ByteArray, degrees: Int): ByteArray {
    if (degrees % 360 == 0) return bytes
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return bytes
    val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
    val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    return ByteArrayOutputStream().also { rotated.compress(Bitmap.CompressFormat.JPEG, 85, it) }.toByteArray()
}
