package kg.freedge.feature.main

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun CameraPreview(
    onImageCaptured: (ByteArray) -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier,
    triggerCapture: Boolean = false,
    onCaptureDone: () -> Unit = {}
)
