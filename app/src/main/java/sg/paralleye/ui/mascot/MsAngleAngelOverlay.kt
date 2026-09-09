package sg.paralleye.ui.mascot

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import sg.paralleye.config.MascotConfig
import sg.paralleye.domain.alert.MascotVisibility

/**
 * Ch.10 §38: fixed upper-left corner anchor, never repositioned. Ch.10 §13/§43: tap
 * immediately dismisses, no confirmation dialog. Ch.10 §42: smooth transition between states
 * (a Crossfade rather than the full corner-peel rig — see MascotFrame's KDoc on scope).
 */
@Composable
fun MsAngleAngelOverlay(
    visibility: MascotVisibility,
    config: MascotConfig,
    onTapped: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.offset(x = config.cornerOffsetXDp.dp, y = config.cornerOffsetYDp.dp)) {
        Crossfade(
            targetState = visibility,
            animationSpec = tween(durationMillis = config.appearanceAnimationMillis.toInt()),
            label = "ms_angle_angel_visibility",
        ) { state ->
            if (state is MascotVisibility.Visible) {
                Image(
                    painter = painterResource(id = frameFor(state.level)),
                    contentDescription = "Ms Angle Angel posture reminder",
                    modifier = Modifier
                        .size(96.dp)
                        .clickable(onClickLabel = "Dismiss posture reminder") { onTapped() }
                        .semantics { contentDescription = "Ms Angle Angel posture reminder, tap to dismiss" },
                )
            }
        }
    }
}
