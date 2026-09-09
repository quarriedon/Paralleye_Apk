package sg.paralleye.domain.behaviour

import sg.paralleye.config.RecoveryConfig

/**
 * Ch.8: recovery activates continuously (no minimum-duration gate — the Algorithm Spec's
 * 120s qualification was explicitly not carried forward) whenever θ < activationDegrees.
 * Recovery = rate × ((activationDegrees − θ) / activationDegrees), applied every qualifying
 * monitoring cycle. Ch.8 Rule 8: the one-time +5 Responsiveness Bonus for a Prompt Correction
 * Signal is applied HERE, by the Recovery Engine, never by the Adaptive Alert Engine that
 * emits the signal (Ch.10 §32.1) — enforced by this class owning both operations.
 */
object RecoveryEngine {
    fun calculateRecovery(angleDegrees: Double, config: RecoveryConfig): Double {
        if (angleDegrees >= config.activationDegrees) return 0.0
        return config.rate * ((config.activationDegrees - angleDegrees) / config.activationDegrees)
    }

    fun isRecoveryQualifying(angleDegrees: Double, config: RecoveryConfig): Boolean =
        angleDegrees < config.activationDegrees

    /** Ch.8 Rule 8: applied once, at the start of the cycle following signal receipt; caller is responsible for not re-applying it for the same signal. */
    fun applyPromptCorrectionBonus(cumulativeLoad: CumulativeLoadEngine, config: RecoveryConfig): Double =
        cumulativeLoad.subtract(config.promptCorrectionBonus)
}
