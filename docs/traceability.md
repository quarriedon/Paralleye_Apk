# PARALLEYE — Requirements Traceability

Per Technical Methodology Ch.2 §37. Updated as each module is implemented — not filled in
speculatively ahead of the code existing.

| Req ID | Description | Source | Component | Test | Status |
|---|---|---|---|---|---|
| GOV-01 | Central versioned parameter configuration | Ch.2 §23, §24 | `config.ParallayeParameters`, `config.ParameterVersion` | `ConfigValidatorTest` (indirect) | Done |
| GOV-02 | Configuration validation before monitoring begins | Ch.2 §35 | `config.ConfigValidator` | `ConfigValidatorTest` | Done |
| GOV-03 | Structured cycle logging, privacy-redacted | Ch.2 §13, §33 | `logging.CycleRecord`, `logging.ParallayeLogger` | — | Done (untested: thin Android `Log` wrapper) |
| GOV-04 | Non-linear angle-load mapping table | Ch.1 §11 | `config.AngleLoadTable` | `ConfigValidatorTest.angle-load lookup` | Done |
| GOV-05 | Activity multiplier set selection (Patent Document values active) | Ch.1 §14, Ch.6 | `config.ActivityMultipliers` | — | Config only; Activity Engine not yet implemented |
| GOV-06 | Recovery formula + Prompt Correction Signal parameters | Ch.1 §18, Ch.8 | `config.RecoveryConfig` | — | Config only; Recovery Engine not yet implemented |
| GOV-07 | Score formula parameters | Ch.1 §19, Ch.9 | `config.ScoreConfig` | — | Config only; Scoring Engine not yet implemented |
| GOV-08 | Alert-state score ranges | Ch.10 | `config.AlertStateRanges` | — | Config only; Alert Engine not yet implemented |
| GOV-09 | Sensitivity presets (Low/Medium/High) | Ch.2 §25 | `config.SensitivityAdjustment` | — | Config only; not yet wired to any engine |

| SENS-01 | Objective device-angle calculation from accelerometer, clamped/validated | Ch.3 §7-§10 | `domain.measurement.DeviceAngleCalculator` | `DeviceAngleCalculatorTest` | Done — axis convention needs physical confirmation, see open-questions.md #6 |
| SENS-02 | Portrait/landscape-left/landscape-right axis remapping | Ch.3 §16-§18 | `DeviceAngleCalculator.remapForOrientation` | `DeviceAngleCalculatorTest` | Done |
| SENS-03 | Sensor fusion (complementary filter) | Ch.3 §11.4 | `domain.measurement.ComplementaryFilter` | `ComplementaryFilterTest` | Done |
| SENS-04 | Transient-movement rejection | Ch.3 §14 | `domain.measurement.TransientMotionDetector` | `MotionDetectorsTest` | Done |
| SENS-05 | Stable-posture qualification | Ch.3 §15 | `domain.measurement.StablePostureQualifier` | `MotionDetectorsTest` | Done |
| SENS-06 | Measurement quality state | Ch.3 §27 | `domain.measurement.MeasurementQuality` | — | Done |
| SENS-07 | Neck-flexion estimation (modular, disabled) | Ch.3 §20 | `domain.measurement.NeckFlexionEstimator` | — | Done — deliberately inert, see open-questions.md #1 |
| SENS-08 | Sensor availability check, accelerometer-only fallback | Ch.3 §4 | `sensors.SensorFrameworkEngine` | — | Done (untested: Android SensorManager glue) |
| SENS-09 | Timestamp-normalised interval validation, gap rejection | Ch.3 §5.3 | `sensors.SensorFrameworkEngine.processAccelerometerEvent` | — | Done (untested: Android glue) |
| SENS-10 | Startup stabilisation, orientation-transition pause | Ch.3 §13, §18 | `sensors.SensorFrameworkEngine` | — | Done (untested: Android glue) |
| SENS-11 | Centralised sensor-framework configuration | Ch.3 §34 | `config.SensorFrameworkConfig` | — | Done |

