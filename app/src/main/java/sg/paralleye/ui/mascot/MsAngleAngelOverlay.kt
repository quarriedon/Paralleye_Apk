package sg.paralleye.ui.mascot

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
 */
@Composable
fun MsAngleAngelOverlay(
    visibility: MascotVisibility,
    config: MascotConfig,
    onTapped: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.offset(x = -config.cornerOffsetXDp.dp, y = config.cornerOffsetYDp.dp)) {
        // The plain Crossfade() composable (used previously) hard-codes Alignment.TopStart for
        // its own internal content placement, with no parameter to override it -- so whenever
        // its box ended up wider than the currently-visible frame (e.g. mid-transition between
        // a narrow PEEK crop and a wider PEEL/FULL_ALERT one), that frame rendered left-aligned
        // *inside* Crossfade's own bounds, appearing to float away from the true right edge and
        // slide toward it as later, wider frames filled more of the space -- the exact bug
        // reported. AnimatedContent (what Crossfade is itself built on) exposes contentAlignment
        // directly, so every frame is anchored to the right edge instead of an unconfigurable
        // default; fadeIn/fadeOut together reproduce Crossfade's plain cross-dissolve.
        val fadeSpec = tween<Float>(durationMillis = config.appearanceAnimationMillis.toInt())
        AnimatedContent(
            targetState = visibility,
            transitionSpec = { fadeIn(animationSpec = fadeSpec) togetherWith fadeOut(animationSpec = fadeSpec) },
            contentAlignment = Alignment.CenterEnd,
            label = "ms_angle_angel_visibility",
        ) { state ->
            if (state is MascotVisibility.Visible) {
                // Fixed height, no explicit width: the three frames frameFor() maps to are
                // cropped tightly to the character (no more phone-bezel padding), each at a
                // slightly different aspect ratio. A fixed *square* box previously forced the
                // tallest/narrowest of them down to fit, rendering far smaller than intended —
                // letting width follow each frame's own intrinsic aspect ratio avoids that.
                // 260dp per client instruction ("not dim and small, up to 30% of that
                // quadrant") -- roughly double the previous 130dp.
                Image(
                    painter = painterResource(id = frameFor(state.level)),
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
