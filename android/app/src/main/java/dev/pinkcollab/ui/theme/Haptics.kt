package dev.pinkcollab.ui.theme

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Decorates a click with Android's light key-tap feedback while keeping the callback stable
 * across recompositions. Platform haptic settings remain authoritative.
 */
@Composable
internal fun rememberHapticOnClick(onClick: () -> Unit): () -> Unit {
    val haptics = LocalHapticFeedback.current
    val currentOnClick = rememberUpdatedState(onClick)
    return remember(haptics) {
        {
            haptics.performHapticFeedback(HapticFeedbackType.KeyboardTap)
            currentOnClick.value()
        }
    }
}

/**
 * 回复完成的细微震动（Grok 风格）：两下极轻的"嗒-嗒"，克制不打扰。
 * 用短波形（0/18/40/18ms）配合小振幅，感知为"轻轻一顿"。
 */
@SuppressLint("MissingPermission")
internal fun performCompletionHaptic(context: android.content.Context) {
    val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        (context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE)
            as? android.os.VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator
    } ?: return
    if (!vibrator.hasVibrator()) return
    val timings = longArrayOf(0, 16, 40, 16)
    val amplitudes = intArrayOf(0, 80, 0, 80)
    runCatching {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator.vibrate(android.os.VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(android.os.VibrationEffect.createWaveform(timings, -1))
        }
    }
}
