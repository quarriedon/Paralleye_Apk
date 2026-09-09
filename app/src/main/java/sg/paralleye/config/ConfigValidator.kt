package sg.paralleye.config

/**
 * Ch.2 §35 "Configuration Validation Principle": before monitoring begins, the active
 * configuration must be validated. An invalid configuration must never be used silently.
 */
object ConfigValidator {

    fun validate(params: ParallayeParameters): ValidationResult {
        val problems = mutableListOf<String>()

        val zones = params.zoneThresholds
        if (!(zones.greenMaxDegrees < zones.yellowMaxDegrees)) {
            problems += "Zone thresholds are not ordered: green=${zones.greenMaxDegrees} must be < yellow=${zones.yellowMaxDegrees}"
        }

        val bands = params.angleLoadTable.bands
        if (bands.isEmpty()) {
            problems += "Angle-load table has no bands"
        } else {
            val sorted = bands.sortedBy { it.minDegrees }
            for (band in bands) {
                if (band.load < 0.0) problems += "Angle-load band [${band.minDegrees}, ${band.maxDegrees}] has negative load ${band.load}"
                if (band.maxDegrees != null && band.maxDegrees < band.minDegrees) {
                    problems += "Angle-load band has max < min: [${band.minDegrees}, ${band.maxDegrees}]"
                }
            }
            for (i in 0 until sorted.size - 1) {
                val current = sorted[i]
                val next = sorted[i + 1]
                val currentMax = current.maxDegrees
                if (currentMax == null) {
                    problems += "Angle-load band starting at ${current.minDegrees} is unbounded but is not the last band"
                } else if (currentMax >= next.minDegrees) {
                    problems += "Angle-load bands overlap: [${current.minDegrees}, $currentMax] and [${next.minDegrees}, ${next.maxDegrees}]"
                } else if (currentMax + 1.0 < next.minDegrees) {
                    problems += "Angle-load bands have a gap between $currentMax and ${next.minDegrees}"
                }
            }
            if (sorted.last().maxDegrees != null) {
                problems += "Angle-load table has no unbounded top band to cover all angles"
            }
        }

        params.activityMultipliers.values.forEach { (category, multiplier) ->
            if (multiplier < 0.0) problems += "Activity multiplier for $category is negative: $multiplier"
        }
        if (ActivityCategory.UNKNOWN !in params.activityMultipliers.values) {
            problems += "Activity multipliers must define a fallback for ${ActivityCategory.UNKNOWN}"
        }

        if (params.frameFactor <= 0.0) {
            problems += "Frame factor must be positive, was ${params.frameFactor}"
        }

        if (params.recovery.rate < 0.0) problems += "Recovery rate must be non-negative, was ${params.recovery.rate}"
        if (params.recovery.activationDegrees <= 0.0) {
            problems += "Recovery activation angle must be positive, was ${params.recovery.activationDegrees}"
        }
        if (params.recovery.promptCorrectionWindowSeconds < 0) {
            problems += "Recovery prompt-correction window must be non-negative"
        }

        if (params.score.scalingFactor <= 0.0) {
            problems += "Score scaling factor must be positive, was ${params.score.scalingFactor}"
        }

        val alerts = params.alertStateRanges
        val orderedAlertBounds = listOf(alerts.fullAlertMin, alerts.peelMin, alerts.peekMin, alerts.idleMin)
        if (orderedAlertBounds != orderedAlertBounds.sorted() || orderedAlertBounds.toSet().size != 4) {
            problems += "Alert-state ranges are not strictly ordered: fullAlertMin=${alerts.fullAlertMin} < peelMin=${alerts.peelMin} < peekMin=${alerts.peekMin} < idleMin=${alerts.idleMin} required"
        }
        if (alerts.fullAlertMin != 0) problems += "Alert-state ranges must start at 0, was ${alerts.fullAlertMin}"
        if (alerts.idleMin > 100) problems += "Alert-state idle range must not exceed 100"

        if (params.sensitivityPresets.keys != SensitivityLevel.entries.toSet()) {
            problems += "Sensitivity presets must define all of ${SensitivityLevel.entries}, found ${params.sensitivityPresets.keys}"
        }
        params.sensitivityPresets.forEach { (level, adjustment) ->
            if (adjustment.accumulationMultiplier <= 0.0) problems += "Sensitivity $level has non-positive accumulationMultiplier"
            if (adjustment.alertQualificationSeconds < 0) problems += "Sensitivity $level has negative alertQualificationSeconds"
            if (adjustment.reappearanceIntervalSeconds < 0) problems += "Sensitivity $level has negative reappearanceIntervalSeconds"
        }

        return if (problems.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(problems)
    }
}

sealed interface ValidationResult {
    data object Valid : ValidationResult
    data class Invalid(val problems: List<String>) : ValidationResult
}
