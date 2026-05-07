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
 * Real measured channels:
 *   - [pH]      from the pH probe
 *   - [tdsPpm]  from the TDS probe
 *   - [rgb]     from the TCS34725 colour sensor (or ESP32-CAM)
 *   - [tempC]   ambient temperature, used for TDS compensation
 *
 * Derived for UI compatibility:
 *   - [specificGravity] estimated from TDS via SG ≈ 1 + tds/30000.
 *     Real specific gravity needs a refractometer; we expose a derived
 *     value so the existing Specific Gravity card keeps rendering.
 *     Tune SG_DIVISOR if you ever validate against a refractometer —
 *     the constant defines where SG class boundaries land in TDS space.
 *   - [color] HSV computed from [rgb] for the existing colour swatch UI.
 */
data class SensorReading(
    val pH: Float? = null,
    val tdsPpm: Float? = null,
    val tempC: Float? = null,
    val rgb: RgbColor? = null,
) {
    /** Derived: specific gravity from TDS (linear approximation). */
    val specificGravity: Float?
        get() = tdsPpm?.let { 1.000f + (it.coerceAtLeast(0f) / SG_DIVISOR) }

    /** Derived: HSV from RGB so existing UI keeps working. */
    val color: HsvColor?
        get() = rgb?.let { rgbToHsv(it.r, it.g, it.b) }

    /** Have we received the full set of measurements? */
    val isComplete: Boolean
        get() = pH != null && tdsPpm != null && rgb != null

    companion object {
        /**
         * Linear TDS → SG approximation tuned so the classification
         * table's class boundaries (1.005 / 1.010 / 1.020 / 1.030)
         * land at TDS readings of 150 / 300 / 600 / 900 ppm. Retune
         * if you ever validate against a refractometer.
         */
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