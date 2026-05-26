package kg.freedge.core.platform

import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle
import platform.UIKit.UINotificationFeedbackGenerator
import platform.UIKit.UINotificationFeedbackType

actual class Haptics actual constructor() {

    actual fun performSuccess() {
        UINotificationFeedbackGenerator().notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeSuccess)
    }

    actual fun performError() {
        UINotificationFeedbackGenerator().notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeError)
    }

    actual fun performClick() {
        UIImpactFeedbackGenerator(style = UIImpactFeedbackStyle.UIImpactFeedbackStyleLight).impactOccurred()
    }
}
