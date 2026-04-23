package com.example.urisis_android.bluetooth

data class HsvColor(
    val hue: Float,         // 0..360°
    val saturation: Float,  // 0..100%
    val value: Float        // 0..100%
)

data class SensorReading(
    val pH: Float? = null,
    val specificGravity: Float? = null,
    val color: HsvColor? = null
) {
    val isComplete: Boolean
        get() = pH != null && specificGravity != null && color != null
}