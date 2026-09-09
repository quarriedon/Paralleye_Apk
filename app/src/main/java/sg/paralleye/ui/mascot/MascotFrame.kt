package sg.paralleye.ui.mascot

import androidx.annotation.DrawableRes
import sg.paralleye.R
import sg.paralleye.domain.alert.AlertLevel

/**
 * Ch.10 §39, and the corrected asset pack's signature corner-peel sequence
 * (docs/source-materials/Angle Angel/README_FOR_CLAUDE_CODE.txt): six frames from hidden to
 * fully visible. Frame 1 (idle, no character) needs no drawable — [MascotVisibility.Hidden]
 * already renders nothing. The four behavioural levels are mapped to the settled frame that
 * best represents each stage of the emergence sequence; the source assets don't define
 * intermediate frames per level, so this is a one-frame-per-level MVP mapping rather than a
 * full 30fps tween (see docs/open-questions.md — deferred, not fabricated).
 */
@DrawableRes
fun frameFor(level: AlertLevel): Int = when (level) {
    AlertLevel.IDLE -> 0 // never rendered — Hidden state shows nothing
    AlertLevel.PEEK -> R.drawable.ms_angle_angel_03_peek_face
    AlertLevel.PEEL -> R.drawable.ms_angle_angel_04_peel
    AlertLevel.FULL_ALERT -> R.drawable.ms_angle_angel_06_ready
}
