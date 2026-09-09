package sg.paralleye.session

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import sg.paralleye.config.ParallayeParameters
import sg.paralleye.config.SensitivityLevel
import sg.paralleye.config.ConfigValidator
import sg.paralleye.config.ValidationResult
import sg.paralleye.data.calibration.CalibrationRepository
import sg.paralleye.data.reporting.ReportingRepository
import sg.paralleye.domain.alert.AdaptiveAlertEngine
import sg.paralleye.domain.alert.MascotVisibility
import sg.paralleye.domain.behaviour.ActivityClassifier
import sg.paralleye.domain.behaviour.ActivityObservation
import sg.paralleye.domain.behaviour.AngleInterpretationEngine
import sg.paralleye.domain.behaviour.CumulativeLoadEngine
import sg.paralleye.domain.behaviour.DynamicLoadEngine
import sg.paralleye.domain.behaviour.PostureZone
import sg.paralleye.domain.behaviour.RecoveryEngine
import sg.paralleye.domain.behaviour.ScoringEngine
import sg.paralleye.domain.behaviour.ZoneTransitionGate
import sg.paralleye.domain.measurement.MeasurementSample
import sg.paralleye.domain.reporting.SessionSummaryAccumulator
import sg.paralleye.logging.CycleRecord
import sg.paralleye.logging.ParallayeLogger
import sg.paralleye.sensors.SensorFrameworkEngine

data class PipelineCycleResult(
    val zone: PostureZone,
    val angleLoad: Double,
    val cumulativeLoad: Double,
    val score: Int,
    val alertVisibility: MascotVisibility,
)

/**
 * Ch.11 §5-9, §33-36: the master coordinator. Owns the [MonitoringState] machine and the
 * Ch.11 §34 module initialisation order, and runs one forward pass through the behavioural
 * pipeline (Sensor Framework → Angle Interpretation → Activity → Dynamic Load → Recovery →
 * Scoring → Adaptive Alert) per valid measurement sample. It performs none of those
 * calculations itself (§3, §21, §30) — it only sequences calls into the engines from Ch.5-10.
 */
