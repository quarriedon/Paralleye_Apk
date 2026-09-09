package sg.paralleye.ui.onboarding

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import sg.paralleye.config.ParallayeParameters
import sg.paralleye.domain.calibration.BaselineCaptureOutcome
import sg.paralleye.domain.calibration.BaselineCaptureSession
import sg.paralleye.domain.measurement.MeasurementQuality
import sg.paralleye.sensors.SensorFrameworkEngine

/**
 * Ch.4 §9-11 "Guided Baseline-Capture": owns the live sensor engine and capture session for
 * the duration of the calibration screen only — a UI-lifecycle-scoped controller, not part of
 * the domain layer, since it directly drives Android sensor hardware.
 */
class CalibrationCaptureController(context: Context, params: ParallayeParameters) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var collectJob: Job? = null

    private val sensorEngine = SensorFrameworkEngine(context, params.sensorFramework)
    private val captureSession = BaselineCaptureSession(
        config = params.calibration,
        methodologyVersion = "1.0",
        configurationVersion = params.version.configVersion,
    )

    private val _outcome = MutableStateFlow<BaselineCaptureOutcome>(
        BaselineCaptureOutcome.InProgress(0, 0, params.calibration.minValidSamples),
    )
    val outcome: StateFlow<BaselineCaptureOutcome> = _outcome.asStateFlow()

    fun start() {
        if (!sensorEngine.isMinimumCapabilityAvailable()) {
            _outcome.value = BaselineCaptureOutcome.Failure(
                sg.paralleye.domain.calibration.BaselineCaptureFailureReason.SENSOR_UNAVAILABLE,
            )
            return
        }
        captureSession.start(System.currentTimeMillis())
        sensorEngine.start()
        collectJob = scope.launch {
            sensorEngine.samples.collect { sample ->
                if (sample == null || _outcome.value !is BaselineCaptureOutcome.InProgress) return@collect
                val result = captureSession.observe(sample, System.currentTimeMillis())
                _outcome.value = result
                if (result !is BaselineCaptureOutcome.InProgress) {
                    stop()
                }
            }
        }
    }

    /** Exposed for diagnostics/testing; [MeasurementQuality.VALID] is what actually counts toward the baseline. */
    fun stop() {
        sensorEngine.stop()
        collectJob?.cancel()
    }
}
