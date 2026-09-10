package sg.paralleye.ui.mascot

import androidx.annotation.DrawableRes
import sg.paralleye.R
import sg.paralleye.domain.alert.AlertLevel

/**
 * Ch.10 §39: three frames, hidden to fully visible, one per non-idle [AlertLevel]. Frame 1
 * (idle, no character) needs no drawable — [MascotVisibility.Hidden] already renders nothing.
 *
 * As of the client-supplied character redesign (single full-reveal source image, no separate
 * per-stage art provided), all three drawables are progressive crops of that one image — kept
 * from the right edge inward at increasing width (PEEK narrowest, FULL_ALERT the whole
 * character) — rather than genuinely distinct poses, so "the sequence" is a reveal-more-of-the-
 * same-image effect, not independently illustrated stages. Still one static frame per level
 * with a crossfade between them ([MsAngleAngelOverlay]), not a full 30fps tween (Ch.10 §40
 * marks the tween itself optional; see docs/open-questions.md).
 */
@DrawableRes
fun frameFor(level: AlertLevel): Int = when (level) {
    AlertLevel.IDLE -> 0 // never rendered — Hidden state shows nothing
    AlertLevel.PEEK -> R.drawable.ms_angle_angel_03_peek_face
    AlertLevel.PEEL -> R.drawable.ms_angle_angel_04_peel
    AlertLevel.FULL_ALERT -> R.drawable.ms_angle_angel_06_ready
}
