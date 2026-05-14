package com.example.urisis_android.urinalysis

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.urisis_android.auth.AppDatabase
import com.example.urisis_android.bluetooth.BleViewModel
import com.example.urisis_android.bluetooth.RgbColor
import com.example.urisis_android.bluetooth.SensorReading
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject

class UrinalysisViewModel(
    private val bleViewModel: BleViewModel,
    private val historyRepository: HistoryRepository? = null,
) : ViewModel() {

    private val _ui = MutableStateFlow(UrinalysisUiState())
    val ui: StateFlow<UrinalysisUiState> = _ui.asStateFlow()

    private var sessionJob: Job? = null
    private var stepAnimationJob: Job? = null
    private var activeUserEmail: String? = null

    /** Set by MainActivity whenever the logged-in user changes. */
    fun setActiveUser(email: String?) {
        activeUserEmail = email?.lowercase()?.trim()
    }

    /**
     * Stage a stored historical record into the UI as if a fresh test
     * had just completed. After this call, [ui] reports stage=COMPLETE
     * with the historical result, and TestResultsScreen / Advanced...
     * render the stored test using the same composables.
     *
     * Callers should navigate to the results screen immediately after.
     *
     * Important: this short-circuits any in-flight session, so calling
     * it during a live test will discard the live test. The caller is
     * expected to ensure no test is in progress (typically by only
     * triggering this from the History screen).
     */
    fun loadFromHistory(record: TestRecord) {
        sessionJob?.cancel()
        stepAnimationJob?.cancel()
        _ui.value = UrinalysisUiState(
            stage = UrinalysisStage.COMPLETE,
            currentStep = null,
            completedSteps = AnalysisStep.entries.toSet(),
            progress = 1f,
            partialReading = SensorReading(
                pH = record.pH,
                tdsPpm = record.tdsPpm,
                tempC = record.tempC,
                sensorRgb = record.sensorRgb,
                cameraRgb = record.cameraRgb,
            ),
            result = record.toUrinalysisResult(),
            errorMessage = null,
        )
    }

    /**
     * Start a urinalysis session — listens for the Arduino's JSON payload
     * over BLE and routes it through the Fuzzy KNN classifier when the
     * device sends a result.
     */
    fun beginSession() {
        reset()
        _ui.update { it.copy(stage = UrinalysisStage.WAITING) }
        sessionJob = viewModelScope.launch {
            listenToArduino()
        }
    }

    fun reset() {
        sessionJob?.cancel()
        stepAnimationJob?.cancel()
        sessionJob = null
        stepAnimationJob = null
        _ui.value = UrinalysisUiState()
    }

    // ── Real Arduino path ────────────────────────────────────────────────
    private suspend fun listenToArduino() {
        // Subscribe to JSON documents reassembled by BleManager. The flow
        // is shared, so collecting from inside this coroutine doesn't
        // affect any other consumers.
        bleViewModel.incoming.collect { json ->
            handleIncomingJson(json)
        }
    }

    private fun handleIncomingJson(json: String) {
        val parsed = runCatching { JSONObject(json) }.getOrNull() ?: return
        val type = parsed.optString("type", "")

        when (type) {
            "test_started" -> onTestStarted()
            "urinalysis"   -> onTestResult(parsed)
            else           -> { /* ignore unknown frames (acks, settings, etc.) */ }
        }
    }

    /** Arduino has begun reading sensors. Walk the UI through analysis steps. */
    private fun onTestStarted() {
        _ui.update { it.copy(stage = UrinalysisStage.ANALYZING, errorMessage = null) }
        stepAnimationJob?.cancel()
        stepAnimationJob = viewModelScope.launch {
            // The Arduino reads all sensors essentially in parallel and then
            // sends the full payload, so we don't get true per-step progress.
            // Walk through the steps over ~5 seconds for UX purposes; the
            // KNN step finishes when the result arrives.
            val steps = AnalysisStep.entries.toList()
            // Advance through everything except the last step (KNN) — KNN
            // completes when we receive the urinalysis JSON.
            steps.dropLast(1).forEachIndexed { i, step ->
                _ui.update { state ->
                    state.copy(
                        currentStep = step,
                        completedSteps = steps.take(i).toSet(),
                        progress = i.toFloat() / steps.size
                    )
                }
                delay(900)
            }
            // Park on the KNN step until result arrives
            _ui.update { state ->
                state.copy(
                    currentStep = AnalysisStep.KNN,
                    completedSteps = steps.dropLast(1).toSet(),
                    progress = (steps.size - 1).toFloat() / steps.size
                )
            }
        }
    }

    /** Arduino has finished — JSON contains the full sensor payload. */
    private fun onTestResult(root: JSONObject) {
        val reading = parseUrinalysisJson(root)
        if (reading == null) {
            _ui.update {
                it.copy(
                    stage = UrinalysisStage.ERROR,
                    errorMessage = "Malformed sensor payload"
                )
            }
            return
        }

        _ui.update { it.copy(partialReading = reading) }

        val result = HydrationClassifier.classify(reading)
        if (result == null) {
            _ui.update {
                it.copy(
                    stage = UrinalysisStage.ERROR,
                    errorMessage = "Incomplete sensor data"
                )
            }
        } else {
            stepAnimationJob?.cancel()
            _ui.update {
                it.copy(
                    stage = UrinalysisStage.COMPLETE,
                    result = result,
                    progress = 1f,
                    completedSteps = AnalysisStep.entries.toSet(),
                    currentStep = null
                )
            }
            persistResult(result)
        }
    }

    /**
     * Parse the Arduino's urinalysis JSON. Schema (from
     * ArduinoUrinalysis.ino::startTest):
     *
     *   {
     *     "device":  "URINE-TEST-001",
     *     "version": "1.0",
     *     "type":    "urinalysis",
     *     "sensors": {
     *       "temp_c":   25.3,
     *       "pH":       6.42,
     *       "tds_ppm":  450,
     *       "ec_us_cm": 900,
     *       "color":    { "r":..,"g":..,"b":..,"hex":"#..","lux":..,"cct":.. },
     *       "camera":   { "r":..,"g":..,"b":..,"hex":"#.." }    // optional
     *     }
     *   }
     */
    private fun parseUrinalysisJson(root: JSONObject): SensorReading? {
        val sensors = root.optJSONObject("sensors") ?: return null

        val pH    = sensors.optDouble("pH", Double.NaN).toFloat().takeUnless { it.isNaN() }
        val tds   = sensors.optDouble("tds_ppm", Double.NaN).toFloat().takeUnless { it.isNaN() }
        val temp  = sensors.optDouble("temp_c", Double.NaN).toFloat().takeUnless { it.isNaN() }

        // Capture both colour channels independently. The classifier
        // prefers camera (direct sample view), but the advanced screen
        // shows them separately for transparency.
        val sensorRgb = sensors.optJSONObject("color")?.let { it.toRgb() }
        val cameraRgb = sensors.optJSONObject("camera")?.let { it.toRgb() }

        return SensorReading(
            pH         = pH,
            tdsPpm     = tds,
            tempC      = temp,
            sensorRgb  = sensorRgb,
            cameraRgb  = cameraRgb,
        )
    }

    private fun JSONObject.toRgb(): RgbColor = RgbColor(
        r = optInt("r", 0),
        g = optInt("g", 0),
        b = optInt("b", 0),
        hex = optString("hex").takeIf { it.isNotEmpty() },
        lux = if (has("lux")) optDouble("lux").toFloat() else null,
        cct = if (has("cct")) optInt("cct") else null,
    )

    private fun persistResult(result: UrinalysisResult) {
        val email = activeUserEmail ?: return
        val repo = historyRepository ?: return
        viewModelScope.launch {
            runCatching { repo.save(email, result) }
        }
    }
}

// Factory that wires both the BLE source and the history repository
@Suppress("UNCHECKED_CAST")
class UrinalysisViewModelFactory(
    private val application: Application,
    private val bleViewModel: BleViewModel,
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val repo = HistoryRepository(AppDatabase.get(application).testRecordDao())
        return UrinalysisViewModel(bleViewModel, repo) as T
    }
}