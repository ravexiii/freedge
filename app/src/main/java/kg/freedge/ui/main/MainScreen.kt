package kg.freedge.ui.main

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import dev.jeziellago.compose.markdowntext.MarkdownText
import kg.freedge.app.R
import kg.freedge.data.repo.RecipeImage
import kg.freedge.utils.HapticUtils
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainScreen(
    onNavigateToHistory: () -> Unit = {},
    viewModel: MainViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) {
            cameraPermission.launchPermissionRequest()
        }
    }

    // Haptic on scan success
    val prevResult = remember { mutableStateOf<String?>(null) }
    LaunchedEffect(state.result) {
        if (state.result != null && prevResult.value == null) {
            HapticUtils.performSuccess(context)
        }
        prevResult.value = state.result
    }

    val screenKey = when {
        !cameraPermission.status.isGranted -> "permission"
        state.imageBytes != null -> "result"
        else -> "camera"
    }

    AnimatedContent(
        targetState = screenKey,
        transitionSpec = {
            if (targetState == "result") {
                (fadeIn(tween(350)) + slideInVertically(tween(350)) { it / 4 }) togetherWith
                    fadeOut(tween(200))
            } else {
                fadeIn(tween(300)) togetherWith
                    (fadeOut(tween(250)) + slideOutVertically(tween(350)) { it / 4 })
            }
        },
        modifier = Modifier.fillMaxSize(),
        label = "screen"
    ) { screen ->
        when (screen) {
            "permission" -> PermissionRequest { cameraPermission.launchPermissionRequest() }
            "result" -> ResultScreen(
                result = state.result,
                error = state.error,
                imageBytes = state.imageBytes,
                isLoading = state.isLoading,
                recipeImages = state.recipeImages,
                isLoadingRecipeImages = state.isLoadingRecipeImages,
                onRetry = { viewModel.reset() },
                onNavigateToHistory = onNavigateToHistory
            )
            else -> CameraScreen(
                isLoading = state.isLoading,
                error = state.error,
                onImageCaptured = { bytes, degrees -> viewModel.onImageCaptured(bytes, degrees) },
                onCaptureError = { viewModel.onCaptureError(it) },
                onClearError = { viewModel.clearError() },
                onNavigateToHistory = onNavigateToHistory
            )
        }
    }
}

@Composable
fun PermissionRequest(onRequest: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 1.dp
        ) {
            Text(
                text = "📷",
                fontSize = 54.sp,
                modifier = Modifier.padding(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.camera_permission_title),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.camera_permission_desc),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRequest) { Text(stringResource(R.string.permission_allow)) }
    }
}