| CAL-01 | User profile (age/gender/height) collection + validation | Ch.4 §5 | `domain.calibration.UserProfile`, `UserProfileValidator` | `UserProfileValidatorTest` | Done |
| CAL-02 | Guided baseline capture, valid-samples-only accumulation | Ch.4 §9-11 | `domain.calibration.BaselineCaptureSession` | `BaselineCaptureSessionTest` | Done |
| CAL-03 | Baseline statistic calculation (mean/median/trimmed mean) | Ch.4 §12 | `domain.calibration.BaselineCalculator` | `BaselineCalculatorTest` | Done |
| CAL-04 | Baseline persistence, original-baseline immutability | Ch.4 §14, §24, §37 | `data.calibration.CalibrationRepository`, Room entities | — | Done (untested: Room glue) |
| CAL-05 | Reset operations (new assessment archives, delete-all clears) | Ch.4 §35 | `CalibrationRepository.startNewBaselineAssessment/deleteAllCalibrationData` | — | Done (untested: Room glue); Reset Settings/Reset Behavioural Progress not yet applicable — no settings or adaptation state exist yet |
| CAL-06 | Baseline/live-angle independence (no offset subtraction) | Ch.4 §15, §28-30 | Architectural — `BaselineProfile` is never read by any Ch.5-9 engine | — | Done by construction; revisit when those engines are implemented to confirm they stay that way |

| ANG-01 | Deterministic posture-zone classification | Ch.5 §5-11 | `domain.behaviour.PostureZone`, `AngleInterpretationEngine` | `AngleInterpretationEngineTest` | Done |
| ANG-02 | Non-linear angle-load mapping (reused from Ch.1/2 config) | Ch.5 §31 | `AngleInterpretationEngine` (uses `config.AngleLoadTable`) | `AngleInterpretationEngineTest` | Done — Ch.1 and Ch.5's copies of this table were checked and match (both already reflect the 20° revision); open-questions.md #4 resolved, no conflict found |
| ANG-03 | Zone-transition hysteresis (dead-band, angle itself untouched) | Ch.5 §44-45 | `domain.behaviour.ZoneTransitionGate` | `ZoneTransitionGateTest` | Done |
| ANG-04 | Full decimal-precision boundary handling, no gaps/overlaps | Ch.5 §42-43 | `PostureZone.classify` | `AngleInterpretationEngineTest` | Done |

| ACT-01 | Evidence-based activity classification, Unknown fallback | Ch.1 §12, Ch.2 §8-9, Ch.6 | `domain.behaviour.ActivityClassifier` | `ActivityClassifierTest` | Done (real Android UsageStatsManager signal gathering deferred to Session Management integration) |
| LOAD-01 | Dynamic Load Increment formula, timestamp-normalised | Ch.1 §15, Ch.2 §11 | `domain.behaviour.DynamicLoadEngine` | `DynamicLoadEngineTest` | Done — resolves open-questions.md #10 |
| LOAD-02 | Cumulative Load accumulation, zero floor | Ch.1 §16, Ch.8 Rule 7 | `domain.behaviour.CumulativeLoadEngine` | `CumulativeLoadEngineTest` | Done; session reset ownership belongs to Ch.11 |
| REC-01 | Recovery formula, continuous per-cycle evaluation (no min-duration gate) | Ch.8 §12-13, §23 | `domain.behaviour.RecoveryEngine` | `RecoveryEngineTest` | Done |
| REC-02 | Prompt Correction Signal +5 bonus, applied by Recovery not Alert Engine | Ch.8 Rule 8 | `RecoveryEngine.applyPromptCorrectionBonus` | `RecoveryEngineTest` | Done; signal emission is Ch.10's responsibility (not yet implemented) |
| SCORE-01 | Score formula, clamped, silent while suspended | Ch.9 | `domain.behaviour.ScoringEngine` | `ScoringEngineTest` | Done |

| ALERT-01 | Score-driven alert-level classification | Ch.10 §22, §26.1 | `domain.alert.AlertLevel` | `AlertLevelTest` | Done |
| ALERT-02 | Dismissal hides mascot without affecting load/score; never confused with correction | Ch.10 §29, §36, §49 | `domain.alert.AdaptiveAlertEngine` | `AdaptiveAlertEngineTest` | Done |
| ALERT-03 | Reappearance shows current level, cancels if corrected first | Ch.10 §30-31, §35 | `AdaptiveAlertEngine.onCycle` | `AdaptiveAlertEngineTest` | Done |
| ALERT-04 | Prompt Correction Signal (once per Full-Alert episode, within window) | Ch.10 §32.1 | `AdaptiveAlertEngine.onCycle` | `AdaptiveAlertEngineTest` | Done; consumed by `RecoveryEngine.applyPromptCorrectionBonus` — wiring the two together happens in the Integration pass |
| ALERT-05 | Fixed upper-left corner presentation, tap-to-dismiss | Ch.10 §38, §43 | `ui.mascot.MsAngleAngelOverlay` | — | Done (untested: Compose UI); full rigged animation deferred, see open-questions.md #11 |

