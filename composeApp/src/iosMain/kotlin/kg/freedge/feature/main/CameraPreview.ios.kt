package kg.freedge.feature.main

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.*
import platform.AVFoundation.*
import platform.Foundation.NSError
import platform.UIKit.*
import platform.darwin.DISPATCH_QUEUE_PRIORITY_DEFAULT
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_global_queue
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun CameraPreview(
    onImageCaptured: (ByteArray) -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier,
    triggerCapture: Boolean,
    onCaptureDone: () -> Unit
) {
    val session = remember { IosCameraSession(onImageCaptured, onError) }

    LaunchedEffect(Unit) { session.start() }

    LaunchedEffect(triggerCapture) {
        if (triggerCapture) {
            session.capturePhoto()
            onCaptureDone()
        }
    }

    UIKitView(
        factory = { session.containerView },
        modifier = modifier,
        update = { view ->
            session.previewLayer?.frame = view.layer.bounds
        },
        onRelease = { session.stop() }
    )
}

@OptIn(ExperimentalForeignApi::class)
class IosCameraSession(
    private val onCaptured: (ByteArray) -> Unit,
    private val onError: (String) -> Unit
) : NSObject(), AVCapturePhotoCaptureDelegate {

    val containerView = UIView()
    var previewLayer: AVCaptureVideoPreviewLayer? = null
    private val captureSession = AVCaptureSession()
    private val photoOutput = AVCapturePhotoOutput()

    fun start() {
        AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
            if (!granted) { onError("Camera permission denied"); return@requestAccessForMediaType }
            setupSession()
        }
    }

    private fun setupSession() {
        captureSession.beginConfiguration()
        captureSession.sessionPreset = AVCaptureSessionPresetPhoto

        val device = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)
            ?: run { onError("No camera found"); captureSession.commitConfiguration(); return }

        val input = memScoped {
            val err = alloc<ObjCObjectVar<NSError?>>()
            AVCaptureDeviceInput.deviceInputWithDevice(device, err.ptr) as? AVCaptureDeviceInput
        } ?: run { onError("Cannot open camera"); captureSession.commitConfiguration(); return }

        if (captureSession.canAddInput(input)) captureSession.addInput(input)
        if (captureSession.canAddOutput(photoOutput)) captureSession.addOutput(photoOutput)
        captureSession.commitConfiguration()

        val layer = AVCaptureVideoPreviewLayer(session = captureSession)
        layer.videoGravity = AVLayerVideoGravityResizeAspectFill
        previewLayer = layer
        containerView.layer.addSublayer(layer)

        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.toLong(), 0u)) {
            captureSession.startRunning()
        }
    }

    fun capturePhoto() {
        val settings = AVCapturePhotoSettings.photoSettings()
        photoOutput.capturePhotoWithSettings(settings, delegate = this)
    }

    fun stop() { captureSession.stopRunning() }

    override fun captureOutput(
        output: AVCapturePhotoOutput,
        didFinishProcessingPhoto: AVCapturePhoto,
        error: NSError?
    ) {
        if (error != null) { onError(error.localizedDescription); return }
        val data = didFinishProcessingPhoto.fileDataRepresentation()
            ?: run { onError("Failed to get image data"); return }
        val bytes = ByteArray(data.length.toInt())
        bytes.usePinned { pinned -> memcpy(pinned.addressOf(0), data.bytes, data.length) }
        onCaptured(bytes)
    }
}
