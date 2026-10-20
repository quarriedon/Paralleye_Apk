package sg.paralleye.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import sg.paralleye.config.ActivityCategory

/**
 * "Usage Type Analytics": what fraction of a session's tracked time was spent in each
 * [ActivityCategory] -- duration-weighted (not sample-count-weighted), using the same
 * `activityDurationsMillis` map [sg.paralleye.ui.history.HistoryScreen] already stores per
 * session (Ch.12 §12-16). Real percentages only once Usage Access is granted
 * (`sensors.ForegroundAppObserver`) -- before that, every cycle classifies Unknown, so this
 * correctly renders as 100% Unknown rather than fabricating a breakdown.
 */
@Composable
fun ActivityBreakdownBar(activityDurationsMillis: Map<ActivityCategory, Long>, modifier: Modifier = Modifier) {
    val total = activityDurationsMillis.values.sum().coerceAtLeast(1L)
    val order = listOf(
        ActivityCategory.VIDEO,
        ActivityCategory.SCROLLING,
        ActivityCategory.TYPING,
        ActivityCategory.GAMING,
        ActivityCategory.UNKNOWN,
    )
    val fractions = order
        .map { category -> category to (activityDurationsMillis[category] ?: 0L).toFloat() / total }
        .filter { (_, fraction) -> fraction > 0f }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp)),
        ) {
            fractions.forEach { (category, fraction) ->
                Box(
                    modifier = Modifier
                        .weight(fraction)
                        .height(10.dp)
                        .background(PostureDisplay.activityColor(category)),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            fractions.forEach { (category, fraction) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(PostureDisplay.activityColor(category)),
                    )
                    Text(
                        "${PostureDisplay.activityLabel(category)} ${(fraction * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }
        }
    }
}
