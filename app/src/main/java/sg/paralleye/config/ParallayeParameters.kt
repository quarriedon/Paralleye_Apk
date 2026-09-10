package sg.paralleye.config

/**
 * Central, version-controlled configuration for every tunable PARALLEYE methodology value
 * (Technical Methodology Ch.2 §23 "Parameter Centralisation Principle").
 *
 * No engine may hard-code a threshold, multiplier, timer or formula constant that appears
 * here — it must read it from the active [ParallayeParameters] instance instead.
 *
 * Where the source documents disagree (Ch.2 §3 "Source-of-Truth Hierarchy"), the value
 * actually used is recorded via [ParameterOrigin] rather than silently merged. Where no
 * source document resolves a value at all (Ch.2 §26 "No Hidden Placeholder Principle"),
 * the field is nullable and its absence is treated as "not yet implemented", never guessed.
 */
data class ParallayeParameters(
    val version: ParameterVersion,

    /** Ch.1 §10, revised per Ch.1 revision note (Green/Yellow boundary raised 15°→20°). */
    val zoneThresholds: ZoneThresholds = ZoneThresholds.DEFAULT,

    /**
     * Ch.1 §11 non-linear angle→load mapping. NOTE: as authored in Chapter 1 this table's
     * bands already start at the revised 20° boundary, but Chapter 5 independently flags
     * its own copy of this table as NOT updated when the zone boundary moved — see
     * docs/open-questions.md. Verified once Ch.5 is implemented (Task: Ch.5-9 engines).
     */
    val angleLoadTable: AngleLoadTable = AngleLoadTable.DEFAULT,

    /**
     * Ch.1 §14. Two source documents disagree (Algorithm Spec vs Patent Document); the
     * Patent Document set is selected as the active multiplier set per Ch.6 resolution.
     */
    val activityMultipliers: ActivityMultipliers = ActivityMultipliers.PATENT_DOCUMENT,

    /** Ch.1 §15. Dynamic Load Increment = AngleLoad × ActivityMultiplier × frameFactor. */
    val frameFactor: Double = 0.02,

    val recovery: RecoveryConfig = RecoveryConfig.DEFAULT,

    val score: ScoreConfig = ScoreConfig.DEFAULT,

    val alertStateRanges: AlertStateRanges = AlertStateRanges.DEFAULT,

    val sensitivityPresets: Map<SensitivityLevel, SensitivityAdjustment> = SensitivityAdjustment.DEFAULTS,

    val calibration: CalibrationConfig = CalibrationConfig.DEFAULT,

    val sensorFramework: SensorFrameworkConfig = SensorFrameworkConfig.DEFAULT,

    val mascot: MascotConfig = MascotConfig.DEFAULT,

    /**
     * Ch.3/4/5 explicitly instruct that the device-angle → neck-flexion transform is
     * methodologically unresolved and must be kept modular/disabled rather than guessed
     * at (Ch.2 §26). Null means "not computed"; the field exists so the pipeline slot is
     * visible and traceable, per the same principle.
     */
    val neckFlexionTransform: NeckFlexionTransformConfig? = null,
) {
    companion object {
        val PROVISIONAL = ParallayeParameters(version = ParameterVersion.INITIAL)
    }
}

enum class ParameterOrigin {
    ALGORITHM_SPECIFICATION,
    PATENT_DOCUMENT,
    TECHNICAL_METHODOLOGY_RESOLUTION,
    ENGINEERING_DEFAULT_UNVALIDATED,
}

data class ZoneThresholds(
    /** Upper bound (inclusive, degrees) of the Green zone. */
    val greenMaxDegrees: Double,
    /** Upper bound (inclusive, degrees) of the Yellow zone; Red is everything above. */
    val yellowMaxDegrees: Double,
    /** Ch.5 §44: dead-band margin (degrees) a zone transition must clear before it takes effect. Not specified in the source documents. */
    val hysteresisMarginDegrees: Double,
    val origin: ParameterOrigin,
) {
    companion object {
        val DEFAULT = ZoneThresholds(
            greenMaxDegrees = 20.0,
            yellowMaxDegrees = 25.0,
            hysteresisMarginDegrees = 0.5,
            origin = ParameterOrigin.TECHNICAL_METHODOLOGY_RESOLUTION,
        )
    }
}

/** One row of the Ch.1 §11 non-linear angle-load mapping. [minDegrees] inclusive, [maxDegrees] inclusive (null = unbounded). */
data class AngleLoadBand(val minDegrees: Double, val maxDegrees: Double?, val load: Double)

