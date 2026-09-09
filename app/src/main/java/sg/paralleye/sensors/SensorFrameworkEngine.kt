package sg.paralleye.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import sg.paralleye.config.SensorFrameworkConfig
import sg.paralleye.domain.measurement.ComplementaryFilter
import sg.paralleye.domain.measurement.DeviceAngleCalculator
import sg.paralleye.domain.measurement.MeasurementQuality
import sg.paralleye.domain.measurement.MeasurementSample
import sg.paralleye.domain.measurement.NeckFlexionEstimator
import sg.paralleye.domain.measurement.ScreenOrientation
import sg.paralleye.domain.measurement.SensorMode
import sg.paralleye.domain.measurement.StablePostureQualifier
import sg.paralleye.domain.measurement.TransientMotionDetector
import sg.paralleye.domain.measurement.Vector3
import sg.paralleye.logging.ParallayeLogger

/**
 * Owns Android's [SensorManager] and orchestrates the pure calculation objects in
 * `sg.paralleye.domain.measurement` into one authoritative [MeasurementSample] stream
 * (Ch.3 §31 "Threading and Concurrency": one authoritative stream, no competing listeners).
 *
 * The Android-framework glue lives here deliberately separated from the pure math (angle
 * calculation, filtering, transient/stability detection) so those remain unit-testable
 * without the Android framework, per Ch.3 §35.1.
 */
