package com.narezany.calculator.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

/**
 * Haptic feedback for key presses.
 *
 * Two layers are used on purpose: the platform's own key tick
 * ([HapticFeedbackConstants.VIRTUAL_KEY]) and a short predefined vibration,
 * which makes the feedback noticeable on phones with a weak actuator.
 * Vibration is wrapped in try/catch because some OEM ROMs deny it when the
 * user disabled touch vibration in the system settings.
 */
class CalculatorHaptics(
    private val view: View,
    private val vibrator: Vibrator?,
) {

    fun tick(strong: Boolean = false) {
        val constant = if (strong && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.CONFIRM
        } else {
            HapticFeedbackConstants.VIRTUAL_KEY
        }
        view.performHapticFeedback(constant)

        val v = vibrator ?: return
        if (!v.hasVibrator()) return

        try {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> v.vibrate(
                    VibrationEffect.createPredefined(
                        if (strong) VibrationEffect.EFFECT_CLICK else VibrationEffect.EFFECT_TICK
                    )
                )

                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> v.vibrate(
                    VibrationEffect.createOneShot(
                        if (strong) 24L else 12L,
                        if (strong) 120 else 70
                    )
                )

                else -> legacyVibrate(v, if (strong) 24L else 12L)
            }
        } catch (_: Exception) {
            // Ignore: haptics must never break the calculator.
        }
    }

    @Suppress("DEPRECATION")
    private fun legacyVibrate(vibrator: Vibrator, durationMs: Long) {
        vibrator.vibrate(durationMs)
    }
}

@Composable
fun rememberCalculatorHaptics(): CalculatorHaptics {
    val view = LocalView.current
    val context = LocalContext.current

    val vibrator = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            val service = context.getSystemService(Context.VIBRATOR_SERVICE)
            service as? Vibrator
        }
    }

    return remember(view, vibrator) { CalculatorHaptics(view, vibrator) }
}
