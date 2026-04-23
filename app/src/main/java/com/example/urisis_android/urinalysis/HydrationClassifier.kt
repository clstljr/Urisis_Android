package com.example.urisis_android.urinalysis

import com.example.urisis_android.bluetooth.HsvColor
import com.example.urisis_android.bluetooth.SensorReading
import kotlin.math.abs

object HydrationClassifier {

    fun classify(r: SensorReading): UrinalysisResult? {
        if (!r.isComplete) return null
        val pH = r.pH!!
        val sg = r.specificGravity!!
        val color = r.color!!

        // ── Specific gravity score: 0 = dilute, 1 = very concentrated ──
        val sgScore = when {
            sg < 1.010f -> 0.10f
            sg < 1.015f -> 0.30f
            sg < 1.020f -> 0.55f
            sg < 1.025f -> 0.75f
            else        -> 0.90f
        }

        // ── Color score: pale = hydrated, deep amber = dehydrated ──
        // Higher saturation + lower value → more concentrated urine
        val satScore = (color.saturation / 100f).coerceIn(0f, 1f)
        val darkScore = (1f - color.value / 100f).coerceIn(0f, 1f)
        val colorScore = satScore * 0.6f + darkScore * 0.4f

        // Weighted combination — SG dominates
        val combined = sgScore * 0.6f + colorScore * 0.4f

        val status = when {
            combined < 0.25f -> HydrationStatus.WELL_HYDRATED
            combined < 0.50f -> HydrationStatus.NORMAL
            combined < 0.72f -> HydrationStatus.MILDLY_DEHYDRATED
            else             -> HydrationStatus.DEHYDRATED
        }

        // Confidence = distance from the nearest decision boundary
        val boundaries = listOf(0.25f, 0.50f, 0.72f)
        val distance = boundaries.minOf { abs(combined - it) }
        val confidence = (0.75f + distance * 1.2f).coerceIn(0.60f, 0.99f)

        return UrinalysisResult(
            reading = r,
            status = status,
            confidence = confidence,
            pHInRange = pH in UrinalysisResult.PH_MIN..UrinalysisResult.PH_MAX,
            sgInRange = sg in UrinalysisResult.SG_MIN..UrinalysisResult.SG_MAX,
            colorLabel = colorLabel(color)
        )
    }

    private fun colorLabel(c: HsvColor): String = when {
        c.value > 90f && c.saturation < 15f -> "Clear"
        c.value > 85f && c.saturation < 30f -> "Pale Yellow"
        c.value > 75f && c.saturation < 50f -> "Pale — Amber"
        c.value > 65f && c.saturation < 70f -> "Amber"
        c.value > 50f                       -> "Dark Amber"
        else                                -> "Deep Amber"
    }
}