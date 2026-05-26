package kg.freedge.core.platform

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kg.freedge.AppContextHolder

actual class Haptics actual constructor() {

    private val vibrator: Vibrator? by lazy {
        val ctx = AppContextHolder.context
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (ctx.getSystemService(VibratorManager::class.java) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            ctx.getSystemService(Vibrator::class.java)
        }
    }

    actual fun performSuccess() = vibrate(longArrayOf(0, 60, 40, 30))
    actual fun performError() = vibrate(longArrayOf(0, 100, 60, 100))
    actual fun performClick() = vibrate(longArrayOf(0, 20))

    private fun vibrate(pattern: LongArray) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, -1)
        }
    }
}
