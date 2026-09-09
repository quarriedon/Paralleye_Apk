package sg.paralleye.domain.measurement

import kotlin.math.abs

/**
 * Ch.3 §11.4-11.5: sensor fusion is required, but neither the fusion method nor its
 * coefficients are specified — the chapter explicitly allows "a simpler stable method" for
 * the MVP and requires that whichever is chosen "must be identified as an engineering choice
 * rather than a filed methodology constant." A complementary filter is used here rather than
 * a Kalman filter, matching §11.5's guidance to prefer the simpler method when it meets the
 * requirements; [alpha] is a configured, tested value, not a filed constant.
 *
 * Fused angle = alpha × (previous fused angle + gyro change) + (1 − alpha) × accelerometer angle
 */
class ComplementaryFilter(private val alpha: Double) {
    init {
        require(alpha in 0.0..1.0) { "alpha must be in [0,1], was $alpha" }
    }

    private var fusedAngleDegrees: Double? = null

    /**
     * @param accelerometerAngleDegrees this cycle's accelerometer-only angle (from [DeviceAngleCalculator]).
     * @param gyroChangeDegrees the integrated gyroscope rotation since the last update, around the same axis as the angle (degrees), or null if unavailable.
     */
    fun update(accelerometerAngleDegrees: Double, gyroChangeDegrees: Double?): Double {
        val previous = fusedAngleDegrees
        val result = if (previous == null || gyroChangeDegrees == null) {
            accelerometerAngleDegrees
        } else {
            alpha * (previous + gyroChangeDegrees) + (1 - alpha) * accelerometerAngleDegrees
        }
        fusedAngleDegrees = result
        return result
    }

    fun reset() {
        fusedAngleDegrees = null
    }

    companion object {
        /** Ch.3 §11.4: "The exact coefficient must remain configurable and be selected through testing." Not a filed value. */
        const val DEFAULT_ALPHA = 0.90
    }
}

/**
 * Ch.3 §14 "Transient-Movement Rejection". A simple magnitude-based heuristic: a sample is
 * transient if angular velocity or acceleration deviates sharply from a stable gravity read.
 * All thresholds are engineering parameters (not specified in the source documents) and are
 * exposed via [SensorFrameworkConfig] so they're centrally configurable and testable.
 */
object TransientMotionDetector {
    fun isTransient(
        angularVelocityMagnitude: Double?,
        accelerationMagnitude: Double,
        maxAngularVelocityDegPerSec: Double,
        gravityToleranceMs2: Double,
    ): Boolean {
        val gyroExceeded = angularVelocityMagnitude != null && angularVelocityMagnitude > maxAngularVelocityDegPerSec
        val gravityDeviation = abs(accelerationMagnitude - EARTH_GRAVITY_MS2)
        val accelExceeded = gravityDeviation > gravityToleranceMs2
        return gyroExceeded || accelExceeded
    }

    private const val EARTH_GRAVITY_MS2 = 9.80665
}

/**
 * Ch.3 §15 "Stable-Posture Qualification". Tracks a short rolling window of recent angles and
 * reports whether they're stable enough (bounded variation) to be treated as behaviourally
 * valid, preventing boundary oscillation from single noisy samples.
 */
class StablePostureQualifier(
    private val windowSize: Int,
    private val maxVariationDegrees: Double,
) {
    private val recentAngles = ArrayDeque<Double>()

    fun observe(angleDegrees: Double): Boolean {
        recentAngles.addLast(angleDegrees)
        while (recentAngles.size > windowSize) recentAngles.removeFirst()
        if (recentAngles.size < windowSize) return false
        val min = recentAngles.min()
        val max = recentAngles.max()
        return (max - min) <= maxVariationDegrees
    }

    fun reset() {
        recentAngles.clear()
    }
}
