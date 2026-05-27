package kg.freedge.feature.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.mikepenz.markdown.m3.Markdown
import kg.freedge.app.LocalAppDeps
import kg.freedge.app.UiStrings
import kg.freedge.app.rememberUiStrings
import kg.freedge.core.platform.ShareController
import kg.freedge.feature.components.ShareCopyRow
import kg.freedge.shared.RecipeImage

@Composable
fun MainScreen(onNavigateToHistory: () -> Unit) {
    val deps = LocalAppDeps.current
    val strings = rememberUiStrings()
    val vm = viewModel<MainViewModel> {
        MainViewModel(deps.sharedClient, deps.scanRepository, deps.connectivity, deps.haptics, deps.imageCompressor)
    }
    val state by vm.state.collectAsState()

    val hasImage = state.imageBytes != null
    val screenKey = if (hasImage) "result" else "camera"

    AnimatedContent(
        targetState = screenKey,
        transitionSpec = {
            if (targetState == "result") {
                fadeIn(tween(350)) + slideInVertically(tween(350)) { it / 4 } togetherWith fadeOut(tween(200))
            } else {
                fadeIn(tween(300)) togetherWith fadeOut(tween(250)) + slideOutVertically(tween(350)) { it / 4 }
            }
        },
        modifier = Modifier.fillMaxSize(),
        label = "main_screen"
    ) { screen ->
        when (screen) {
            "result" -> ResultScreen(
                state = state,
                share = deps.share,
                onRetry = { vm.reset() },
                strings = strings,
                onNavigateToHistory = onNavigateToHistory
            )
            else -> CameraScreen(
                isLoading = state.isLoading,
                error = state.error,
                strings = strings,
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
    strings: UiStrings,
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
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            IconButton(
                onClick = onNavigateToHistory,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .background(Color.Black.copy(alpha = 0.35f), CircleShape)
            ) {
                Icon(Icons.Default.History, contentDescription = strings.history, tint = Color.White)
            }
        }

        if (!isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .padding(bottom = 40.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                ShutterButton(onClick = { triggerCapture = true })
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }

        error?.let { errorMessage ->
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 56.dp, start = 16.dp, end = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onClearError,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Text(strings.retry)
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
            .border(3.dp, Color.White.copy(alpha = 0.86f), CircleShape)
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
    strings: UiStrings = rememberUiStrings(),
    onNavigateToHistory: (() -> Unit)? = null,
    showBackArrow: Boolean = false,
    retryLabel: String = strings.takeAnother
) {
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
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp, bottom = 8.dp)
        ) {
            state.imageBytes?.let { bytes ->
                PastelSection(
                    title = strings.photo,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    AsyncImage(
                        model = bytes,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 214.dp, max = 360.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            if (state.isLoadingRecipeImages || state.recipeImages.isNotEmpty()) {
                RecipeImagesSection(state.recipeImages, state.isLoadingRecipeImages, strings)
                Spacer(Modifier.height(16.dp))
            }

            when {
                state.error != null -> ErrorSection(state.error)
                state.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                state.result != null -> {
                    PastelSection(
                        title = strings.recipeIdeas,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ) {
                        Markdown(content = state.result, modifier = Modifier.padding(top = 2.dp))
                    }

                    Spacer(Modifier.height(12.dp))

                    ShareCopyRow(
                        text = state.result,
                        imageBytes = state.imageBytes,
                        share = share
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!showBackArrow && onNavigateToHistory != null) {
                OutlinedButton(
                    onClick = onNavigateToHistory,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(strings.history, textAlign = TextAlign.Center)
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
                Text(retryLabel, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun ErrorSection(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

@Composable
private fun RecipeImagesSection(images: List<RecipeImage>, isLoading: Boolean, strings: UiStrings) {
    PastelSection(
        title = strings.servingIdeas,
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        if (isLoading && images.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(142.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(end = 2.dp)) {
                items(images, key = { it.imageUrl }) { image ->
                    Column(modifier = Modifier.width(190.dp)) {
                        Text(image.title, style = MaterialTheme.typography.labelLarge, maxLines = 2, lineHeight = 18.sp)
                        Spacer(Modifier.height(8.dp))
                        AsyncImage(
                            model = image.imageUrl,
                            contentDescription = image.query,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            strings.photoBy(image.photographer),
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
private fun PastelSection(
    title: String,
    containerColor: Color,
    contentColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
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
