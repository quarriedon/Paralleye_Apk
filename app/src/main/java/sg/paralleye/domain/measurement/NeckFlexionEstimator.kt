package sg.paralleye.domain.measurement

import sg.paralleye.config.NeckFlexionTransformConfig

/**
 * Ch.3 §20: the device-angle → neck-flexion transform is methodologically unresolved (unclear
 * reference convention, θ vs 90°−θ, coefficient 0.4 vs 0.5). Ch.3 §37 instructs: "keep any
 * anatomical estimate modular and clearly provisional" until resolved — this object is that
 * modular slot. With no config supplied it always returns null (not computed), matching
 * ParallayeParameters.neckFlexionTransform defaulting to null. See docs/open-questions.md #1.
 */
object NeckFlexionEstimator {
    fun estimate(deviceAngleDegrees: Double, config: NeckFlexionTransformConfig?): Double? {
        if (config == null) return null
        return (90.0 - deviceAngleDegrees) * config.coefficient
    }
}
