package dev.wearstral.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

// ALL THE BITS!!!!!! (or HAPTICS?)

enum class Haptic { Tick, Click, HeavyClick }

fun Context.haptic(haptic: Haptic) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    } ?: return
    if (!vibrator.hasVibrator()) return

    val (duration, amplitude) = when (haptic) {
        Haptic.Tick -> 15 to 72
        Haptic.Click -> 25 to 140
        Haptic.HeavyClick -> 40 to 220
    }
    val predefinedSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            vibrator.areAllEffectsSupported(
                VibrationEffect.EFFECT_TICK,
                VibrationEffect.EFFECT_CLICK,
                VibrationEffect.EFFECT_HEAVY_CLICK
            ) == Vibrator.VIBRATION_EFFECT_SUPPORT_YES
    if (predefinedSupported) {
        val effect = when (haptic) {
            Haptic.Tick -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
            Haptic.Click -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
            Haptic.HeavyClick -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
        }
        vibrator.vibrate(effect)
    } else {
        vibrator.vibrate(
            VibrationEffect.createWaveform(
                longArrayOf(0, duration.toLong()),
                intArrayOf(0, amplitude),
                -1
            )
        )
    }
}


/**
 * Meaning in plain english:
 * Send (typed or voice final) -> Click
 * Reply lands -> Tick
 * Send failed -> HeavyClick
 * Panel opens / closes	Tick -> Tick
 * Mic starts / voice failed -> Tick / HeavyClick
 * Model download done / failed	-> Click / HeavyClick
 * Clear-history / confirm -> Tick / HeavyClick
 * Long-press popups -> existing platform haptics
 */
