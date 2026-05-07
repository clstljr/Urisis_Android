package com.example.urisis_android.urinalysis

import com.example.urisis_android.bluetooth.SensorReading
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 6-D feature vector fed into [FuzzyKnnClassifier].
 *
 *   [0] pH       — pH / 14
 *   [1] sg       — (SG - 1.000) / 0.040  (clamped)
 *   [2] cosH     — (cos(hue) + 1) / 2
 *   [3] sinH     — (sin(hue) + 1) / 2
 *   [4] sat      — HSV saturation in [0, 1]
 *   [5] value    — HSV value (brightness) in [0, 1]
 *
 * Hue is split into (cos, sin) to handle the 0°/360° wraparound. Treating
 * raw hue degrees as a scalar would make red samples either side of the
 * wheel look maximally distant despite being the same colour — important
 * for FLAG_A (red/green/brown) detection.
 */
data class FeatureVector(val values: FloatArray) {

    init { require(values.size == DIM) { "Expected $DIM features" } }

    fun distanceTo(other: FeatureVector): Float {
        var sum = 0f
        for (i in 0 until DIM) {
            val d = values[i] - other.values[i]
            sum += d * d
        }
        return sqrt(sum)
    }

    override fun equals(other: Any?): Boolean =
        other is FeatureVector && values.contentEquals(other.values)
    override fun hashCode(): Int = values.contentHashCode()

    companion object { const val DIM = 6 }
}

fun buildFeatures(pH: Float, tdsPpm: Float, r: Int, g: Int, b: Int): FeatureVector {
    val sg = 1.000f + (tdsPpm.coerceAtLeast(0f) / SensorReading.SG_DIVISOR)
    val hsv = SensorReading.rgbToHsv(r, g, b)
    val hueRad = (hsv.hue.toDouble() * PI / 180.0)
    return FeatureVector(
        floatArrayOf(
            (pH / 14f).coerceIn(0f, 1f),
            ((sg - 1.000f) / 0.040f).coerceIn(0f, 1f),
            ((cos(hueRad).toFloat()) + 1f) / 2f,
            ((sin(hueRad).toFloat()) + 1f) / 2f,
            (hsv.saturation / 100f).coerceIn(0f, 1f),
            (hsv.value / 100f).coerceIn(0f, 1f),
        )
    )
}

fun buildFeatures(reading: SensorReading): FeatureVector? {
    val pH = reading.pH ?: return null
    val tds = reading.tdsPpm ?: return null
    val rgb = reading.rgb ?: return null
    return buildFeatures(pH, tds, rgb.r, rgb.g, rgb.b)
}