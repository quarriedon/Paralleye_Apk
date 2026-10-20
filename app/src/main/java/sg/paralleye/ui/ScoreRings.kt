package sg.paralleye.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import sg.paralleye.domain.behaviour.PostureZone

/**
 * The live Monitor screen's single-value ring -- one colour (the current zone's), filled
 * proportionally to the live score out of 100.
 */
@Composable
fun ScoreRing(score: Int, ringColor: Color, modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(180.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(180.dp)) {
            val stroke = 16.dp.toPx()
            drawArc(
                color = ringColor.copy(alpha = 0.18f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = 360f * (score.coerceIn(0, 100) / 100f),
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$score", style = MaterialTheme.typography.displayMedium)
            Text("Score", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/**
 * The Progress screen's per-session ring -- segments sized by how long the session spent in
 * each [PostureZone] (the same data [sg.paralleye.ui.history.HistoryScreen] already lists as
 * text), drawn as proportional coloured arcs around the average score.
 */
@Composable
fun ZoneBreakdownRing(zoneDurationsMillis: Map<PostureZone, Long>, averageScore: Int, modifier: Modifier = Modifier) {
    val total = zoneDurationsMillis.values.sum().coerceAtLeast(1L)
    Box(modifier = modifier.size(150.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(150.dp)) {
            val stroke = 14.dp.toPx()
            var startAngle = -90f
            listOf(PostureZone.GREEN, PostureZone.YELLOW, PostureZone.RED).forEach { zone ->
                val fraction = (zoneDurationsMillis[zone] ?: 0L).toFloat() / total
                val sweep = 360f * fraction
                if (sweep > 0.5f) {
                    drawArc(
                        color = PostureDisplay.zoneColor(zone),
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Butt),
                    )
                }
                startAngle += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$averageScore", style = MaterialTheme.typography.headlineMedium)
            Text("Avg Score", style = MaterialTheme.typography.bodySmall)
        }
    }
}
