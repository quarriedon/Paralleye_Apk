package sg.paralleye.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Ch.12 §23: "Historical records shall remain unchanged once stored... appended to the
 * existing history" — this table is insert-only from the repository's perspective, never
 * updated or deleted except by Ch.4 §35.4 "Delete All Data".
 *
 * Zone/activity duration maps are serialised as "NAME:millis,NAME:millis" pairs rather than a
 * separate join table — a pragmatic MVP simplification for a small, fixed enum-keyed map, not
 * a general-purpose relational design.
 */
@Entity(tableName = "session_summary")
data class SessionSummaryEntity(
    @PrimaryKey val sessionId: String,
    val startEpochMillis: Long,
    val endEpochMillis: Long,
    val finalScore: Int,
    val averageScore: Double,
    val minScore: Int,
    val maxScore: Int,
    val peakCumulativeLoad: Double,
    val totalRecoveryAchieved: Double,
    val zoneDurationsSerialised: String,
    val activityDurationsSerialised: String,
    val alertCount: Int,
    val correctionEventCount: Int,
)
