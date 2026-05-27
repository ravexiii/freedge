package kg.freedge.core.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSData
import platform.Foundation.dataWithBytes
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow

actual class ShareController actual constructor() {

    @OptIn(ExperimentalForeignApi::class)
    actual fun shareText(text: String, imageBytes: ByteArray?) {
        val items = mutableListOf<Any>(text)
        imageBytes?.let { bytes ->
            if (bytes.isNotEmpty()) {
                val data = bytes.usePinned { pinned ->
                    NSData.dataWithBytes(pinned.addressOf(0), bytes.size.toULong())
                }
                items.add(data)
            }
        }

        val controller = UIActivityViewController(activityItems = items, applicationActivities = null)
        val rootVc = currentRootViewController() ?: return

        controller.popoverPresentationController?.apply {
            sourceView = rootVc.view
            sourceRect = rootVc.view.bounds.useContents {
                CGRectMake(
                    origin.x + size.width / 2.0,
                    origin.y + size.height / 2.0,
                    0.0,
                    0.0
                )
            }
        }

        rootVc.presentViewController(controller, animated = true, completion = null)
    }

    private fun currentRootViewController(): UIViewController? {
        val keyWindow = UIApplication.sharedApplication.windows
            .filterIsInstance<UIWindow>()
            .lastOrNull()
            ?: UIApplication.sharedApplication.keyWindow

        return keyWindow?.rootViewController?.topMostPresented()
    }

    private tailrec fun UIViewController.topMostPresented(): UIViewController =
        presentedViewController?.topMostPresented() ?: this
}
