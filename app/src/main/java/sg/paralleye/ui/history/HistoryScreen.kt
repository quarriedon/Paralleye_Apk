package sg.paralleye.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import sg.paralleye.data.db.ParallayeDatabase
import sg.paralleye.data.reporting.ReportingRepository
import sg.paralleye.domain.behaviour.PostureZone
import sg.paralleye.domain.reporting.SessionSummary
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Functional Spec System 10, Ch.12 §12-16: read-only presentation of already-completed
 * [SessionSummary] records — nothing here recalculates anything, it only formats what
 * [ReportingRepository] already has stored.
 */
@Composable
fun HistoryScreen() {
    val context = LocalContext.current
    var sessions by remember { mutableStateOf<List<SessionSummary>?>(null) }

    LaunchedEffect(Unit) {
        val repository = ReportingRepository(ParallayeDatabase.getInstance(context).reportingDao())
        sessions = withContext(Dispatchers.Default) { repository.getRecentSessions(50) }
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Session history", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Past monitoring sessions, most recent first.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
        )

        val currentSessions = sessions
        when {
            currentSessions == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            currentSessions.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "No completed sessions yet — this fills in once Paralleye has monitored a full session.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(currentSessions, key = { it.sessionId }) { session -> SessionCard(session) }
            }
        }
    }
}

@Composable
private fun SessionCard(session: SessionSummary) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatDate(session.startEpochMillis), style = MaterialTheme.typography.titleMedium)
                Text(formatDuration(session.durationMillis), style = MaterialTheme.typography.titleMedium)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SummaryStat("Final score", "${session.finalScore}")
                SummaryStat("Average", "${session.averageScore.toInt()}")
                SummaryStat("Lowest", "${session.minScore}")
            }
            Text(
                zoneBreakdown(session),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 12.dp),
            )
            if (session.alertCount > 0) {
                Text(
                    "${session.alertCount} reminder(s) shown, ${session.correctionEventCount} prompt correction(s)",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun SummaryStat(label: String, value: String) {
    Column {
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

private fun zoneBreakdown(session: SessionSummary): String {
    val totalMillis = session.zoneDurationsMillis.values.sum().coerceAtLeast(1)
    val order = listOf(PostureZone.GREEN, PostureZone.YELLOW, PostureZone.RED)
    return order.joinToString("  ·  ") { zone ->
        val percent = 100 * (session.zoneDurationsMillis[zone] ?: 0) / totalMillis
        "${zone.name.lowercase().replaceFirstChar { it.uppercase() }} $percent%"
    }
}

private fun formatDate(epochMillis: Long): String =
    SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(epochMillis)

private fun formatDuration(durationMillis: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis)
    return if (minutes < 1) "<1 min" else "$minutes min"
}
