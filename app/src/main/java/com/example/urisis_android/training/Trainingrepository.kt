package com.example.urisis_android.training

import android.content.Context
import android.os.Environment
import com.example.urisis_android.urinalysis.RawFeatures
import com.example.urisis_android.urinalysis.TrainedModel
import com.example.urisis_android.urinalysis.TrainedModelExporter
import com.example.urisis_android.urinalysis.UrineClass
import java.io.File

/**
 * Coordinates everything Training Mode does: ingesting labeled measurements,
 * tracking progress, and running the export pipeline.
 *
 * Pure orchestration — no UI, no BLE, no preferences. The Activity/ViewModel
 * layer wires this to actual user input.
 */
class TrainingRepository(
    private val context: Context,
    private val dao: MeasurementDao,
) {

    /**
     * Persist a single labeled trial. Called after each successful BLE
     * sensor capture in Training Mode.
     */
    suspend fun recordTrial(
        formulationId: String,
        label: UrineClass,
        raw: RawFeatures,
        ambientTempC: Float? = null,
    ): Long {
        val nextIndex = dao.loadProgress()
            .firstOrNull { it.formulationId == formulationId }
            ?.trialCount?.plus(1) ?: 1

        return dao.insert(
            MeasurementEntity(
                formulationId = formulationId,
                label = label,
                trialIndex = nextIndex,
                pH = raw.pH,
                sg = raw.sg,
                hue = raw.hue,
                saturation = raw.saturation,
                value = raw.value,
                capturedAt = System.currentTimeMillis(),
                ambientTempC = ambientTempC,
            )
        )
    }

    suspend fun loadProgress(): List<FormulationProgress> = dao.loadProgress()

    suspend fun totalMeasurements(): Int = dao.totalCount()

    /**
     * Validates the dataset, runs grid search + final training, and writes
     * trained_model.json to the device's Downloads folder. You then pull
     * that file via USB or share it to yourself, drop it into
     * `app/src/main/res/raw/`, and rebuild the APK for production release.
     *
     * Returns the path of the written file so the UI can show it.
     */
    suspend fun trainAndExport(
        targetTrialsPerFormulation: Int = 20,
    ): ExportResult {
        val allMeasurements = dao.loadAll()
        require(allMeasurements.isNotEmpty()) {
            "No measurements recorded. Collect data before training."
        }

        val progress = dao.loadProgress()
        val incomplete = progress.filter { it.trialCount < targetTrialsPerFormulation }
        require(incomplete.isEmpty()) {
            "Some formulations are incomplete: " +
                    incomplete.joinToString { "${it.formulationId} (${it.trialCount}/$targetTrialsPerFormulation)" } +
                    ". Collect remaining trials before training."
        }

        // Sanity check: at least 5 formulations per class for 5-fold CV.
        val formulationsPerClass = progress
            .groupBy { it.label }
            .mapValues { it.value.size }
        val sparseClasses = formulationsPerClass.filterValues { it < 5 }
        require(sparseClasses.isEmpty()) {
            "Stratified 5-fold CV requires ≥5 formulations per class. " +
                    "Sparse classes: $sparseClasses. " +
                    "Either collect more formulations or collapse the class scheme."
        }

        // Convert MeasurementEntity → LabMeasurement and train.
        val labMeasurements = allMeasurements.map {
            TrainedModelExporter.LabMeasurement(
                formulationId = it.formulationId,
                label = it.label,
                raw = RawFeatures(
                    pH = it.pH, sg = it.sg,
                    hue = it.hue,
                    saturation = it.saturation, value = it.value,
                ),
            )
        }

        val model = TrainedModelExporter.train(labMeasurements)

        // Write to the Downloads folder so it's reachable via ADB or USB.
        val outputDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "urino"
        ).apply { mkdirs() }

        val outputFile = File(outputDir, "trained_model.json")
        outputFile.writeText(model.toJson())

        return ExportResult(
            file = outputFile,
            chosenK = model.k,
            cvAccuracy = model.trainingMetadata.cvAccuracy,
            cvConfidence = model.trainingMetadata.cvConfidence,
            measurementCount = model.trainingMetadata.totalMeasurements,
        )
    }

    suspend fun clearAllMeasurements() {
        dao.deleteAll()
    }

    suspend fun clearFormulation(formulationId: String) {
        dao.deleteFormulation(formulationId)
    }

    data class ExportResult(
        val file: File,
        val chosenK: Int,
        val cvAccuracy: Float,
        val cvConfidence: Float,
        val measurementCount: Int,
    )
}