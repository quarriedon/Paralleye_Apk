package sg.paralleye.domain.behaviour

import sg.paralleye.config.ScoreConfig

/**
 * Ch.9: Posture Score = max(0, 100 − CumulativeLoad × scalingFactor), clamped to [0, 100].
 * Ch.9 §"update only while monitoring active": returns null (no new score) rather than a
 * value when monitoring isn't active — the caller must not fabricate a frozen or recalculated
 * score for a suspended session.
 */
object ScoringEngine {
    fun calculateScore(cumulativeLoad: Double, config: ScoreConfig, monitoringActive: Boolean): Int? {
        if (!monitoringActive) return null
        val raw = 100.0 - (cumulativeLoad * config.scalingFactor)
        return raw.coerceIn(0.0, 100.0).toInt()
    }
}
