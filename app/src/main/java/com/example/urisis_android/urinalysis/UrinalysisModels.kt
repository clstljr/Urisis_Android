package com.example.urisis_android.urinalysis

import com.example.urisis_android.bluetooth.SensorReading
import java.util.Date

enum class UrinalysisStage {
    IDLE, GUIDE, WAITING, ANALYZING, COMPLETE, ERROR
}

enum class AnalysisStep(val label: String) {
    RGB("Reading RGB Sensor"),
    PH("Reading pH Sensor"),
    SG("Reading Specific Gravity"),
    IMAGE("Processing Image Data"),
    KNN("Running Enhanced Fuzzy KNN Analysis")
}

enum class HydrationStatus(
    val label: String,
    val description: String,
    val recommendation: String
) {
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
    )
}

data class UrinalysisResult(
    val reading: SensorReading,
    val status: HydrationStatus,
    val confidence: Float,        // 0..1
    val pHInRange: Boolean,
    val sgInRange: Boolean,
    val colorLabel: String,
    val timestamp: Date = Date()
) {
    companion object {
        const val PH_MIN = 4.5f
        const val PH_MAX = 8.0f
        const val SG_MIN = 1.005f
        const val SG_MAX = 1.030f
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