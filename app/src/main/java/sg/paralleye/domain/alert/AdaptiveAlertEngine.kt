package sg.paralleye.domain.alert

import sg.paralleye.config.AlertStateRanges

data class AlertCycleResult(
    val level: AlertLevel,
    val visibility: MascotVisibility,
    /** Ch.10 §32.1: true exactly once per qualifying Full-Alert episode, consumed by the caller (Recovery Engine applies the actual bonus — this engine never touches Cumulative Load, per Ch.10 §9/§36/§49). */
    val promptCorrectionSignal: Boolean,
    /** Ch.12 §16: true exactly on the cycle the mascot transitions from Hidden to Visible — for the reporting system's alert count, not consumed by any behavioural engine. */
    val alertJustAppeared: Boolean,
    /** Ch.12 §16: true exactly on the cycle the behavioural level reaches Idle from a non-Idle level — a "posture-correction event" for reporting, distinct from the Ch.10 §32.1 prompt-correction bonus condition. */
    val postureJustCorrected: Boolean,
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
    private var wasVisible = false
    private var fullAlertEnteredAtMillis: Long? = null
    private var promptCorrectionConsumedForCurrentFullAlert = false

    // A score sitting right on a boundary (e.g. 74/75) previously flipped AlertLevel, and
    // therefore the mascot frame, every single cycle -- reported as the alert "seeming
    // glitchy." Gated here rather than in AlertLevel.classify, mirroring how
    // ZoneTransitionGate wraps AngleInterpretationEngine's zone classification without
    // touching it: classify() stays the pure function of score Ch.10 §26.1 describes, this
    // is purely a presentation-layer dead-band, same as ZoneTransitionGate is for zones.
    private var effectiveLevel: AlertLevel? = null

    fun onCycle(
        score: Int,
        ranges: AlertStateRanges,
        nowMillis: Long,
        reappearanceIntervalMillis: Long,
        promptCorrectionWindowMillis: Long,
    ): AlertCycleResult {
        val level = gatedLevel(AlertLevel.classify(score, ranges), score, ranges)
        val postureJustCorrected = level == AlertLevel.IDLE && previousLevel != AlertLevel.IDLE

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

        val isVisibleNow = visibility is MascotVisibility.Visible
        val alertJustAppeared = isVisibleNow && !wasVisible
        wasVisible = isVisibleNow

        return AlertCycleResult(level, visibility, promptSignal, alertJustAppeared, postureJustCorrected)
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
        wasVisible = false
        fullAlertEnteredAtMillis = null
        promptCorrectionConsumedForCurrentFullAlert = false
        effectiveLevel = null
    }

    private fun gatedLevel(rawLevel: AlertLevel, score: Int, ranges: AlertStateRanges): AlertLevel {
        val current = effectiveLevel
        if (current == null || current == rawLevel) {
            effectiveLevel = rawLevel
            return rawLevel
        }
        // Same "boundary crossed by margin, not merely crossed" rule as ZoneTransitionGate,
        // just score-based: rawLevel more severe (higher ordinal) needs score to have dropped
        // meaningfully below current's own threshold; rawLevel milder needs score to have
        // risen meaningfully above the next-milder level's own threshold.
        val crossedWithMargin = when {
            rawLevel.ordinal > current.ordinal -> score < levelMin(current, ranges) - ranges.hysteresisMarginPoints
            rawLevel.ordinal < current.ordinal -> score > levelMin(nextMilder(current), ranges) + ranges.hysteresisMarginPoints
            else -> true
        }
        if (crossedWithMargin) {
            effectiveLevel = rawLevel
        }
        return effectiveLevel!!
    }

    private fun levelMin(level: AlertLevel, ranges: AlertStateRanges): Int = when (level) {
        AlertLevel.IDLE -> ranges.idleMin
        AlertLevel.PEEK -> ranges.peekMin
        AlertLevel.PEEL -> ranges.peelMin
        AlertLevel.FULL_ALERT -> ranges.fullAlertMin
    }

    private fun nextMilder(level: AlertLevel): AlertLevel = when (level) {
        AlertLevel.IDLE -> AlertLevel.IDLE // already mildest; unused (rawLevel can't be milder than IDLE)
        AlertLevel.PEEK -> AlertLevel.IDLE
        AlertLevel.PEEL -> AlertLevel.PEEK
        AlertLevel.FULL_ALERT -> AlertLevel.PEEL
    }
}
