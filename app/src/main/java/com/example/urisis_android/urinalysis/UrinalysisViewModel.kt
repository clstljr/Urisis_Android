package com.example.urisis_android.urinalysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.urisis_android.bluetooth.BleViewModel
import com.example.urisis_android.bluetooth.HsvColor
import com.example.urisis_android.bluetooth.SensorReading
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UrinalysisViewModel(
    private val bleViewModel: BleViewModel
) : ViewModel() {

    private val _ui = MutableStateFlow(UrinalysisUiState())
    val ui: StateFlow<UrinalysisUiState> = _ui.asStateFlow()

    private var job: Job? = null

    // ── Called from Protocol screen's Continue button ─────────────────────
    fun beginSession(demo: Boolean = true) {
        reset()
        _ui.update { it.copy(stage = UrinalysisStage.WAITING) }
        job = viewModelScope.launch {
            if (demo) runDemo() else listenToArduino()
        }
    }

    fun reset() {
        job?.cancel()
        _ui.value = UrinalysisUiState()
    }

    // ── Real Arduino path ────────────────────────────────────────────────
    // Expected messages from Arduino (adapt to your firmware's protocol):
    //   READY                          → button pressed, analysis starts
    //   STAGE:RGB|PH|SG|IMAGE|KNN      → progress updates
    //   DATA:PH=7.4;SG=1.015;HSV=45,30,85
    //   DONE                           → final
    private suspend fun listenToArduino() {
        bleViewModel.bleState.value  // ensure connection exists
        bleViewModel.let { /* BleManager.incoming flow */ }
        // Collect BLE notifications
        val manager = bleViewModel
        // NOTE: expose manager.incoming in BleViewModel if not already
        // e.g. val incoming: SharedFlow<String> = managerField.incoming
        // Then: manager.incoming.collect { handleMessage(it) }
        // For now this stub hands off to the demo runner so the UI flows.
        runDemo()
    }

    private fun handleMessage(msg: String) {
        when {
            msg == "READY" -> {
                _ui.update { it.copy(stage = UrinalysisStage.ANALYZING) }
            }
            msg.startsWith("STAGE:") -> {
                val step = runCatching {
                    AnalysisStep.valueOf(msg.removePrefix("STAGE:"))
                }.getOrNull() ?: return
                _ui.update { state ->
                    val completed = state.currentStep
                        ?.let { state.completedSteps + it } ?: state.completedSteps
                    state.copy(
                        currentStep = step,
                        completedSteps = completed,
                        progress = completed.size.toFloat() / AnalysisStep.values().size
                    )
                }
            }
            msg.startsWith("DATA:") -> {
                parseData(msg.removePrefix("DATA:"))?.let { reading ->
                    _ui.update { it.copy(partialReading = reading) }
                }
            }
            msg == "DONE" -> finalise()
        }
    }

    private fun parseData(payload: String): SensorReading? {
        val map = payload.split(";")
            .mapNotNull {
                val parts = it.split("=", limit = 2)
                if (parts.size == 2) parts[0] to parts[1] else null
            }.toMap()
        val ph = map["PH"]?.toFloatOrNull()
        val sg = map["SG"]?.toFloatOrNull()
        val hsv = map["HSV"]?.split(",")?.mapNotNull { it.toFloatOrNull() }
        val color = hsv?.takeIf { it.size == 3 }
            ?.let { HsvColor(it[0], it[1], it[2]) }
        return SensorReading(pH = ph, specificGravity = sg, color = color)
    }

    private fun finalise() {
        val reading = _ui.value.partialReading
        val result = HydrationClassifier.classify(reading)
        if (result == null) {
            _ui.update {
                it.copy(
                    stage = UrinalysisStage.ERROR,
                    errorMessage = "Incomplete sensor data"
                )
            }
        } else {
            _ui.update {
                it.copy(
                    stage = UrinalysisStage.COMPLETE,
                    result = result,
                    progress = 1f,
                    completedSteps = AnalysisStep.values().toSet(),
                    currentStep = null
                )
            }
        }
    }

    // ── Demo path — used when no Arduino is connected ─────────────────────
    private suspend fun runDemo() {
        // Simulate waiting for button press
        delay(2_500)
        _ui.update { it.copy(stage = UrinalysisStage.ANALYZING) }

        val steps = AnalysisStep.values().toList()
        steps.forEachIndexed { i, step ->
            _ui.update { state ->
                state.copy(
                    currentStep = step,
                    completedSteps = steps.take(i).toSet(),
                    progress = i.toFloat() / steps.size
                )
            }
            delay(1_400)
        }

        // Randomize so the result isn't always identical — driven by actual values
        val pH = (5.5f + Math.random().toFloat() * 2.5f)   // 5.5..8.0
        val sg = (1.005f + Math.random().toFloat() * 0.025f) // 1.005..1.030
        val hue = 45f + Math.random().toFloat() * 20f       // 45..65°
        val sat = 20f + Math.random().toFloat() * 70f       // 20..90%
        val value = 55f + Math.random().toFloat() * 40f     // 55..95%

        _ui.update {
            it.copy(
                partialReading = SensorReading(
                    pH = pH,
                    specificGravity = sg,
                    color = HsvColor(hue, sat, value)
                )
            )
        }
        finalise()
    }
}

// Simple factory so screens can obtain a VM that's wired to BleViewModel
@Suppress("UNCHECKED_CAST")
class UrinalysisViewModelFactory(
    private val bleViewModel: BleViewModel
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        UrinalysisViewModel(bleViewModel) as T
}