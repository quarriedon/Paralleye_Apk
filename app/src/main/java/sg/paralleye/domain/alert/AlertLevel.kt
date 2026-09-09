package sg.paralleye.domain.alert

import sg.paralleye.config.AlertStateRanges

/** Ch.10 §22, §26.1: the four patent-defined visual/behavioural states, driven purely by Posture Score. */
enum class AlertLevel {
    IDLE,
    PEEK,
    PEEL,
    FULL_ALERT;

    companion object {
        /** Ch.10 §26.1: Idle 75-100, Peek 50-74, Peel 25-49, Full Alert 0-24. Pure function of score, per §21/§31 ("the visual state shall correspond to the current... state"). */
        fun classify(score: Int, ranges: AlertStateRanges): AlertLevel = when {
            score >= ranges.idleMin -> IDLE
            score >= ranges.peekMin -> PEEK
            score >= ranges.peelMin -> PEEL
            else -> FULL_ALERT
        }
    }
}

sealed interface MascotVisibility {
    data object Hidden : MascotVisibility
    data class Visible(val level: AlertLevel) : MascotVisibility
}