class SensorFrameworkEngine(
    context: Context,
    private val config: SensorFrameworkConfig,
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    private val complementaryFilter = ComplementaryFilter(config.complementaryFilterAlpha)
    private val stabilityQualifier = StablePostureQualifier(
        windowSize = config.stablePostureWindowSize,
        maxVariationDegrees = config.stablePostureMaxVariationDegrees,
    )

    private var screenOrientation: ScreenOrientation = ScreenOrientation.PORTRAIT
    private var monitoringStartUptimeMillis: Long = 0L
    private var lastAccelTimestampNanos: Long? = null
    private var lastGyroVector: Vector3? = null
    private var lastGyroTimestampNanos: Long? = null
    private var inOrientationTransitionUntilMillis: Long = 0L

    private val _samples = MutableStateFlow<MeasurementSample?>(null)
    val samples: StateFlow<MeasurementSample?> = _samples.asStateFlow()

    val sensorMode: SensorMode = when {
        accelerometer == null -> SensorMode.UNAVAILABLE
        gyroscope == null -> SensorMode.ACCELEROMETER_ONLY_FALLBACK
        else -> SensorMode.FULL_FUSION
    }

    /** Ch.3 §4: monitoring must not begin without the minimum required sensor capability. */
    fun isMinimumCapabilityAvailable(): Boolean = accelerometer != null

    fun start() {
        if (!isMinimumCapabilityAvailable()) {
            ParallayeLogger.error("SensorFrameworkEngine", "Accelerometer unavailable; cannot start monitoring")
            _samples.value = unavailableSample()
            return
        }
        monitoringStartUptimeMillis = android.os.SystemClock.elapsedRealtime()
        complementaryFilter.reset()
        stabilityQualifier.reset()
        lastAccelTimestampNanos = null
        lastGyroVector = null

        val samplingPeriodUs = (1_000_000 / config.requestedSamplingRateHz)
        sensorManager.registerListener(this, accelerometer, samplingPeriodUs)
        if (sensorMode == SensorMode.FULL_FUSION) {
            sensorManager.registerListener(this, gyroscope, samplingPeriodUs)
        } else {
            ParallayeLogger.error("SensorFrameworkEngine", "Gyroscope unavailable; using accelerometer-only fallback")
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    /** Called by the hosting Activity/Service when display rotation changes (Ch.3 §18). */
    fun onScreenOrientationChanged(newOrientation: ScreenOrientation) {
        if (newOrientation == screenOrientation) return
        screenOrientation = newOrientation
        complementaryFilter.reset()
        stabilityQualifier.reset()
        inOrientationTransitionUntilMillis = android.os.SystemClock.elapsedRealtime() + config.orientationTransitionStabilisationMillis
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_GYROSCOPE -> {
                lastGyroVector = Vector3(event.values[0], event.values[1], event.values[2])
                lastGyroTimestampNanos = event.timestamp
            }
            Sensor.TYPE_ACCELEROMETER -> processAccelerometerEvent(event)
        }
    }

    private fun processAccelerometerEvent(event: SensorEvent) {
        val raw = Vector3(event.values[0], event.values[1], event.values[2])
        val timestampNanos = event.timestamp

        // Ch.3 §5.3: reject invalid/out-of-order/duplicate intervals rather than assuming uniform duration.
        val previousTimestamp = lastAccelTimestampNanos
        val gapMillis = previousTimestamp?.let { (timestampNanos - it) / 1_000_000.0 }
        val hasValidGap = previousTimestamp == null ||
            (gapMillis != null && gapMillis > 0 && gapMillis <= config.maxAcceptedSampleGapMillis)
        lastAccelTimestampNanos = timestampNanos

        val accelAngle = DeviceAngleCalculator.calculateAngleDegrees(raw, screenOrientation)
        val gravityValid = DeviceAngleCalculator.isGravityMagnitudeValid(raw, config.gravityToleranceMs2)

        if (accelAngle == null || !hasValidGap) {
            emit(raw, null, timestampNanos, MeasurementQuality.INVALID)
            return
        }

        val gyroChangeDegrees = computeGyroChangeDegrees(gapMillis)
        val fusedAngle = if (sensorMode == SensorMode.FULL_FUSION) {
            complementaryFilter.update(accelAngle, gyroChangeDegrees)
        } else {
            accelAngle
        }

        val angularVelocityMagnitude = lastGyroVector?.let { Math.toDegrees(it.magnitude) }

        val nowMillis = android.os.SystemClock.elapsedRealtime()
        val quality = when {
            !gravityValid -> MeasurementQuality.LOW_CONFIDENCE
            nowMillis < inOrientationTransitionUntilMillis -> MeasurementQuality.ORIENTATION_TRANSITION
            nowMillis - monitoringStartUptimeMillis < config.startupStabilisationMillis -> MeasurementQuality.STABILISING
            TransientMotionDetector.isTransient(
                angularVelocityMagnitude = angularVelocityMagnitude,
                accelerationMagnitude = raw.magnitude,
                maxAngularVelocityDegPerSec = config.maxValidAngularVelocityDegPerSec,
                gravityToleranceMs2 = config.gravityToleranceMs2,
            ) -> MeasurementQuality.TRANSIENT_MOVEMENT
            !stabilityQualifier.observe(fusedAngle) -> MeasurementQuality.STABILISING
            else -> MeasurementQuality.VALID
        }

        emit(raw, fusedAngle, timestampNanos, quality, angularVelocityMagnitude)
    }

    private fun computeGyroChangeDegrees(gapMillis: Double?): Double? {
        val gyro = lastGyroVector ?: return null
        if (gapMillis == null || gapMillis <= 0) return null
        // Engineering approximation for the complementary filter's 1D "gyroscope change" term:
        // integrate angular velocity around the remapped X axis (the axis a forward/backward
        // pitch rotates about), which corresponds to the change in our Y-derived tilt angle.
        val remapped = DeviceAngleCalculator.remapForOrientation(gyro, screenOrientation)
        val dtSeconds = gapMillis / 1000.0
        return Math.toDegrees(remapped.x.toDouble()) * dtSeconds
    }

    private fun emit(
        raw: Vector3,
        angleDegrees: Double?,
        timestampNanos: Long,
        quality: MeasurementQuality,
        angularVelocityMagnitude: Double? = null,
    ) {
        val sample = MeasurementSample(
            timestampNanos = timestampNanos,
            rawAccelerometer = raw,
            rawGyroscope = lastGyroVector,
            screenOrientation = screenOrientation,
            deviceAngleDegrees = angleDegrees,
            estimatedNeckFlexionDegrees = angleDegrees?.let { NeckFlexionEstimator.estimate(it, config = null) },
            angularVelocityMagnitude = angularVelocityMagnitude,
            quality = quality,
            sensorMode = sensorMode,
        )
        _samples.value = sample
        ParallayeLogger.cycle(
            sg.paralleye.logging.CycleRecord(
                timestampMillis = timestampNanos / 1_000_000,
                deviceAngleDegrees = sample.deviceAngleDegrees,
            ),
        )
    }

    private fun unavailableSample() = MeasurementSample(
        timestampNanos = 0L,
        rawAccelerometer = null,
        rawGyroscope = null,
        screenOrientation = screenOrientation,
        deviceAngleDegrees = null,
        estimatedNeckFlexionDegrees = null,
        angularVelocityMagnitude = null,
        quality = MeasurementQuality.SENSOR_UNAVAILABLE,
        sensorMode = SensorMode.UNAVAILABLE,
    )

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Ch.3 doesn't define behaviour on accuracy-change callbacks beyond quality classification,
        // which is already derived from gravity-magnitude and stability checks above.
    }
}
