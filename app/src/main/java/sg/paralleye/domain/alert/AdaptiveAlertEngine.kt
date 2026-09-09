package sg.paralleye.domain.alert

import sg.paralleye.config.AlertStateRanges

data class AlertCycleResult(
    val level: AlertLevel,
    val visibility: MascotVisibility,
    /** Ch.10 §32.1: true exactly once per qualifying Full-Alert episode, consumed by the caller (Recovery Engine applies the actual bonus — this engine never touches Cumulative Load, per Ch.10 §9/§36/§49). */
    val promptCorrectionSignal: Boolean,
)

/**
 * Ch.10: the Adaptive Alert Engine is purely a presentation-state machine layered on top of
 * the live, score-derived [AlertLevel] — it never recalculates posture, load, recovery or
 * score (§9, §36, §49), and the visible level always reflects the *current* behavioural state
 * (§21, §31), never a level frozen at the moment of the last appearance.
 *
 * Dismissal (§29-31) only ever affects visibility: tapping the mascot starts a waiting
 * interval during which it stays hidden regardless of level changes, then reappears showing
 * whatever level is current when the interval elapses — or is cancelled outright the moment
 * the level reaches [AlertLevel.IDLE] before the interval ends (§31).
 */
class AdaptiveAlertEngine {
    private var isDismissed = false
    private var waitingUntilMillis: Long? = null
    private var previousLevel: AlertLevel = AlertLevel.IDLE
    private var fullAlertEnteredAtMillis: Long? = null
    private var promptCorrectionConsumedForCurrentFullAlert = false

    fun onCycle(
        score: Int,
        ranges: AlertStateRanges,
        nowMillis: Long,
        reappearanceIntervalMillis: Long,
        promptCorrectionWindowMillis: Long,
    ): AlertCycleResult {
        val level = AlertLevel.classify(score, ranges)

        // Ch.10 §32.1: Prompt Correction Signal — reaching Idle within the window of first
        // entering Full Alert, detected once per qualifying episode.
        var promptSignal = false
        if (level == AlertLevel.IDLE && previousLevel != AlertLevel.IDLE) {
            val enteredAt = fullAlertEnteredAtMillis
            if (enteredAt != null && !promptCorrectionConsumedForCurrentFullAlert &&
                (nowMillis - enteredAt) <= promptCorrectionWindowMillis
            ) {
                promptSignal = true
                promptCorrectionConsumedForCurrentFullAlert = true
            }
        }
        if (level == AlertLevel.FULL_ALERT && previousLevel != AlertLevel.FULL_ALERT) {
            fullAlertEnteredAtMillis = nowMillis
            promptCorrectionConsumedForCurrentFullAlert = false
        }
        previousLevel = level

        // Ch.10 §16, §29-31: visibility / dismissal / waiting / reappearance.
        val visibility: MascotVisibility = when {
            level == AlertLevel.IDLE -> {
                isDismissed = false
                waitingUntilMillis = null
                MascotVisibility.Hidden
            }
            isDismissed -> {
                val waitUntil = waitingUntilMillis
                if (waitUntil != null && nowMillis >= waitUntil) {
                    isDismissed = false
                    waitingUntilMillis = null
                    MascotVisibility.Visible(level)
                } else {
                    MascotVisibility.Hidden
                }
            }
            else -> MascotVisibility.Visible(level)
        }

        return AlertCycleResult(level, visibility, promptSignal)
    }

    /** Ch.10 §13, §29, §43: tapping the mascot dismisses it immediately; never interpreted as correction. */
    fun onMascotTapped(nowMillis: Long, reappearanceIntervalMillis: Long) {
        isDismissed = true
        waitingUntilMillis = nowMillis + reappearanceIntervalMillis
    }

    fun reset() {
        isDismissed = false
        waitingUntilMillis = null
        previousLevel = AlertLevel.IDLE
        fullAlertEnteredAtMillis = null
        promptCorrectionConsumedForCurrentFullAlert = false
    }
}
