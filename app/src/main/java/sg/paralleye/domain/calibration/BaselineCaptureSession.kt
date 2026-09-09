package sg.paralleye.domain.calibration

import sg.paralleye.config.CalibrationConfig
import sg.paralleye.domain.measurement.MeasurementQuality
import sg.paralleye.domain.measurement.MeasurementSample
import sg.paralleye.domain.measurement.ScreenOrientation
import java.util.UUID

enum class BaselineCaptureFailureReason { INSUFFICIENT_VALID_SAMPLES, SENSOR_UNAVAILABLE }

sealed interface BaselineCaptureOutcome {
    data class InProgress(val elapsedMillis: Long, val validSampleCount: Int, val requiredSamples: Int) : BaselineCaptureOutcome
    data class Success(val profile: BaselineProfile) : BaselineCaptureOutcome
    data class Failure(val reason: BaselineCaptureFailureReason) : BaselineCaptureOutcome
}

/**
 * Ch.4 §9-§12 "Guided Baseline-Capture Instructions" / "Calibration Duration" / "Baseline
 * Calculation". Only [MeasurementQuality.VALID] samples count (Ch.4 §11: no orientation
 * transition, no extreme movement, sufficient valid samples) — the Sensor Framework's own
 * quality classification already excludes transient movement and orientation transitions, so
 * this session doesn't re-implement that filtering, only accumulates what's already valid.
 *
 * Time source is injected wall-clock milliseconds (a one-time, low-frequency capture — not
 * the high-frequency interval case Ch.3 §5.3 warns against using wall-clock for).
 */
class BaselineCaptureSession(
    private val config: CalibrationConfig,
    private val methodologyVersion: String,
    private val configurationVersion: String,
) {
    private val validAngles = mutableListOf<Double>()
    private var captureOrientation: ScreenOrientation? = null
    private var startMillis: Long? = null

    fun start(nowMillis: Long) {
        startMillis = nowMillis
        validAngles.clear()
        captureOrientation = null
    }

    fun observe(sample: MeasurementSample, nowMillis: Long): BaselineCaptureOutcome {
        val start = startMillis ?: nowMillis.also { start(it) }

        if (sample.quality == MeasurementQuality.VALID && sample.deviceAngleDegrees != null) {
            validAngles += sample.deviceAngleDegrees
            if (captureOrientation == null) captureOrientation = sample.screenOrientation
        }

        val elapsed = nowMillis - start
        return if (elapsed >= config.captureDurationMillis) {
            finish(elapsed)
        } else {
            BaselineCaptureOutcome.InProgress(elapsed, validAngles.size, config.minValidSamples)
        }
    }

    private fun finish(elapsedMillis: Long): BaselineCaptureOutcome {
        if (validAngles.size < config.minValidSamples) {
            return BaselineCaptureOutcome.Failure(BaselineCaptureFailureReason.INSUFFICIENT_VALID_SAMPLES)
        }
        val baselineAngle = BaselineCalculator.calculate(validAngles, config.baselineStatistic)
            ?: return BaselineCaptureOutcome.Failure(BaselineCaptureFailureReason.INSUFFICIENT_VALID_SAMPLES)

        val profile = BaselineProfile(
            baselineId = UUID.randomUUID().toString(),
            createdAtEpochMillis = System.currentTimeMillis(),
            profileVersion = "1",
            guidedBaselineDeviceAngleDegrees = baselineAngle,
            captureOrientation = captureOrientation ?: ScreenOrientation.PORTRAIT,
            captureDurationMillis = elapsedMillis,
            validSampleCount = validAngles.size,
            quality = BaselineQuality.GUIDED_CAPTURE_ONLY,
            methodologyVersion = methodologyVersion,
            configurationVersion = configurationVersion,
            isOriginalBaseline = true,
        )
        return BaselineCaptureOutcome.Success(profile)
    }
}
