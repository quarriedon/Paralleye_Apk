package sg.paralleye.domain.behaviour

import sg.paralleye.config.ParallayeParameters
import sg.paralleye.domain.measurement.MeasurementQuality

/**
 * Ch.5 §8 "Outputs". [severityLevel] is an alias of [zone] — the source lists "posture
 * severity level" as a distinct output but never defines separate values for it beyond zone
 * and load, so no third classification is invented here (Ch.2 §26). Kept as its own field so
 * a future methodology revision can differentiate it without changing this type's shape.
 */
data class AngleInterpretationResult(
    val zone: PostureZone,
    val angleLoad: Double,
    val severityLevel: PostureZone,
    val angleInterpretationVersion: String,
    val configurationVersion: String,
    val measurementQuality: MeasurementQuality,
)

/**
 * Ch.5 §5-11: a pure, deterministic interpretation layer. Given the same validated angle it
 * always produces the same zone and load, regardless of score, activity, calibration or any
 * other behavioural state (§11). It does not itself decide whether a sample should influence
 * downstream accumulation — that's [MeasurementQuality] (Sensor Framework) plus the caller's
 * own hysteresis gating (see [ZoneTransitionGate]), kept deliberately separate so this
 * function stays pure per §11.
 */
object AngleInterpretationEngine {
    const val VERSION = "1.0"

    fun interpret(
        angleDegrees: Double,
        quality: MeasurementQuality,
        params: ParallayeParameters,
    ): AngleInterpretationResult? {
        val load = params.angleLoadTable.loadFor(angleDegrees) ?: return null
        val zone = PostureZone.classify(angleDegrees, params.zoneThresholds)
        return AngleInterpretationResult(
            zone = zone,
            angleLoad = load,
            severityLevel = zone,
            angleInterpretationVersion = VERSION,
            configurationVersion = params.version.configVersion,
            measurementQuality = quality,
        )
    }
}

/**
 * Ch.5 §44 "Hysteresis": a dead-band wrapper around zone classification so small sensor
 * fluctuation near a boundary doesn't repeatedly flip the *effective* zone used by downstream
 * behavioural processing. Deliberately separate from [AngleInterpretationEngine] (which stays
 * pure per §11) — this class holds the only state, and never modifies the objective angle
 * itself, only when a transition is allowed to register.
 */
class ZoneTransitionGate(private val marginDegrees: Double) {
    private var effectiveZone: PostureZone? = null

    fun observe(rawZone: PostureZone, angleDegrees: Double, thresholds: sg.paralleye.config.ZoneThresholds): PostureZone {
        val current = effectiveZone
        if (current == null || current == rawZone) {
            effectiveZone = rawZone
            return rawZone
        }
        // Only accept a transition once the angle has moved past the boundary by the margin,
        // not merely across it — prevents 14.9/15.1/14.8/15.2-style oscillation.
        val crossedWithMargin = when {
            rawZone > current -> angleDegrees > boundaryAbove(current, thresholds) + marginDegrees
            rawZone < current -> angleDegrees < boundaryBelow(current, thresholds) - marginDegrees
            else -> true
        }
        if (crossedWithMargin) {
            effectiveZone = rawZone
        }
        return effectiveZone!!
    }

    fun reset() {
        effectiveZone = null
    }

    private fun boundaryAbove(zone: PostureZone, thresholds: sg.paralleye.config.ZoneThresholds): Double = when (zone) {
        PostureZone.GREEN -> thresholds.greenMaxDegrees
        PostureZone.YELLOW -> thresholds.yellowMaxDegrees
        PostureZone.RED -> Double.MAX_VALUE
    }

    private fun boundaryBelow(zone: PostureZone, thresholds: sg.paralleye.config.ZoneThresholds): Double = when (zone) {
        PostureZone.GREEN -> Double.MIN_VALUE
        PostureZone.YELLOW -> thresholds.greenMaxDegrees
        PostureZone.RED -> thresholds.yellowMaxDegrees
    }

    private operator fun PostureZone.compareTo(other: PostureZone): Int = this.ordinal.compareTo(other.ordinal)
}
