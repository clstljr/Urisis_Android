package com.example.urisis_android.urinalysis

import com.example.urisis_android.bluetooth.SensorReading
import java.util.Date

enum class UrinalysisStage {
    IDLE, GUIDE, WAITING, ANALYZING, COMPLETE, ERROR
}

enum class AnalysisStep(val label: String) {
    RGB("Reading RGB Sensor"),
    PH("Reading pH Sensor"),
    SG("Reading TDS / Specific Gravity"),
    IMAGE("Processing Image Data"),
    KNN("Running Enhanced Fuzzy KNN Analysis")
}

/**
 * Hydration tiers used by the existing screens. The 8-class Fuzzy KNN
 * output ([UrineClass]) maps onto this 5-tier enum via [fromUrineClass]
 * so the existing UI keeps rendering without rewrite.
 *
 * OVERHYDRATED is new — corresponds to LEVEL_1 from the table, which
 * indicates polydipsia / renal failure risk and is clinically distinct
 * from "well hydrated".
 */
enum class HydrationStatus(
    val label: String,
    val description: String,
    val recommendation: String
) {
    OVERHYDRATED(
        "OVERHYDRATED",
        "Your urine is unusually dilute.",
        "If this persists, consult a physician — could indicate excessive water intake or kidney function changes."
    ),
    WELL_HYDRATED(
        "WELL HYDRATED",
        "Your hydration level is optimal.",
        "Maintain your current water intake."
    ),
    NORMAL(
        "NORMAL HYDRATION",
        "Your hydration level is within normal range.",
        "Keep up with regular fluid intake."
    ),
    MILDLY_DEHYDRATED(
        "MILDLY DEHYDRATED",
        "Your hydration level is slightly low.",
        "Increase water intake and retest in 6 hours."
    ),
    DEHYDRATED(
        "DEHYDRATED",
        "Your hydration level is significantly low.",
        "Drink water immediately and consult a physician if symptoms persist."
    );

    companion object {
        /** Map an 8-class Fuzzy KNN level → 5-tier HydrationStatus for UI. */
        fun fromUrineClass(c: UrineClass): HydrationStatus = when (c) {
            UrineClass.LEVEL_1   -> OVERHYDRATED
            UrineClass.LEVEL_2   -> WELL_HYDRATED
            UrineClass.LEVEL_3_4 -> NORMAL
            UrineClass.LEVEL_5_6 -> MILDLY_DEHYDRATED
            UrineClass.LEVEL_7_8 -> DEHYDRATED
            // Flags shouldn't be passed in, but fall back gracefully.
            else                 -> NORMAL
        }
    }
}

/**
 * Result of one urinalysis test.
 *
 * Existing fields ([status], [confidence], [pHInRange], [sgInRange],
 * [colorLabel], [reading], [timestamp]) preserved for screen compatibility.
 *
 * New fields from the Fuzzy KNN output:
 *   - [urineClass]    : the dominant level (Level 1 / 2 / 3-4 / 5-6 / 7-8)
 *   - [activeFlags]   : any of FLAG_A / FLAG_B / FLAG_C above threshold
 *   - [memberships]   : full membership map for transparency
 *   - [tdsInRange]    : real TDS-based range check (separate from sgInRange)
 */
data class UrinalysisResult(
    val reading: SensorReading,
    val status: HydrationStatus,
    val confidence: Float,        // 0..1
    val pHInRange: Boolean,
    val sgInRange: Boolean,
    val tdsInRange: Boolean,
    val colorLabel: String,
    val urineClass: UrineClass,
    val activeFlags: List<UrineClass>,
    val memberships: Map<UrineClass, Float>,
    val timestamp: Date = Date()
) {
    companion object {
        const val PH_MIN = 4.5f
        const val PH_MAX = 8.0f
        const val SG_MIN = 1.005f
        const val SG_MAX = 1.030f

        // Healthy TDS for adult urine is roughly 150–900 ppm (corresponds
        // to SG 1.005–1.030 via the chosen TDS→SG mapping).
        const val TDS_MIN = 150f
        const val TDS_MAX = 900f
    }
}

data class UrinalysisUiState(
    val stage: UrinalysisStage = UrinalysisStage.IDLE,
    val currentStep: AnalysisStep? = null,
    val completedSteps: Set<AnalysisStep> = emptySet(),
    val progress: Float = 0f,
    val partialReading: SensorReading = SensorReading(),
    val result: UrinalysisResult? = null,
    val errorMessage: String? = null
)