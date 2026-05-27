package kg.freedge.core.platform

import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle
import platform.UIKit.UINotificationFeedbackGenerator
import platform.UIKit.UINotificationFeedbackType
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

actual class Haptics actual constructor() {

    actual fun performSuccess() = onMain {
        UINotificationFeedbackGenerator().notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeSuccess)
    }

    actual fun performError() = onMain {
        UINotificationFeedbackGenerator().notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeError)
    }

    actual fun performClick() = onMain {
        UIImpactFeedbackGenerator(style = UIImpactFeedbackStyle.UIImpactFeedbackStyleLight).impactOccurred()
    }

    // UIFeedbackGenerator subclasses must be created and invoked on the main thread.
    private inline fun onMain(crossinline block: () -> Unit) {
        dispatch_async(dispatch_get_main_queue()) { block() }
    }
}
