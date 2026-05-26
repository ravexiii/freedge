package kg.freedge.feature.main

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.mikepenz.markdown.m3.Markdown
import kg.freedge.app.LocalAppDeps
import kg.freedge.core.platform.ShareController
import kg.freedge.shared.RecipeImage

@Composable
fun MainScreen(onNavigateToHistory: () -> Unit) {
    val deps = LocalAppDeps.current
    val vm = viewModel<MainViewModel> {
        MainViewModel(deps.sharedClient, deps.scanRepository, deps.connectivity, deps.haptics)
    }
    val state by vm.state.collectAsState()

    val hasImage = state.imageBytes != null
    val screenKey = if (hasImage) "result" else "camera"

    AnimatedContent(
        targetState = screenKey,
        transitionSpec = {
            if (targetState == "result") fadeIn(tween(350)) + slideInVertically(tween(350)) { it / 4 } togetherWith
                fadeOut(tween(200))
            else fadeIn(tween(300)) togetherWith fadeOut(tween(250)) + slideOutVertically(tween(350)) { it / 4 }
        },
        modifier = Modifier.fillMaxSize(),
        label = "main_screen"
    ) { screen ->
        when (screen) {
            "result" -> ResultScreen(
                state = state,
                share = deps.share,
                onRetry = { vm.reset() },
                onNavigateToHistory = onNavigateToHistory
            )
            else -> CameraScreen(
                isLoading = state.isLoading,
                error = state.error,
                onImageCaptured = { bytes -> vm.onImageCaptured(bytes) },
                onCaptureError = { vm.onCaptureError(it) },
                onClearError = { vm.clearError() },
                onNavigateToHistory = onNavigateToHistory
            )
        }
    }
}

@Composable
private fun CameraScreen(
    isLoading: Boolean,
    error: String?,
    onImageCaptured: (ByteArray) -> Unit,
    onCaptureError: (String) -> Unit,
    onClearError: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    var triggerCapture by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        CameraPreview(
            onImageCaptured = {
                triggerCapture = false
                onImageCaptured(it)
            },
            onError = {
                triggerCapture = false
                onCaptureError(it)
            },
            modifier = Modifier.fillMaxSize(),
            triggerCapture = triggerCapture,
            onCaptureDone = { triggerCapture = false }
        )

        Box(
            modifier = Modifier.fillMaxWidth().statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            IconButton(
                onClick = onNavigateToHistory,
                modifier = Modifier.align(Alignment.TopEnd)
                    .background(Color.Black.copy(alpha = 0.35f), CircleShape)
            ) {
                Icon(Icons.Default.History, contentDescription = null, tint = Color.White)
            }
        }

        if (!isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().navigationBarsPadding().padding(bottom = 40.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                ShutterButton(onClick = { triggerCapture = true })
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }

        error?.let {
            Card(
                modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding()
                    .padding(top = 56.dp, start = 16.dp, end = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFCDD2)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(it, color = Color(0xFFB71C1C), textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onClearError, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C))) {
                        Text(if (isRussian()) "Попробовать снова" else "Retry", color = Color.White)
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
    val scale by animateFloatAsState(if (isPressed) 0.92f else 1f, tween(120), label = "shutter")

    Box(
        modifier = Modifier
            .size(72.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .border(3.dp, MaterialTheme.colorScheme.primaryContainer, CircleShape)
            .padding(6.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary, CircleShape)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
    )
}

@Composable
fun ResultScreen(
    state: MainState,
    share: ShareController,
    onRetry: () -> Unit,
    onNavigateToHistory: (() -> Unit)? = null,
    showBackArrow: Boolean = false,
    retryLabel: String = if (isRussian()) "Сфотографировать ещё" else "Take another"
) {
    val clipboard = LocalClipboardManager.current

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeDrawingPadding()
    ) {
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp).padding(top = 16.dp, bottom = 8.dp)
        ) {
            state.imageBytes?.let { bytes ->
                PastelSection(
                    title = if (isRussian()) "Снимок" else "Photo",
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    AsyncImage(
                        model = bytes,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 214.dp, max = 360.dp).clip(RoundedCornerShape(8.dp))
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            if (state.isLoadingRecipeImages || state.recipeImages.isNotEmpty()) {
                RecipeImagesSection(state.recipeImages, state.isLoadingRecipeImages)
                Spacer(Modifier.height(16.dp))
            }

            when {
                state.error != null -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Text(state.error, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
                state.isLoading -> {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                state.result != null -> {
                    PastelSection(
                        title = if (isRussian()) "Идеи для готовки" else "Recipe ideas",
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ) {
                        Markdown(content = state.result, modifier = Modifier.padding(top = 2.dp))
                    }

                    Spacer(Modifier.height(12.dp))

                    val shareFooter = if (isRussian()) "\n\n-\nСгенерировано в Freedge" else "\n\n-\nGenerated in Freedge"
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = { clipboard.setText(AnnotatedString(state.result)) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (isRussian()) "Скопировать" else "Copy")
                        }
                        Button(
                            onClick = { share.shareText(state.result + shareFooter, state.imageBytes) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (isRussian()) "Поделиться" else "Share")
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!showBackArrow && onNavigateToHistory != null) {
                OutlinedButton(onClick = onNavigateToHistory, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) {
                    Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (isRussian()) "История" else "History")
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
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text(retryLabel)
            }
        }
    }
}

@Composable
private fun RecipeImagesSection(images: List<RecipeImage>, isLoading: Boolean) {
    PastelSection(
        title = if (isRussian()) "Пример подачи" else "Inspiration",
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        if (isLoading && images.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(142.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp), color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(end = 2.dp)) {
                items(images, key = { it.imageUrl }) { image ->
                    Column(modifier = Modifier.width(190.dp)) {
                        Text(image.title, style = MaterialTheme.typography.labelLarge, maxLines = 2, lineHeight = 18.sp)
                        Spacer(Modifier.height(8.dp))
                        AsyncImage(
                            model = image.imageUrl, contentDescription = image.query,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "${if (isRussian()) "Фото:" else "Photo:"} ${image.photographer} / Pexels",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.72f),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PastelSection(title: String, containerColor: Color, contentColor: Color, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = contentColor.copy(alpha = 0.78f))
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}
