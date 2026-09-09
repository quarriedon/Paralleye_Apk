package sg.paralleye.domain.measurement

import kotlin.math.acos

/**
 * Ch.3 §7-§10, §16-§18: pure, unit-testable device-angle calculation.
 *
 * ## Axis convention — an engineering decision, flagged for physical verification
 *
 * The source documents give θ = arccos(Z / |v|) and state that a vertical phone must read
 * ≈0° with the angle increasing toward horizontal. Taken literally with Android's standard
 * accelerometer frame (Z = out of the screen face), those two statements contradict each
 * other: held vertically with the screen facing the user, gravity's reaction is measured
 * almost entirely on the device's Y axis (up the screen), with Z near zero — so
 * arccos(Z/|v|) reads ≈90° when vertical and ≈0° flat on a table, backwards from the stated
 * intent. This is exactly the ambiguity Ch.3 §7-8 anticipates ("the formula must not be
 * accepted solely because it produces a numerical output — it must be validated against
 * physical test positions") and §37 lists explicitly as unresolved.
 *
 * This implementation therefore computes tilt-from-vertical from the accelerometer's Y
 * component after remapping for the current screen orientation (see [remapForOrientation]),
 * which does satisfy both stated anchors (vertical ≈ 0°, flat/horizontal ≈ 90°). It is
 * documented here, in docs/open-questions.md, and must be confirmed against the Ch.3 §8
 * physical test positions on a real device before being treated as final — this is a
 * candidate for correction, not a silent guess.
 */
object DeviceAngleCalculator {

    /**
     * Remaps a raw device-frame vector so that Y consistently means "up the currently
     * displayed screen", using the standard rotation-compensation mapping (equivalent to
     * `SensorManager.remapCoordinateSystem` for each `Surface.ROTATION_*` value, reimplemented
     * here as pure arithmetic so it stays unit-testable without the Android framework).
     */
    fun remapForOrientation(raw: Vector3, orientation: ScreenOrientation): Vector3 = when (orientation) {
        ScreenOrientation.PORTRAIT -> raw
        ScreenOrientation.LANDSCAPE_LEFT -> Vector3(x = raw.y, y = -raw.x, z = raw.z)
        ScreenOrientation.REVERSE_PORTRAIT -> Vector3(x = -raw.x, y = -raw.y, z = raw.z)
        ScreenOrientation.LANDSCAPE_RIGHT -> Vector3(x = -raw.y, y = raw.x, z = raw.z)
    }

    /**
     * Computes tilt-from-vertical in degrees from a raw accelerometer vector, or null if the
     * sample is physically implausible (Ch.3 §9, §25) and must be rejected rather than turned
     * into an arbitrary angle.
     */
    fun calculateAngleDegrees(rawAccelerometer: Vector3, orientation: ScreenOrientation): Double? {
        if (!isFinite(rawAccelerometer)) return null
        val magnitude = rawAccelerometer.magnitude
        if (magnitude < MIN_VALID_MAGNITUDE) return null

        val remapped = remapForOrientation(rawAccelerometer, orientation)
        val ratio = (remapped.y / magnitude).coerceIn(-1.0, 1.0)
        val angleRadians = acos(ratio)
        val angleDegrees = Math.toDegrees(angleRadians)
        return if (angleDegrees.isFinite()) angleDegrees else null
    }

    /** Ch.3 §26: gravity-magnitude tolerance — engineering parameter, not specified in the source documents. */
    fun isGravityMagnitudeValid(vector: Vector3, toleranceMs2: Double = DEFAULT_GRAVITY_TOLERANCE_MS2): Boolean {
        if (!isFinite(vector)) return false
        return kotlin.math.abs(vector.magnitude - EARTH_GRAVITY_MS2) <= toleranceMs2
    }

    private fun isFinite(v: Vector3): Boolean =
        v.x.isFinite() && v.y.isFinite() && v.z.isFinite()

    private const val EARTH_GRAVITY_MS2 = 9.80665
    private const val DEFAULT_GRAVITY_TOLERANCE_MS2 = 2.5
    private const val MIN_VALID_MAGNITUDE = 0.5
}
