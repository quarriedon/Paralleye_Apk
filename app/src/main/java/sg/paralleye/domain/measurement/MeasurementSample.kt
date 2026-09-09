package sg.paralleye.domain.measurement

/** Raw accelerometer or gyroscope vector in the device's physical coordinate frame. */
data class Vector3(val x: Float, val y: Float, val z: Float) {
    val magnitude: Double get() = kotlin.math.sqrt((x * x + y * y + z * z).toDouble())
}

/**
 * Ch.3 §32 "Measurement Data Model" — the single authoritative processed measurement object
 * the Measurement Domain hands to everything downstream. Per §33 "Measurement Output Contract"
 * this must NOT carry activity multiplier, dynamic load, cumulative load, score or mascot
 * state — those belong to later layers and must never be attached here.
 */
data class MeasurementSample(
    val timestampNanos: Long,
    val rawAccelerometer: Vector3?,
    val rawGyroscope: Vector3?,
    val screenOrientation: ScreenOrientation,
    /** Device tilt-from-vertical angle in degrees, per the convention in DeviceAngleCalculator's KDoc. Null if not computable this cycle. */
    val deviceAngleDegrees: Double?,
    /** Ch.3 §20: modular, provisional, disabled by default — see NeckFlexionEstimator. */
    val estimatedNeckFlexionDegrees: Double?,
    val angularVelocityMagnitude: Double?,
    val quality: MeasurementQuality,
    val sensorMode: SensorMode,
)

/** Ch.3 §4: which sensors are actually backing the current measurement. */
enum class SensorMode {
    FULL_FUSION,
    ACCELEROMETER_ONLY_FALLBACK,
    UNAVAILABLE,
}
