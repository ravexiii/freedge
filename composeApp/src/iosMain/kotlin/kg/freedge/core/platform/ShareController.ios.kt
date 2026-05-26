package kg.freedge.core.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.dataWithBytes
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

actual class ShareController actual constructor() {

    @OptIn(ExperimentalForeignApi::class)
    actual fun shareText(text: String, imageBytes: ByteArray?) {
        val items = mutableListOf<Any>(text)
        imageBytes?.let { bytes ->
            val data = bytes.usePinned { pinned ->
                NSData.dataWithBytes(pinned.addressOf(0), bytes.size.toULong())
            }
            items.add(data)
        }

        val controller = UIActivityViewController(activityItems = items, applicationActivities = null)
        UIApplication.sharedApplication.keyWindow?.rootViewController
            ?.presentViewController(controller, animated = true, completion = null)
    }
}
