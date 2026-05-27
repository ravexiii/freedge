package kg.freedge.feature.main

import android.Manifest
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kg.freedge.app.rememberUiStrings
import kotlinx.coroutines.suspendCancellableCoroutine
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
    val strings = rememberUiStrings()

    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) cameraPermission.launchPermissionRequest()
    }

    if (!cameraPermission.status.isGranted) {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        modifier = Modifier
                            .size(96.dp)
                            .padding(26.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    strings.cameraPermissionTitle,
                    fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    strings.cameraPermissionBody,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                Button(onClick = { cameraPermission.launchPermissionRequest() }) {
                    Text(strings.allow)
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
                        // Return the raw JPEG with EXIF orientation intact.
                        // ImageCompressor handles rotation + downscale + re-encode off this executor.
                        val raw = file.readBytes()
                        file.delete()
                        cont.resume(raw)
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