class SessionManager(
    private val context: Context,
    private val calibrationRepository: CalibrationRepository,
    private val reportingRepository: ReportingRepository,
    private val params: ParallayeParameters,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    var state: MonitoringState = MonitoringState.INITIALISATION
        private set

    private var sensorEngine: SensorFrameworkEngine? = null
    private val zoneGate = ZoneTransitionGate(params.zoneThresholds.hysteresisMarginDegrees)
    private val cumulativeLoad = CumulativeLoadEngine()
    private val alertEngine = AdaptiveAlertEngine()
    private val sessionSummaryAccumulator = SessionSummaryAccumulator()
    private var lastSampleTimestampNanos: Long? = null

    private val _cycleResults = MutableStateFlow<PipelineCycleResult?>(null)
    val cycleResults: StateFlow<PipelineCycleResult?> = _cycleResults.asStateFlow()

    /** Ch.11 §34 module initialisation order (Config → Profile → Calibration → Sensor Framework; the engines themselves are stateless/instantiated above and need no separate init step). */
    suspend fun initialise(): InitialisationOutcome {
        state = MonitoringState.INITIALISATION

        if (ConfigValidator.validate(params) is ValidationResult.Invalid) {
            return InitialisationOutcome.ConfigurationInvalid
        }

        calibrationRepository.getActiveProfile() ?: return InitialisationOutcome.OnboardingRequired
        calibrationRepository.getOriginalBaseline() ?: return InitialisationOutcome.OnboardingRequired

        val missing = PermissionChecker.missingRequiredPermissions(context)
        if (missing.isNotEmpty()) return InitialisationOutcome.PermissionsMissing(missing)

        val engine = SensorFrameworkEngine(context, params.sensorFramework)
        if (!engine.isMinimumCapabilityAvailable()) return InitialisationOutcome.SensorCapabilityUnavailable
        sensorEngine = engine

        state = MonitoringState.READY
        ParallayeLogger.monitoringStateChange("INITIALISATION", "READY")
        return InitialisationOutcome.Ready
    }

    /** Ch.11 §16, §28: begins the monitoring session; each valid sample runs one pipeline pass. */
    fun startMonitoring() {
        val engine = sensorEngine ?: return
        ParallayeLogger.monitoringStateChange(state.name, MonitoringState.MONITORING_ACTIVE.name)
        state = MonitoringState.MONITORING_ACTIVE
        sessionSummaryAccumulator.start(System.currentTimeMillis())
        engine.start()
        scope.launch {
            engine.samples.collect { sample -> if (sample != null) onSample(sample) }
        }
    }

    /** Ch.11 §26: no new samples/calculations/alerts while paused; existing state is preserved untouched. */
    fun pauseMonitoring() {
        if (state != MonitoringState.MONITORING_ACTIVE) return
        ParallayeLogger.monitoringStateChange(state.name, MonitoringState.MONITORING_PAUSED.name)
        state = MonitoringState.MONITORING_PAUSED
    }

    /** Ch.11 §27: resumes using the existing profile/calibration/session state, no re-onboarding. */
    fun resumeMonitoring() {
        if (state != MonitoringState.MONITORING_PAUSED) return
        ParallayeLogger.monitoringStateChange(state.name, MonitoringState.MONITORING_ACTIVE.name)
        state = MonitoringState.MONITORING_ACTIVE
    }

    /** Ch.11 §29, Ch.12 §11/§20: stops processing and generates+stores the Session Summary. */
    fun completeSession() {
        ParallayeLogger.monitoringStateChange(state.name, MonitoringState.SESSION_COMPLETED.name)
        val wasActive = state == MonitoringState.MONITORING_ACTIVE || state == MonitoringState.MONITORING_PAUSED
        state = MonitoringState.SESSION_COMPLETED
        sensorEngine?.stop()
        if (wasActive) {
            // Deliberately NOT `scope`, which is cancelled immediately below — this save must
            // outlive that cancellation to actually reach the database.
            val summary = sessionSummaryAccumulator.finish(System.currentTimeMillis())
            CoroutineScope(Dispatchers.Default).launch { reportingRepository.saveSession(summary) }
        }
        scope.cancel()
    }

    fun onMascotTapped() {
        val sensitivity = params.sensitivityPresets.getValue(SensitivityLevel.MEDIUM)
        alertEngine.onMascotTapped(System.currentTimeMillis(), sensitivity.reappearanceIntervalSeconds * 1000L)
    }

    private fun onSample(sample: MeasurementSample) {
        if (state != MonitoringState.MONITORING_ACTIVE) return
        val angle = sample.deviceAngleDegrees ?: return
        if (!sample.quality.isBehaviourallyValid) return

        val nowMillis = sample.timestampNanos / 1_000_000
        val intendedIntervalMillis = 1000.0 / params.sensorFramework.requestedSamplingRateHz
        val actualIntervalMillis = lastSampleTimestampNanos?.let {
            (sample.timestampNanos - it) / 1_000_000.0
        } ?: intendedIntervalMillis
        lastSampleTimestampNanos = sample.timestampNanos

        val interpretation = AngleInterpretationEngine.interpret(angle, sample.quality, params) ?: return
        val effectiveZone = zoneGate.observe(interpretation.zone, angle, params.zoneThresholds)

        // Ch.6/ACT-01: real foreground-app/keyboard signal gathering is a follow-up; Unknown
        // fallback is the spec-compliant default per Ch.2 Sec.9, not a placeholder guess.
        val activity = ActivityClassifier.classify(
            ActivityObservation(keyboardActive = false, foregroundAppCategory = null, continuousInteractionSeconds = 0),
        )
        val multiplier = params.activityMultipliers.multiplierFor(activity.category)

        val increment = DynamicLoadEngine.calculateIncrement(
            angleLoad = interpretation.angleLoad,
            activityMultiplier = multiplier,
            frameFactor = params.frameFactor,
            actualIntervalMillis = actualIntervalMillis,
            intendedIntervalMillis = intendedIntervalMillis,
        )
        cumulativeLoad.addIncrement(increment)

        var recoveryThisCycle = 0.0
        if (RecoveryEngine.isRecoveryQualifying(angle, params.recovery)) {
            val recoveryAmount = RecoveryEngine.calculateRecovery(angle, params.recovery)
            cumulativeLoad.subtract(recoveryAmount)
            recoveryThisCycle += recoveryAmount
        }

        val score = ScoringEngine.calculateScore(cumulativeLoad.currentLoad, params.score, monitoringActive = true) ?: return

        val sensitivity = params.sensitivityPresets.getValue(SensitivityLevel.MEDIUM)
        val alertResult = alertEngine.onCycle(
            score = score,
            ranges = params.alertStateRanges,
            nowMillis = nowMillis,
            reappearanceIntervalMillis = sensitivity.reappearanceIntervalSeconds * 1000L,
            promptCorrectionWindowMillis = params.recovery.promptCorrectionWindowSeconds * 1000L,
        )
        if (alertResult.promptCorrectionSignal) {
            val before = cumulativeLoad.currentLoad
            RecoveryEngine.applyPromptCorrectionBonus(cumulativeLoad, params.recovery)
            recoveryThisCycle += before - cumulativeLoad.currentLoad
        }

        sessionSummaryAccumulator.observeCycle(
            zone = effectiveZone,
            activity = activity.category,
            intervalMillis = actualIntervalMillis.toLong(),
            score = score,
            cumulativeLoad = cumulativeLoad.currentLoad,
            recoveryThisCycle = recoveryThisCycle,
            alertJustAppeared = alertResult.alertJustAppeared,
            postureJustCorrected = alertResult.postureJustCorrected,
        )

        ParallayeLogger.cycle(
            CycleRecord(
                timestampMillis = nowMillis,
                deviceAngleDegrees = angle,
                postureZone = effectiveZone.name,
                angleLoad = interpretation.angleLoad,
                activityCategory = activity.category.name,
                activityMultiplier = multiplier,
                dynamicLoadIncrement = increment,
                cumulativeLoadAfter = cumulativeLoad.currentLoad,
                score = score,
                alertState = alertResult.level.name,
            ),
        )

        _cycleResults.value = PipelineCycleResult(
            zone = effectiveZone,
            angleLoad = interpretation.angleLoad,
            cumulativeLoad = cumulativeLoad.currentLoad,
            score = score,
            alertVisibility = alertResult.visibility,
        )
    }
}
