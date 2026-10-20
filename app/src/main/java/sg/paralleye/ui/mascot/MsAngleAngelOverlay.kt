package sg.paralleye.ui.mascot

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import sg.paralleye.config.MascotConfig
import sg.paralleye.domain.alert.AlertLevel
import sg.paralleye.domain.alert.MascotVisibility

/**
 * Fixed right-edge anchor, never repositioned. Ch.10 §38 text says upper-left ("shall always
 * emerge from the upper-left corner... shall not appear from the right side"), but the actual
 * commissioned reference app the client had a separate team build anchors it to the right edge
 * — confirmed from that app's own screenshots, both in-app and as a system overlay over other
 * apps. Following the reference over the written text here on explicit client instruction; the
 * conflict is recorded in docs/open-questions.md rather than silently overridden. Ch.10 §13/§43:
 * tap immediately dismisses, no confirmation dialog. Ch.10 §42: smooth transition between states
 * (a Crossfade rather than the full corner-peel rig — see MascotFrame's KDoc on scope).
 *
 * Caller must place this with `Modifier.align(Alignment.TopEnd)` in a Box — the offset here is
 * an inset from that anchor, not an absolute position.
 *
 * ## Why per-level `AnimatedVisibility`, not `AnimatedContent`
 *
 * Two previous approaches both produced the same reported symptom ("floats away from the
 * border, slides back toward it"): the plain `Crossfade()` composable hard-codes
 * `Alignment.TopStart` internally with no override, and the `AnimatedContent` that replaced it
 * (contentAlignment = CenterEnd) still reportedly showed it after the character art changed to
 * a set of frames with much larger relative width differences between levels (142dp/214dp/473dp
 * vs. the previous character's narrower spread) -- `AnimatedContent`'s shared-container sizing
 * model re-measures/animates ONE box that both the outgoing and incoming frame are placed
 * inside, and that box's own transient size during the crossfade is where both previous bugs
 * actually lived, not in which `Alignment` was requested. Rather than chase a third variant of
 * the same failure mode, each [AlertLevel]'s frame now gets its own independent
 * `AnimatedVisibility`, `Modifier.align`ed directly on this `Box` -- a plain `Box` aligns each
 * child to its own edge using that child's own current size, with no shared container to
 * mis-animate in between. `fadeIn`/`fadeOut` alone (no `expandHorizontally`/`shrinkHorizontally`)
 * mean each `AnimatedVisibility` holds its full target size for its whole lifetime, so there is
 * nothing to slide: a frame is either fading at its own, always-correct, right-aligned position,
 * or not composed at all.
 */
@Composable
fun MsAngleAngelOverlay(
    visibility: MascotVisibility,
    config: MascotConfig,
    onTapped: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.offset(x = -config.cornerOffsetXDp.dp, y = config.cornerOffsetYDp.dp)) {
        val fadeSpec = tween<Float>(durationMillis = config.appearanceAnimationMillis.toInt())
        for (level in AlertLevel.entries) {
            if (level == AlertLevel.IDLE) continue
            AnimatedVisibility(
                visible = visibility is MascotVisibility.Visible && visibility.level == level,
                enter = fadeIn(animationSpec = fadeSpec),
                exit = fadeOut(animationSpec = fadeSpec),
                modifier = Modifier.align(Alignment.CenterEnd),
            ) {
                // Fixed height, no explicit width: the three frames frameFor() maps to are
                // cropped tightly to the character (no more phone-bezel padding), each at a
                // slightly different aspect ratio. A fixed *square* box previously forced the
                // tallest/narrowest of them down to fit, rendering far smaller than intended —
                // letting width follow each frame's own intrinsic aspect ratio avoids that.
                // 260dp per client instruction ("not dim and small, up to 30% of that
                // quadrant") -- roughly double the previous 130dp.
                Image(
                    painter = painterResource(id = frameFor(level)),
                    contentDescription = "Ms Angle Angel posture reminder",
                    modifier = Modifier
                        .height(260.dp)
                        .clickable(onClickLabel = "Dismiss posture reminder") { onTapped() }
                        .semantics { contentDescription = "Ms Angle Angel posture reminder, tap to dismiss" },
                )
            }
        }
    }
}