data class AngleLoadTable(val bands: List<AngleLoadBand>, val origin: ParameterOrigin) {
    /**
     * Returns the configured load for [angleDegrees], or null if no band matches (should not
     * happen for a valid table). The source table is authored as whole-degree bands with
     * integer gaps between them (e.g. "0°-20°" then "21°-25°") — a continuous angle like
     * 20.4° matches no band as literally written, so the lookup floors to whole degrees
     * first. This is an engineering interpretation of how to apply an integer table to
     * continuous sensor data, not a resolution of a disputed methodology value.
     */
    fun loadFor(angleDegrees: Double): Double? {
        val flooredAngle = kotlin.math.floor(angleDegrees)
        return bands.firstOrNull { band ->
            flooredAngle >= band.minDegrees && (band.maxDegrees == null || flooredAngle <= band.maxDegrees)
        }?.load
    }

    companion object {
        val DEFAULT = AngleLoadTable(
            bands = listOf(
                AngleLoadBand(0.0, 20.0, 0.0),
                AngleLoadBand(21.0, 25.0, 2.0),
                AngleLoadBand(26.0, 35.0, 4.0),
                AngleLoadBand(36.0, 45.0, 6.0),
                AngleLoadBand(46.0, 60.0, 8.0),
                AngleLoadBand(61.0, null, 10.0),
            ),
            origin = ParameterOrigin.ALGORITHM_SPECIFICATION,
        )
    }
}

enum class ActivityCategory { VIDEO, SCROLLING, TYPING, GAMING, UNKNOWN }

data class ActivityMultipliers(val values: Map<ActivityCategory, Double>, val origin: ParameterOrigin) {
    fun multiplierFor(category: ActivityCategory): Double =
        values[category] ?: values.getValue(ActivityCategory.UNKNOWN)

    companion object {
        /** Ch.1 §14.2 — the set selected as active for the MVP (Ch.6 resolution). */
        val PATENT_DOCUMENT = ActivityMultipliers(
            values = mapOf(
                ActivityCategory.VIDEO to 0.8,
                ActivityCategory.SCROLLING to 1.0,
                ActivityCategory.TYPING to 1.3,
                ActivityCategory.GAMING to 1.5,
                ActivityCategory.UNKNOWN to 1.0,
            ),
            origin = ParameterOrigin.PATENT_DOCUMENT,
        )

        /** Ch.1 §14.1 — retained for reference/testing only; not the active set. */
        val ALGORITHM_SPECIFICATION = ActivityMultipliers(
            values = mapOf(
                ActivityCategory.VIDEO to 1.0,
                ActivityCategory.SCROLLING to 1.3,
                ActivityCategory.TYPING to 1.6,
                ActivityCategory.GAMING to 2.0,
                ActivityCategory.UNKNOWN to 1.0,
            ),
            origin = ParameterOrigin.ALGORITHM_SPECIFICATION,
        )
    }
}

/**
 * Ch.1 §18 / Ch.8 resolution: recovery activates continuously below [activationDegrees] with
 * no minimum-duration gate (the Algorithm Spec's 120s qualification was NOT carried forward),
 * subtracting `rate × ((activationDegrees − θ) / activationDegrees)` from cumulative load each
 * qualifying cycle, floored at zero.
 */
data class RecoveryConfig(
    val activationDegrees: Double,
    val rate: Double,
    /** Ch.8 "Prompt Correction Signal": one-time bonus if posture corrects within this window of reaching Full Alert. */
    val promptCorrectionWindowSeconds: Int,
    val promptCorrectionBonus: Double,
    val origin: ParameterOrigin,
) {
    companion object {
        val DEFAULT = RecoveryConfig(
            activationDegrees = 20.0,
            rate = 0.3,
            promptCorrectionWindowSeconds = 10,
            promptCorrectionBonus = 5.0,
            origin = ParameterOrigin.TECHNICAL_METHODOLOGY_RESOLUTION,
        )
    }
}

/** Ch.9 resolution: Score = clamp(100 − CumulativeLoad × scalingFactor, 0, 100). No dismissal penalty (Ch.2 §21). */
data class ScoreConfig(val scalingFactor: Double, val origin: ParameterOrigin) {
    companion object {
        val DEFAULT = ScoreConfig(scalingFactor = 1.0, origin = ParameterOrigin.TECHNICAL_METHODOLOGY_RESOLUTION)
    }
}

