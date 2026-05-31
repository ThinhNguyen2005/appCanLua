package com.GiaThinh.canlua.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Haptic feedback utility for confirming user actions (save, lock, error).
 * Uses VibrationEffect on API 26+, legacy vibrate below.
 */
object HapticUtil {

    private fun getVibrator(context: Context): Vibrator {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    /** Heavy click — khi lưu thành công, chốt giao dịch */
    fun confirm(context: Context) {
        val vibrator = getVibrator(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(50)
        }
    }

    /** Light tick — khi nhập số, chuyển tab */
    fun tick(context: Context) {
        val vibrator = getVibrator(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(20)
        }
    }

    /** Error pattern — khi validation lỗi */
    fun error(context: Context) {
        val vibrator = getVibrator(context)
        val pattern = longArrayOf(0, 80, 100, 80)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createWaveform(pattern, -1)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }
    }

    /**
     * Texture tick — phản hồi cực nhẹ khi gõ từng chữ số trong input lớn.
     * Dùng EFFECT_TICK (API 29+) — pattern haptic dịu hơn cho gõ phím liên tục.
     */
    fun textHandleMove(context: Context) {
        val vibrator = getVibrator(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(10, 60)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(10)
        }
    }

    /** Phản hồi hoàn thành cột lúa — rung 2 nhịp ngắn cách nhau 80ms */
    fun success(context: Context) {
        val vibrator = getVibrator(context)
        val pattern = longArrayOf(0, 80, 80, 80)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createWaveform(pattern, -1)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }
    }
}
