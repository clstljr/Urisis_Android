package com.example.urisis_android.bluetooth

import kotlin.math.max
import kotlin.math.min

data class HsvColor(
    val hue: Float,         // 0..360°
    val saturation: Float,  // 0..100%
    val value: Float        // 0..100%
)

data class RgbColor(
    val r: Int,             // 0..255
    val g: Int,             // 0..255
    val b: Int,             // 0..255
    val hex: String? = null,
    val lux: Float? = null,
    val cct: Int? = null,
)

/**
 * One reading from the Arduino.
 *
 * Two independent colour channels:
 *   - [sensorRgb]  — TCS34725 colour sensor (reflected light off a strip)
 *   - [cameraRgb]  — ESP32-CAM frame (direct sample colour)
 *
 * The classifier uses [classifierRgb] which prefers camera over sensor
 * because the camera sees the actual sample colour rather than reflected
 * light off a chemistry strip.
 *
 * Real measured channels:
 *   - [pH]       from the pH probe
 *   - [tdsPpm]   from the TDS probe
 *   - [tempC]    ambient temperature
 *
 * Derived for UI compatibility:
 *   - [specificGravity] estimated from TDS via SG ≈ 1 + tds/30000.
 *   - [color] HSV computed from [classifierRgb].
 *   - [rgb] backward-compatible alias for [classifierRgb].
 */
data class SensorReading(
    val pH: Float? = null,
    val tdsPpm: Float? = null,
    val tempC: Float? = null,
    val sensorRgb: RgbColor? = null,
    val cameraRgb: RgbColor? = null,
) {
    /** RGB the classifier used. Camera wins when present. */
    val classifierRgb: RgbColor?
        get() = cameraRgb ?: sensorRgb

    /** Backward-compatible alias for screens that read [rgb]. */
    val rgb: RgbColor?
        get() = classifierRgb

    /** Derived: SG from TDS (linear approximation). */
    val specificGravity: Float?
        get() = tdsPpm?.let { 1.000f + (it.coerceAtLeast(0f) / SG_DIVISOR) }

    /** Derived: HSV from the classifier's chosen RGB. */
    val color: HsvColor?
        get() = classifierRgb?.let { rgbToHsv(it.r, it.g, it.b) }

    val isComplete: Boolean
        get() = pH != null && tdsPpm != null && classifierRgb != null

    /** True if both colour sources are present. */
    val hasBothColorSources: Boolean
        get() = sensorRgb != null && cameraRgb != null

    companion object {
        const val SG_DIVISOR = 30_000f

        fun rgbToHsv(r: Int, g: Int, b: Int): HsvColor {
            val rf = r.coerceIn(0, 255) / 255f
            val gf = g.coerceIn(0, 255) / 255f
            val bf = b.coerceIn(0, 255) / 255f
            val mx = max(rf, max(gf, bf))
            val mn = min(rf, min(gf, bf))
            val d  = mx - mn
            val h  = when {
                d < 1e-6f  -> 0f
                mx == rf   -> 60f * (((gf - bf) / d) % 6f)
                mx == gf   -> 60f * (((bf - rf) / d) + 2f)
                else       -> 60f * (((rf - gf) / d) + 4f)
            }
            val hue = if (h < 0f) h + 360f else h
            val sat = if (mx < 1e-6f) 0f else d / mx
            return HsvColor(
                hue = hue,
                saturation = sat * 100f,
                value = mx * 100f,
            )
        }
    }
}