| SESS-01 | Operational state machine (Init/Ready/Active/Paused/Completed) | Ch.11 §22-31 | `session.MonitoringState`, `session.SessionManager` | — | Done (untested: entangled with Android Context/SensorManager/Room; no Robolectric in this build) |
| SESS-02 | Module initialisation order, onboarding/permission gating | Ch.11 §34, §14 | `SessionManager.initialise` | — | Done (untested, as above) |
| SESS-03 | Full pipeline sequencing (Sensor→Angle→Activity→Load→Recovery→Score→Alert) per sample | Ch.11 §28, §36 | `SessionManager.onSample` | — | Done (untested, as above); this is the first point all Ch.5-10 engines are actually wired together |
| SESS-04 | Foreground service, notification, background continuity | Ch.3 §30, Ch.11 §18 | `session.MonitoringForegroundService` | — | Done (untested: Android Service glue) |
| SESS-05 | Automatic restart after device reboot, gated on existing onboarding | Ch.11 §19 | `session.BootCompletedReceiver` | — | Done (untested: Android BroadcastReceiver glue) |
| SESS-06 | Overlay/notification permission checks, graceful degradation | Ch.11 §14, Ch.2 §29 | `session.PermissionChecker` | — | Done (untested: Android permission APIs) |

| REP-01 | Session Summary generation from live cycle data (read-only, no recalculation) | Ch.12 §10-16 | `domain.reporting.SessionSummaryAccumulator`, `SessionSummary` | `SessionSummaryAccumulatorTest` | Done |
| REP-02 | Historical session persistence, append-only | Ch.12 §23 | `data.reporting.ReportingRepository`, `SessionSummaryEntity` | — | Done (untested: Room glue) |
| REP-03 | Alert-count / correction-event tracking | Ch.12 §16 | `AdaptiveAlertEngine` (`alertJustAppeared`/`postureJustCorrected`), consumed by the accumulator | `AdaptiveAlertEngineTest`, `SessionSummaryAccumulatorTest` | Done |
| REP-04 | Delete-all-data extends to session history | Ch.4 §35.4 | `ReportingRepository.deleteAllSessions` | — | Done (untested: Room glue) |

**Not yet built**: a presentation dashboard/history UI consuming `ReportingRepository.getAllSessions()`/`getRecentSessions()` — the data model and persistence are complete, but no screen renders them yet. Scoped into the Integration pass or a follow-up, not fabricated.

| ONB-01 | Welcome, privacy explanation, profile collection | Ch.11 §10-11, Functional Spec Sys.1 | `ui.onboarding.WelcomeScreen`, `ProfileScreen`, `OnboardingViewModel` | — | Done (untested: Compose UI) |
| ONB-02 | Permission explanation + real overlay/notification permission requests | Ch.11 §14 | `ui.onboarding.PermissionExplanationScreen` | — | Done (untested: Compose UI + Activity Result APIs) |
| ONB-03 | Guided baseline capture screen, live sensor-driven | Ch.4 §9-12, Ch.11 §12 | `ui.onboarding.GuidedCalibrationScreen`, `CalibrationCaptureController` | — | Done (untested: Android sensor glue) |
| ONB-04 | Full onboarding flow, one-time only | Ch.11 §10, §15, §20 | `ui.onboarding.OnboardingNavHost`, `MainActivity.AppRoot` | — | Done (untested: Compose Navigation); `MainActivity` checks stored profile+baseline on every launch and never re-shows onboarding once both exist |

**Not yet built**: a live monitoring dashboard bound to `SessionManager.cycleResults` (current score, zone, mascot overlay) — `MonitoringActiveScreen` starts the foreground service and confirms the pipeline runs end-to-end, but doesn't yet render live data or the Ch.10 mascot overlay on screen. Scoped into the Integration pass.
