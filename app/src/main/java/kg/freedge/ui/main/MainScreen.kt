package kg.freedge.ui.main

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import dev.jeziellago.compose.markdowntext.MarkdownText
import android.view.Surface
import android.view.WindowManager
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) {
            cameraPermission.launchPermissionRequest()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            !cameraPermission.status.isGranted -> {
                PermissionRequest { cameraPermission.launchPermissionRequest() }
            }
            state.imageBytes != null -> {
                ResultScreen(
                    result = state.result,
                    error = state.error,
                    imageBytes = state.imageBytes!!,
                    orientationDegrees = state.orientationDegrees,
                    isLoading = state.isLoading,
                    onRetry = { viewModel.reset() }
                )
            }
            else -> {
                CameraScreen(
                    isLoading = state.isLoading,
                    error = state.error,
                    onImageCaptured = { bytes, degrees -> viewModel.onImageCaptured(bytes, degrees) },
                    onCaptureError = { viewModel.onCaptureError(it) }
                )
            }
        }
    }
}

@Composable
fun PermissionRequest(onRequest: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "📷",
            fontSize = 64.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Нужен доступ к камере",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Чтобы сфоткать холодильник",
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRequest) {
            Text("Разрешить")
        }
    }
}

@Composable
fun CameraScreen(
    isLoading: Boolean,
    error: String?,
    onImageCaptured: (ByteArray, Int) -> Unit,
    onCaptureError: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var cameraProviderRef by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        // Следующий кадр message queue: не склеиваем тяжёлый bindToLifecycle с первым layout (Davey)
                        post {
                            if (!isAttachedToWindow) return@post
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(surfaceProvider)
                            }
                            val capture = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                .build()
                            imageCapture = capture
                            cameraProviderRef = cameraProvider
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                capture
                            )
                        }
                    }, mainExecutor)
                }
            },
            modifier = Modifier.fillMaxSize(),
            onRelease = {
                cameraProviderRef?.unbindAll()
                cameraProviderRef = null
                imageCapture = null
            }
        )

        // Кнопка снимка
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 48.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(72.dp),
                    color = Color.White,
                    strokeWidth = 4.dp
                )
            } else {
                Button(
                    onClick = {
                        imageCapture?.let { capture ->
                            val rotationDegrees = getRotationDegrees(context)
                            val photoFile =
                                File(context.cacheDir, "capture_${System.currentTimeMillis()}.jpg")
                            val outputOptions =
                                ImageCapture.OutputFileOptions.Builder(photoFile).build()
                            capture.takePicture(
                                outputOptions,
                                cameraExecutor,
                                object : ImageCapture.OnImageSavedCallback {
                                    override fun onImageSaved(
                                        output: ImageCapture.OutputFileResults
                                    ) {
                                        val bytes = photoFile.readBytes()
                                        val rotatedBytes = rotateJpegBytes(bytes, rotationDegrees)
                                        photoFile.delete()
                                        ContextCompat.getMainExecutor(context).execute {
                                            onImageCaptured(rotatedBytes, rotationDegrees)
                                        }
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        photoFile.delete()
                                        ContextCompat.getMainExecutor(context).execute {
                                            onCaptureError(
                                                exception.message ?: "Не удалось сохранить снимок"
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    },
                    modifier = Modifier.size(72.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                ) {
                    Text("📸", fontSize = 28.sp)
                }
            }
        }

        // Ошибка
        error?.let {
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFCDD2))
            ) {
                Text(
                    text = it,
                    modifier = Modifier.padding(16.dp),
                    color = Color(0xFFB71C1C)
                )
            }
        }
    }
}

@Composable
fun ResultScreen(
    result: String?,
    error: String?,
    imageBytes: ByteArray?,
    orientationDegrees: Int?,
    isLoading: Boolean,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Фото
        val bytes = imageBytes
        if (bytes != null) {
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        orientationDegrees?.let { degrees ->
            Text(
                text = "Ориентация: ${degrees}°",
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        when {
            error != null -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFCDD2))
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        color = Color(0xFFB71C1C)
                    )
                }
            }
            result != null -> {
                // Результат
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    MarkdownText(
                        markdown = result,
                        modifier = Modifier.padding(16.dp),
                        style = LocalTextStyle.current.copy(
                            fontSize = 16.sp,
                            lineHeight = 24.sp
                        )
                    )
                }
            }
            else -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Кнопка
        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Сфоткать ещё")
        }
    }
}

private fun getRotationDegrees(context: Context): Int {
    val rotation = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.rotation
    return when (rotation) {
        Surface.ROTATION_0 -> 0
        Surface.ROTATION_90 -> 90
        Surface.ROTATION_180 -> 180
        Surface.ROTATION_270 -> 270
        else -> 0
    }
}

private fun rotateJpegBytes(jpegBytes: ByteArray, degrees: Int): ByteArray {
    if (degrees % 360 == 0) return jpegBytes

    val bitmap = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size) ?: return jpegBytes
    val rotated = rotateBitmap(bitmap, degrees.toFloat())

    val stream = ByteArrayOutputStream()
    rotated.compress(Bitmap.CompressFormat.JPEG, 80, stream)
    return stream.toByteArray()
}

private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}