/** Ch.10 resolution: 0-100 score ranges driving the alert-state machine. Each range's max is inclusive. */
data class AlertStateRanges(
    val idleMin: Int,
    val peekMin: Int,
    val peelMin: Int,
    val fullAlertMin: Int,
    val origin: ParameterOrigin,
    /**
     * Ch.10 §44-equivalent dead-band, mirroring [ZoneThresholds.hysteresisMarginDegrees]'s
     * already-established pattern: a score oscillating a point or two around a boundary (e.g.
     * 74/75) previously flipped [sg.paralleye.domain.alert.AlertLevel] -- and therefore the
     * mascot frame -- every single cycle, reported as the alert "seeming glitchy." Not a filed
     * methodology constant; applied in [sg.paralleye.domain.alert.AdaptiveAlertEngine], never
     * in [sg.paralleye.domain.alert.AlertLevel.classify] itself, so that function stays the
     * pure score classifier Ch.10 §26.1 describes.
     */
    val hysteresisMarginPoints: Int = 3,
) {
    companion object {
        val DEFAULT = AlertStateRanges(
            idleMin = 75,
            peekMin = 50,
            peelMin = 25,
            fullAlertMin = 0,
            origin = ParameterOrigin.TECHNICAL_METHODOLOGY_RESOLUTION,
            hysteresisMarginPoints = 3,
        )
    }
}

enum class SensitivityLevel { LOW, MEDIUM, HIGH }

/** Ch.2 §25: sensitivity affects approved behavioural parameters only, never the raw angle. */
data class SensitivityAdjustment(
    val accumulationMultiplier: Double,
    val alertQualificationSeconds: Int,
    val reappearanceIntervalSeconds: Int,
) {
    companion object {
        val DEFAULTS = mapOf(
            SensitivityLevel.LOW to SensitivityAdjustment(0.75, 8, 90),
            SensitivityLevel.MEDIUM to SensitivityAdjustment(1.0, 5, 60),
            SensitivityLevel.HIGH to SensitivityAdjustment(1.25, 3, 30),
        )
    }
}

/**
 * Ch.4: baseline statistical method is left configurable/undecided by the spec — default to
 * mean, clearly swappable, not presented as a validated clinical choice (Ch.2 §26).
 */
enum class BaselineStatistic { MEAN, MEDIAN, TRIMMED_MEAN }

data class CalibrationConfig(
    val baselineStatistic: BaselineStatistic,
    /** Ch.4 §10: "the exact calibration duration is not specified... must remain configurable." */
    val captureDurationMillis: Long,
    /** Ch.4 §11: minimum valid samples required before a baseline is accepted. */
    val minValidSamples: Int,
    val origin: ParameterOrigin,
) {
    companion object {
        val DEFAULT = CalibrationConfig(
            baselineStatistic = BaselineStatistic.MEAN,
            captureDurationMillis = 8_000,
            minValidSamples = 30,
            origin = ParameterOrigin.ENGINEERING_DEFAULT_UNVALIDATED,
        )
    }
}

/** Placeholder slot only — see [ParallayeParameters.neckFlexionTransform] KDoc. Not wired to any engine yet. */
data class NeckFlexionTransformConfig(val coefficient: Double, val origin: ParameterOrigin)

/** Ch.10 §47 "Configurable Parameters" — none of these values are specified in the source documents. */
data class MascotConfig(
    val appearanceAnimationMillis: Long,
    val disappearanceAnimationMillis: Long,
    val cornerOffsetXDp: Int,
    val cornerOffsetYDp: Int,
    val peelAnimationEnabled: Boolean,
) {
    companion object {
        val DEFAULT = MascotConfig(
            appearanceAnimationMillis = 500,
            disappearanceAnimationMillis = 350,
            cornerOffsetXDp = 8,
            cornerOffsetYDp = 8,
            peelAnimationEnabled = true,
        )
    }
}

/**
 * Ch.3 §34 "Configuration Parameters". None of these values are specified in the source
 * documents (Ch.3 §37 lists the sampling rate, filter method/coefficients and gravity
 * tolerance as explicitly unresolved) — all are engineering defaults pending pilot tuning.
 */
data class SensorFrameworkConfig(
    val requestedSamplingRateHz: Int,
    val startupStabilisationMillis: Long,
    val gravityToleranceMs2: Double,
    val maxValidAngularVelocityDegPerSec: Double,
    val stablePostureWindowSize: Int,
    val stablePostureMaxVariationDegrees: Double,
    val maxAcceptedSampleGapMillis: Long,
    val orientationTransitionStabilisationMillis: Long,
    val complementaryFilterAlpha: Double,
) {
    companion object {
        val DEFAULT = SensorFrameworkConfig(
            requestedSamplingRateHz = 50,
            startupStabilisationMillis = 1_000,
            gravityToleranceMs2 = 2.5,
            maxValidAngularVelocityDegPerSec = 250.0,
            stablePostureWindowSize = 5,
            stablePostureMaxVariationDegrees = 3.0,
            maxAcceptedSampleGapMillis = 500,
            orientationTransitionStabilisationMillis = 600,
            complementaryFilterAlpha = 0.90,
        )
    }
}