@Composable
fun CameraScreen(
    isLoading: Boolean,
    error: String?,
    onImageCaptured: (ByteArray, Int) -> Unit,
    onCaptureError: (String) -> Unit,
    onClearError: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val view = LocalView.current
    val errorGeneric = stringResource(R.string.error_generic)
    val retryLabel = stringResource(R.string.retry)

    // Haptic on camera error
    LaunchedEffect(error) {
        if (error != null) HapticUtils.performError(context)
    }

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var cameraProviderRef by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }

    DisposableEffect(Unit) {
        onDispose {
            cameraProviderRef?.unbindAll()
            cameraExecutor.shutdown()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera preview — full screen
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
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

        // History button — top right
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            IconButton(
                onClick = onNavigateToHistory,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .background(Color.Black.copy(alpha = 0.35f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = stringResource(R.string.history_title),
                    tint = Color.White
                )
            }
        }

        // Shutter button — bottom (hidden while loading)
        if (!isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .padding(bottom = 40.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                ShutterButton(
                    onClick = {
                        HapticUtils.performClick(view)
                        imageCapture?.let { capture ->
                            val photoFile = File(
                                context.cacheDir,
                                "capture_${System.currentTimeMillis()}.jpg"
                            )
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
                                        val rotatedBytes = rotateJpegBytes(bytes, getExifRotationDegrees(photoFile))
                                        photoFile.delete()
                                        ContextCompat.getMainExecutor(context).execute {
                                            onImageCaptured(rotatedBytes, 0)
                                        }
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        photoFile.delete()
                                        ContextCompat.getMainExecutor(context).execute {
                                            onCaptureError(exception.message ?: errorGeneric)
                                        }
                                    }
                                }
                            )
                        }
                    }
                )
            }
        }

        // Full-screen loading overlay
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                LoadingAnimation()
            }
        }

        // Error card with retry
        error?.let { errorMessage ->
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 56.dp, start = 16.dp, end = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFCDD2)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = errorMessage,
                        color = Color(0xFFB71C1C),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onClearError,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C))
                    ) {
                        Text(retryLabel, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun ShutterButton(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "shutterScale"
    )

    Box(
        modifier = Modifier
            .size(72.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .border(3.dp, MaterialTheme.colorScheme.primaryContainer, CircleShape)
            .padding(6.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary, CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = MaterialTheme.colorScheme.primaryContainer),
                onClick = onClick
            )
    )
}

@Composable
fun ResultScreen(
    result: String?,
    error: String?,
    imageBytes: ByteArray?,
    isLoading: Boolean,
    recipeImages: List<RecipeImage> = emptyList(),
    isLoadingRecipeImages: Boolean = false,
    onRetry: () -> Unit,
    onNavigateToHistory: (() -> Unit)? = null,
    showBackArrow: Boolean = false,
    retryLabel: String = stringResource(R.string.take_another)
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val copiedMsg = stringResource(R.string.copied)
    val shareFooter = stringResource(R.string.share_footer)
    val shareChooserTitle = stringResource(R.string.share_chooser_title)
    var expandedImageBytes by remember { mutableStateOf<ByteArray?>(null) }
    val contentAlpha by animateFloatAsState(
        targetValue = if (result != null || error != null || isLoading) 1f else 0.85f,
        animationSpec = tween(350),
        label = "resultContentAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .graphicsLayer { alpha = contentAlpha }
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp, bottom = 8.dp)
        ) {
            imageBytes?.let { bytes ->
                val bitmap = remember(bytes) {
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }
                bitmap?.let {
                    PastelSection(
                        title = stringResource(R.string.result_photo),
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 214.dp, max = 360.dp)
                                .aspectRatio(
                                    ratio = it.width.toFloat() / it.height.toFloat(),
                                    matchHeightConstraintsFirst = false
                                )
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { expandedImageBytes = bytes }
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            if (isLoadingRecipeImages || recipeImages.isNotEmpty()) {
                RecipeImagesSection(
                    images = recipeImages,
                    isLoading = isLoadingRecipeImages
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            when {
                error != null -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = error,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                result != null -> {
                    PastelSection(
                        title = stringResource(R.string.result_recipes),
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ) {
                        MarkdownText(
                            markdown = result,
                            modifier = Modifier.padding(top = 2.dp),
                            style = LocalTextStyle.current.copy(
                                fontSize = 16.sp,
                                lineHeight = 24.sp
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(result))
                                Toast.makeText(context, copiedMsg, Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.copy))
                        }

                        Button(
                            onClick = { shareRecipe(context, result, imageBytes, shareFooter, shareChooserTitle) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.share))
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!showBackArrow && onNavigateToHistory != null) {
                OutlinedButton(
                    onClick = onNavigateToHistory,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.history_short))
                }
            }

            Button(
                onClick = onRetry,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                if (showBackArrow) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(retryLabel)
            }
        }
    }

    expandedImageBytes?.let { bytes ->
        PhotoPreviewDialog(
            imageBytes = bytes,
            onDismiss = { expandedImageBytes = null }
        )
    }
}

@Composable
private fun PhotoPreviewDialog(
    imageBytes: ByteArray,
    onDismiss: () -> Unit
) {
    val bitmap = remember(imageBytes) {
        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f))
                .safeDrawingPadding()
        ) {
            bitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 56.dp)
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .background(Color.White.copy(alpha = 0.16f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.back),
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun RecipeImagesSection(
    images: List<RecipeImage>,
    isLoading: Boolean
) {
    PastelSection(
        title = stringResource(R.string.result_inspiration),
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        if (isLoading && images.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(142.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(end = 2.dp)
            ) {
                items(images, key = { it.imageUrl }) { image ->
                    RecipeImageCard(image = image)
                }
            }
        }
    }
}

@Composable
private fun RecipeImageCard(image: RecipeImage) {
    Column(
        modifier = Modifier.width(190.dp)
    ) {
        Text(
            text = image.title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            maxLines = 2,
            lineHeight = 18.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        AsyncImage(
            model = image.imageUrl,
            contentDescription = image.query,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(8.dp))
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.photo_by, image.photographer),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.72f),
            maxLines = 1
        )
    }
}

@Composable
private fun PastelSection(
    title: String,
    containerColor: Color,
    contentColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor.copy(alpha = 0.78f)
            )
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

private fun shareRecipe(
    context: Context,
    text: String,
    imageBytes: ByteArray?,
    footer: String,
    chooserTitle: String
) {
    val shareText = "$text$footer"

    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, shareText)
        type = "text/plain"
    }

    imageBytes?.let { bytes ->
        try {
            val file = File(context.cacheDir, "freedge_share_${System.currentTimeMillis()}.jpg")
            file.writeBytes(bytes)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            sendIntent.putExtra(Intent.EXTRA_STREAM, uri)
            sendIntent.type = "image/jpeg"
            sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: Exception) {
            // share text only
        }
    }

    context.startActivity(Intent.createChooser(sendIntent, chooserTitle))
}

private fun getExifRotationDegrees(file: File): Int =
    try {
        when (
            ExifInterface(file.absolutePath).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        ) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    } catch (_: Exception) {
        0
    }

private fun rotateJpegBytes(jpegBytes: ByteArray, degrees: Int): ByteArray {
    if (degrees % 360 == 0) return jpegBytes
    val bitmap =
        BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size) ?: return jpegBytes
    val rotated = rotateBitmap(bitmap, degrees.toFloat())
    val stream = ByteArrayOutputStream()
    rotated.compress(Bitmap.CompressFormat.JPEG, 80, stream)
    return stream.toByteArray()
}

private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}
