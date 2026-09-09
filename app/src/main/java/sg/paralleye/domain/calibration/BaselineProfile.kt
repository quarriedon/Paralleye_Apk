package sg.paralleye.domain.calibration

import sg.paralleye.domain.measurement.ScreenOrientation

/** Ch.4 §23. */
enum class BaselineQuality {
    COMPLETE,
    GUIDED_CAPTURE_ONLY,
    LIMITED_ACTIVITY_COVERAGE,
    LOW_QUALITY_CAPTURE,
    INSUFFICIENT_DATA,
    INVALID,
    LEGACY,
}

/**
 * Ch.4 §13 "Baseline Data Structure", scoped to the fields this MVP build actually populates.
 * Per §13's own instruction — "unused fields should not be populated with fabricated values" —
 * activity-specific and passive-observation fields (§6.3, §6.4, §20-21) are deliberately not
 * included here rather than added as always-null placeholders; see docs/open-questions.md for
 * the scoping decision (Model 2 selected per Ch.4 §7.1, but passive observation itself is
 * deferred past this MVP build — guided capture alone is a complete, valid baseline per §7.2's
 * fallback: "use the guided baseline with reduced confidence and continue gathering
 * supplementary data without overwriting the original baseline").
 *
 * Per Ch.4 §14/§24: once created with [isOriginalBaseline] = true, a record must never be
 * overwritten — a new assessment creates a new [BaselineProfile] with a new [baselineId].
 */
data class BaselineProfile(
    val baselineId: String,
    val createdAtEpochMillis: Long,
    val profileVersion: String,
    val guidedBaselineDeviceAngleDegrees: Double,
    val captureOrientation: ScreenOrientation,
    val captureDurationMillis: Long,
    val validSampleCount: Int,
    val quality: BaselineQuality,
    val methodologyVersion: String,
    val configurationVersion: String,
    val isOriginalBaseline: Boolean,
)
