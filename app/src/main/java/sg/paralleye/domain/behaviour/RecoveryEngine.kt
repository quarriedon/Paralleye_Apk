package sg.paralleye.domain.behaviour

import sg.paralleye.config.RecoveryConfig

/**
 * Ch.8: recovery activates continuously (no minimum-duration gate — the Algorithm Spec's
 * 120s qualification was explicitly not carried forward) whenever θ < activationDegrees.
 * Recovery runs at full [RecoveryConfig.rate] for θ up to `activationDegrees − taperDegrees`,
 * then tapers linearly to zero over just the last `taperDegrees` before the boundary — see
 * [RecoveryConfig]'s own KDoc for why (client-directed tuning, a device-testing report that
 * posture never seemed to "clear" except when the phone was held almost exactly vertical).
 * Applied every qualifying cycle. Ch.8 Rule 8: the one-time +5 Responsiveness Bonus for a
 * Prompt Correction Signal is applied HERE, by the Recovery Engine, never by the Adaptive
 * Alert Engine that emits the signal (Ch.10 §32.1) — enforced by this class owning both
 * operations.
 *
 * [rate] is defined at the sampling rate implied by the configured cadence, exactly like Ch.1
 * §15-16's Dynamic Load Increment — and per Ch.2 §11's "Time and Frame Independence Principle"
 * (the same requirement [DynamicLoadEngine] applies its frame factor under) it must be scaled
 * by actual/intended interval too, or recovery runs sampling-rate-dependent and, at a fast
 * sensor cadence, empties the accumulated load in a couple of seconds regardless of how long it
 * took to accumulate.
 */
object RecoveryEngine {
    fun calculateRecovery(
        angleDegrees: Double,
        config: RecoveryConfig,
        actualIntervalMillis: Double,
        intendedIntervalMillis: Double,
    ): Double {
        if (angleDegrees >= config.activationDegrees) return 0.0
        if (actualIntervalMillis <= 0.0 || intendedIntervalMillis <= 0.0) return 0.0
        val normalisedRate = config.rate * (actualIntervalMillis / intendedIntervalMillis)
        val taperStart = config.activationDegrees - config.taperDegrees
        val strength = if (angleDegrees <= taperStart) {
            1.0
        } else {
            (config.activationDegrees - angleDegrees) / config.taperDegrees
        }
        return normalisedRate * strength
    }

    fun isRecoveryQualifying(angleDegrees: Double, config: RecoveryConfig): Boolean =
        angleDegrees < config.activationDegrees

    /** Ch.8 Rule 8: applied once, at the start of the cycle following signal receipt; caller is responsible for not re-applying it for the same signal. */
    fun applyPromptCorrectionBonus(cumulativeLoad: CumulativeLoadEngine, config: RecoveryConfig): Double =
        cumulativeLoad.subtract(config.promptCorrectionBonus)
}
