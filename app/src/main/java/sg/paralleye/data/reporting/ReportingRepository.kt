package sg.paralleye.data.reporting

import sg.paralleye.config.ActivityCategory
import sg.paralleye.data.db.dao.ReportingDao
import sg.paralleye.data.db.entities.SessionSummaryEntity
import sg.paralleye.domain.behaviour.PostureZone
import sg.paralleye.domain.reporting.SessionSummary

class ReportingRepository(private val dao: ReportingDao) {

    suspend fun saveSession(summary: SessionSummary) {
        dao.insertSession(summary.toEntity())
    }

    suspend fun getAllSessions(): List<SessionSummary> = dao.getAllSessions().map { it.toDomain() }

    suspend fun getRecentSessions(limit: Int): List<SessionSummary> = dao.getRecentSessions(limit).map { it.toDomain() }

    /** Ch.4 §35.4. */
    suspend fun deleteAllSessions() = dao.deleteAllSessions()

    private fun SessionSummary.toEntity() = SessionSummaryEntity(
        sessionId = sessionId,
        startEpochMillis = startEpochMillis,
        endEpochMillis = endEpochMillis,
        finalScore = finalScore,
        averageScore = averageScore,
        minScore = minScore,
        maxScore = maxScore,
        peakCumulativeLoad = peakCumulativeLoad,
        totalRecoveryAchieved = totalRecoveryAchieved,
        zoneDurationsSerialised = zoneDurationsMillis.serialise(),
        activityDurationsSerialised = activityDurationsMillis.serialise(),
        alertCount = alertCount,
        correctionEventCount = correctionEventCount,
    )

    private fun SessionSummaryEntity.toDomain() = SessionSummary(
        sessionId = sessionId,
        startEpochMillis = startEpochMillis,
        endEpochMillis = endEpochMillis,
        finalScore = finalScore,
        averageScore = averageScore,
        minScore = minScore,
        maxScore = maxScore,
        peakCumulativeLoad = peakCumulativeLoad,
        totalRecoveryAchieved = totalRecoveryAchieved,
        zoneDurationsMillis = deserialiseZones(zoneDurationsSerialised),
        activityDurationsMillis = deserialiseActivities(activityDurationsSerialised),
        alertCount = alertCount,
        correctionEventCount = correctionEventCount,
    )

    private fun <K> Map<K, Long>.serialise(): String = entries.joinToString(",") { "${it.key}:${it.value}" }

    private fun deserialiseZones(raw: String): Map<PostureZone, Long> = deserialise(raw) { PostureZone.valueOf(it) }

    private fun deserialiseActivities(raw: String): Map<ActivityCategory, Long> = deserialise(raw) { ActivityCategory.valueOf(it) }

    private fun <K> deserialise(raw: String, keyOf: (String) -> K): Map<K, Long> {
        if (raw.isBlank()) return emptyMap()
        return raw.split(",").associate { pair ->
            val (name, millis) = pair.split(":")
            keyOf(name) to millis.toLong()
        }
    }
}
