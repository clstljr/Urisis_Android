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
     * Start a urinalysis session.
     *
     * @param demo If true, fakes data after step animation. Otherwise
     *             listens for real Arduino JSON over BLE.
     */
    fun beginSession(demo: Boolean = false) {
        reset()
        _ui.update { it.copy(stage = UrinalysisStage.WAITING) }
        sessionJob = viewModelScope.launch {
            if (demo) runDemo() else listenToArduino()
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

        // Prefer the camera RGB block (sees the actual sample) over the
        // TCS34725 RGB block (reflected light). Fall back if camera missing.
        val rgbObj = sensors.optJSONObject("camera") ?: sensors.optJSONObject("color")
        val rgb = rgbObj?.let {
            RgbColor(
                r = it.optInt("r", 0),
                g = it.optInt("g", 0),
                b = it.optInt("b", 0),
                hex = it.optString("hex").takeIf { s -> s.isNotEmpty() },
                lux = if (it.has("lux")) it.optDouble("lux").toFloat() else null,
                cct = if (it.has("cct")) it.optInt("cct") else null,
            )
        }

        return SensorReading(
            pH      = pH,
            tdsPpm  = tds,
            tempC   = temp,
            rgb     = rgb,
        )
    }

    // ── Demo path — used when no Arduino is connected ─────────────────────
    //
    // Generates synthetic sensor data, runs it through the *real* Fuzzy KNN
    // pipeline, and persists the result. The point isn't to mock the
    // classifier — it's to feed the classifier known-class inputs so the
    // entire UI flow (analyzing → result → history → trend chart) can be
    // exercised without the Arduino present.
    //
    // Demo runs rotate through the classification table on each invocation
    // so a sequence of demo tests fills the history with varied entries.
    private suspend fun runDemo() {
        _ui.update { it.copy(stage = UrinalysisStage.ANALYZING) }

        // Walk the analysis steps for visual continuity with the real flow.
        val steps = AnalysisStep.entries.toList()
        steps.forEachIndexed { i, step ->
            _ui.update { state ->
                state.copy(
                    currentStep = step,
                    completedSteps = steps.take(i).toSet(),
                    progress = i.toFloat() / steps.size
                )
            }
            delay(900)
        }

        val scenario = nextDemoScenario()
        val reading = scenario.toReading()
        _ui.update { it.copy(partialReading = reading) }

        val result = HydrationClassifier.classify(reading)
        if (result != null) {
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
        } else {
            _ui.update {
                it.copy(
                    stage = UrinalysisStage.ERROR,
                    errorMessage = "Demo classification failed"
                )
            }
        }
    }

    /**
     * Returns the next demo scenario, rotating deterministically through
     * the table so each demo run exercises a different class. Uses a
     * companion-object counter so rotation persists across VM resets
     * within the same process — meaning a tester pressing "Start Demo"
     * five times in a row sees five different result types instead of
     * the same one repeatedly.
     */
    private fun nextDemoScenario(): DemoScenario {
        val idx = (demoCounter++) % DEMO_SCENARIOS.size
        return DEMO_SCENARIOS[idx].withJitter()
    }

    private fun persistResult(result: UrinalysisResult) {
        val email = activeUserEmail ?: return
        val repo = historyRepository ?: return
        viewModelScope.launch {
            runCatching { repo.save(email, result) }
        }
    }

    companion object {
        // Process-wide rotation counter. Survives ViewModel reset()
        // (which only nukes UI state) so consecutive demo runs march
        // through the table even after the user navigates away.
        @Volatile private var demoCounter: Int = 0

        // One scenario per row of the Silver Swan classification table,
        // plus a few mixed cases so demos can show flags + level coexisting.
        // Each scenario is the *centre* of its class — `withJitter()`
        // adds small variation per run so trend charts get realistic
        // bumpiness instead of flat lines.
        private val DEMO_SCENARIOS = listOf(
            DemoScenario("Well Hydrated",     pH = 6.4f, tdsPpm = 230f,  r = 250, g = 240, b = 180),
            DemoScenario("Minimal Dehydr.",   pH = 6.5f, tdsPpm = 470f,  r = 222, g = 195, b = 95),
            DemoScenario("Significant Dehydr.", pH = 6.2f, tdsPpm = 750f, r = 188, g = 130, b = 45),
            DemoScenario("Overhydrated",      pH = 6.8f, tdsPpm = 80f,   r = 248, g = 248, b = 235),
            DemoScenario("Serious Dehydr.",   pH = 6.0f, tdsPpm = 1050f, r = 152, g = 85,  b = 18),
            DemoScenario("Flag B — Alkaline", pH = 8.8f, tdsPpm = 500f,  r = 220, g = 195, b = 95),
            DemoScenario("Flag A — Red",      pH = 6.5f, tdsPpm = 400f,  r = 178, g = 42,  b = 42),
            DemoScenario("Flag C — Acidic",   pH = 3.8f, tdsPpm = 300f,  r = 248, g = 235, b = 170),
            DemoScenario("Mixed: Alk + Amber", pH = 8.5f, tdsPpm = 750f, r = 185, g = 125, b = 40),
        )
    }
}

/**
 * One synthetic measurement scenario for the demo path.
 */
private data class DemoScenario(
    val label: String,
    val pH: Float,
    val tdsPpm: Float,
    val r: Int, val g: Int, val b: Int,
) {
    fun toReading(): SensorReading = SensorReading(
        pH = pH,
        tdsPpm = tdsPpm,
        tempC = 25.0f,
        rgb = RgbColor(r, g, b, hex = "#%02X%02X%02X".format(r, g, b)),
    )

    /**
     * Returns a copy with small random noise applied to each channel —
     * stays inside the same class but produces visibly distinct values
     * across consecutive runs of the same scenario.
     */
    fun withJitter(): DemoScenario {
        fun rand(): Float = (Math.random().toFloat() - 0.5f) * 2f  // [-1, 1]
        return copy(
            pH = (pH + rand() * 0.15f).coerceIn(0f, 14f),
            tdsPpm = (tdsPpm + rand() * 30f).coerceAtLeast(0f),
            r = (r + (rand() * 8).toInt()).coerceIn(0, 255),
            g = (g + (rand() * 8).toInt()).coerceIn(0, 255),
            b = (b + (rand() * 8).toInt()).coerceIn(0, 255),
        )
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