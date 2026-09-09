package sg.paralleye.domain.reporting

import sg.paralleye.config.ActivityCategory
import sg.paralleye.domain.behaviour.PostureZone

/** Ch.12 §12-16, §23: read-only presentation of already-completed behavioural data — nothing here recalculates anything. */
data class SessionSummary(
    val sessionId: String,
    val startEpochMillis: Long,
    val endEpochMillis: Long,
    val finalScore: Int,
    val averageScore: Double,
    val minScore: Int,
    val maxScore: Int,
    /** Ch.12 §13: peak Cumulative Load reached during the session. */
    val peakCumulativeLoad: Double,
    val totalRecoveryAchieved: Double,
    val zoneDurationsMillis: Map<PostureZone, Long>,
    val activityDurationsMillis: Map<ActivityCategory, Long>,
    val alertCount: Int,
    val correctionEventCount: Int,
) {
    val durationMillis: Long get() = endEpochMillis - startEpochMillis
}
