package sg.paralleye.domain.reporting

import sg.paralleye.config.ActivityCategory
import sg.paralleye.domain.behaviour.PostureZone
import java.util.UUID

/**
 * Ch.12 §11-16: builds the read-only [SessionSummary] from live cycle data as a session
 * progresses, without recalculating anything itself — every value it accumulates is handed to
 * it already-computed by [sg.paralleye.session.SessionManager].
 */
class SessionSummaryAccumulator {
    private var sessionId: String = UUID.randomUUID().toString()
    private var startEpochMillis: Long = 0
    private val zoneDurations = mutableMapOf<PostureZone, Long>()
    private val activityDurations = mutableMapOf<ActivityCategory, Long>()
    private val scores = mutableListOf<Int>()
    private var peakCumulativeLoad = 0.0
    private var totalRecovery = 0.0
    private var alertCount = 0
    private var correctionCount = 0

    fun start(nowMillis: Long) {
        sessionId = UUID.randomUUID().toString()
        startEpochMillis = nowMillis
        zoneDurations.clear()
        activityDurations.clear()
        scores.clear()
        peakCumulativeLoad = 0.0
        totalRecovery = 0.0
        alertCount = 0
        correctionCount = 0
    }

    fun observeCycle(
        zone: PostureZone,
        activity: ActivityCategory,
        intervalMillis: Long,
        score: Int,
        cumulativeLoad: Double,
        recoveryThisCycle: Double,
        alertJustAppeared: Boolean,
        postureJustCorrected: Boolean,
    ) {
        zoneDurations[zone] = (zoneDurations[zone] ?: 0L) + intervalMillis
        activityDurations[activity] = (activityDurations[activity] ?: 0L) + intervalMillis
        scores += score
        if (cumulativeLoad > peakCumulativeLoad) peakCumulativeLoad = cumulativeLoad
        totalRecovery += recoveryThisCycle
        if (alertJustAppeared) alertCount++
        if (postureJustCorrected) correctionCount++
    }

    fun finish(nowMillis: Long): SessionSummary = SessionSummary(
        sessionId = sessionId,
        startEpochMillis = startEpochMillis,
        endEpochMillis = nowMillis,
        finalScore = scores.lastOrNull() ?: 100,
        averageScore = if (scores.isEmpty()) 100.0 else scores.average(),
        minScore = scores.minOrNull() ?: 100,
        maxScore = scores.maxOrNull() ?: 100,
        peakCumulativeLoad = peakCumulativeLoad,
        totalRecoveryAchieved = totalRecovery,
        zoneDurationsMillis = zoneDurations.toMap(),
        activityDurationsMillis = activityDurations.toMap(),
        alertCount = alertCount,
        correctionEventCount = correctionCount,
    )
}
