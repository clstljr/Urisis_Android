package com.example.urisis_android.urinalysis

import com.example.urisis_android.bluetooth.HsvColor
import com.example.urisis_android.bluetooth.SensorReading

/**
 * Hydration classifier.
 *
 * Public API unchanged from the previous rule-based implementation, but
 * the internals now use Enhanced Fuzzy KNN ([FuzzyKnnClassifier]) trained
 * on the Silver Swan classification table. The 8-class output is mapped
 * back to [HydrationStatus] for the existing UI, with abnormal flags
 * surfaced via [UrinalysisResult.activeFlags].
 */
object HydrationClassifier {

    private val classifier = FuzzyKnnClassifier(TrainingData.samples)

    fun classify(r: SensorReading): UrinalysisResult? {
        val features = buildFeatures(r) ?: return null
        val pH  = r.pH ?: return null
        val tds = r.tdsPpm ?: return null
        val sg  = r.specificGravity ?: return null
        val color = r.color ?: return null

        val knn = classifier.classify(features)
        val dominant = knn.dominantLevel
        val flags    = knn.activeFlags
        val status   = HydrationStatus.fromUrineClass(dominant)

        // Confidence reported to the UI is the dominant level's membership
        // clamped to a presentation-friendly band. We avoid 0% / 100%
        // because both feel pathological in clinical UIs.
        val confidence = (knn.dominantLevelMembership)
            .coerceIn(0.40f, 0.99f)

        return UrinalysisResult(
            reading     = r,
            status      = status,
            confidence  = confidence,
            pHInRange   = pH in UrinalysisResult.PH_MIN..UrinalysisResult.PH_MAX,
            sgInRange   = sg in UrinalysisResult.SG_MIN..UrinalysisResult.SG_MAX,
            tdsInRange  = tds in UrinalysisResult.TDS_MIN..UrinalysisResult.TDS_MAX,
            colorLabel  = colorLabel(color, flags),
            urineClass  = dominant,
            activeFlags = flags,
            memberships = knn.memberships,
        )
    }

    /**
     * Surface flag info inline if the colour itself triggered FLAG_A.
     * Callers that want pure colour name should consult the HSV directly.
     */
    private fun colorLabel(c: HsvColor, flags: List<UrineClass>): String {
        if (UrineClass.FLAG_A in flags) {
            return "Abnormal Colour"
        }
        return when {
            c.value > 90f && c.saturation < 15f -> "Clear"
            c.value > 85f && c.saturation < 30f -> "Pale Yellow"
            c.value > 75f && c.saturation < 50f -> "Yellow"
            c.value > 60f && c.saturation < 70f -> "Dark Yellow"
            c.value > 45f                       -> "Amber"
            else                                -> "Deep Amber / Honey"
        }
    }
}