# PARALLEYE — Requirements Traceability

Per Technical Methodology Ch.2 §37. Updated as each module is implemented — not filled in
speculatively ahead of the code existing.

| Req ID | Description | Source | Component | Test | Status |
|---|---|---|---|---|---|
| GOV-01 | Central versioned parameter configuration | Ch.2 §23, §24 | `config.ParallayeParameters`, `config.ParameterVersion` | `ConfigValidatorTest` (indirect) | Done |
| GOV-02 | Configuration validation before monitoring begins | Ch.2 §35 | `config.ConfigValidator` | `ConfigValidatorTest` | Done |
| GOV-03 | Structured cycle logging, privacy-redacted | Ch.2 §13, §33 | `logging.CycleRecord`, `logging.ParallayeLogger` | — | Done (untested: thin Android `Log` wrapper) |
| GOV-04 | Non-linear angle-load mapping table | Ch.1 §11 | `config.AngleLoadTable` | `ConfigValidatorTest.angle-load lookup` | Done |
| GOV-05 | Activity multiplier set selection (Patent Document values active) | Ch.1 §14, Ch.6 | `config.ActivityMultipliers`, used by `ActivityClassifier`/`SessionManager` | — | Done — wired in SessionManager.onSample |
| GOV-06 | Recovery formula + Prompt Correction Signal parameters | Ch.1 §18, Ch.8 | `config.RecoveryConfig`, used by `RecoveryEngine` | `RecoveryEngineTest` | Done |
| GOV-07 | Score formula parameters | Ch.1 §19, Ch.9 | `config.ScoreConfig`, used by `ScoringEngine` | `ScoringEngineTest` | Done |
| GOV-08 | Alert-state score ranges | Ch.10 | `config.AlertStateRanges`, used by `AlertLevel` | `AlertLevelTest` | Done |
| GOV-09 | Sensitivity presets (Low/Medium/High) | Ch.2 §25 | `config.SensitivityAdjustment`, used by `SessionManager` for reappearance timing | — | Now user-facing: `ui.settings.SettingsScreen` writes the choice through `data.settings.SettingsRepository` (DataStore — the dependency existed unused since scaffold), `SessionManager` collects it live and uses it in place of the previous hard-coded `MEDIUM`. Still only affects Ch.10 reappearance interval; `accumulationMultiplier` and `alertQualificationSeconds` remain unused, same gap as before |

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

| ACT-01 | Evidence-based activity classification, Unknown fallback | Ch.1 §12, Ch.2 §8-9, Ch.6 | `domain.behaviour.ActivityClassifier` | `ActivityClassifierTest` | Classifier done; wired into `SessionManager.onSample` but fed a permanently-empty observation (keyboard/foreground-app signals not gathered), so every real cycle currently classifies Unknown — spec-compliant fallback (Ch.2 §9), not fabricated, but real UsageStatsManager/keyboard-state gathering is a follow-up, not yet built |
| LOAD-01 | Dynamic Load Increment formula, timestamp-normalised | Ch.1 §15, Ch.2 §11 | `domain.behaviour.DynamicLoadEngine` | `DynamicLoadEngineTest` | Done — resolves open-questions.md #10 |
| LOAD-02 | Cumulative Load accumulation, zero floor | Ch.1 §16, Ch.8 Rule 7 | `domain.behaviour.CumulativeLoadEngine` | `CumulativeLoadEngineTest` | Done; one instance per `SessionManager`, naturally resets when a new session starts, preserved across pause/resume within a session per Ch.11 §26-27 |
| REC-01 | Recovery formula, continuous per-cycle evaluation (no min-duration gate) | Ch.8 §12-13, §23 | `domain.behaviour.RecoveryEngine` | `RecoveryEngineTest` | Fixed real-device bug: `calculateRecovery` had no interval normalisation, unlike `DynamicLoadEngine`'s Ch.2 §11 "Time and Frame Independence Principle" handling for load. At the 50Hz default sampling rate this applied a full `rate`-sized recovery on every ~20ms sample instead of per intended interval, recovering the whole score in a few seconds — much faster than load accumulates. `calculateRecovery` now takes `actualIntervalMillis`/`intendedIntervalMillis` and scales `rate` the same way `DynamicLoadEngine` scales `frameFactor`, so both sides of the load/recovery balance share one time base |
| REC-02 | Prompt Correction Signal +5 bonus, applied by Recovery not Alert Engine | Ch.8 Rule 8 | `RecoveryEngine.applyPromptCorrectionBonus` | `RecoveryEngineTest` | Done; signal emission is Ch.10's responsibility (not yet implemented) |
| SCORE-01 | Score formula, clamped, silent while suspended | Ch.9 | `domain.behaviour.ScoringEngine` | `ScoringEngineTest` | Done |

| ALERT-01 | Score-driven alert-level classification | Ch.10 §22, §26.1 | `domain.alert.AlertLevel` | `AlertLevelTest` | Done |
| ALERT-02 | Dismissal hides mascot without affecting load/score; never confused with correction | Ch.10 §29, §36, §49 | `domain.alert.AdaptiveAlertEngine` | `AdaptiveAlertEngineTest` | Done |
| ALERT-03 | Reappearance shows current level, cancels if corrected first | Ch.10 §30-31, §35 | `AdaptiveAlertEngine.onCycle` | `AdaptiveAlertEngineTest` | Done |
| ALERT-04 | Prompt Correction Signal (once per Full-Alert episode, within window) | Ch.10 §32.1 | `AdaptiveAlertEngine.onCycle` | `AdaptiveAlertEngineTest` | Done — wired to `RecoveryEngine.applyPromptCorrectionBonus` in `SessionManager.onSample` |
| ALERT-05 | Fixed upper-left corner presentation, tap-to-dismiss | Ch.10 §38, §43 | `ui.mascot.MsAngleAngelOverlay` | — | Done (untested: Compose UI); full rigged animation deferred, see open-questions.md #11. Fixed real-device bug: the shipped `drawable-nodpi/ms_angle_angel_*.png` frames were the pre-mirror originals (confirmed by pixel analysis — 60-88% of character content in the right half, matching the README's own description of the uncorrected assets), not the horizontally-mirrored "corrected" frames `docs/source-materials/Angle Angel/README_FOR_CLAUDE_CODE.txt` says should ship, so she read as emerging from the top-right despite `MsAngleAngelOverlay`'s anchor being correctly top-left all along. Re-mirrored all five frames in place. The phone-bezel mockup frame baked into every asset is unchanged — confirmed leftover concept/presentation art, not part of Ch.10's spec, left in place pending real transparent-background assets from the source (see open-questions.md #11) |

| SESS-01 | Operational state machine (Init/Ready/Active/Paused/Completed) | Ch.11 §22-31 | `session.MonitoringState`, `session.SessionManager` | — | Done (untested: entangled with Android Context/SensorManager/Room; no Robolectric in this build) |
| SESS-02 | Module initialisation order, onboarding/permission gating | Ch.11 §34, §14 | `SessionManager.initialise` | — | Done (untested, as above) |
| SESS-03 | Full pipeline sequencing (Sensor→Angle→Activity→Load→Recovery→Score→Alert) per sample | Ch.11 §28, §36 | `SessionManager.onSample` | — | Done (untested, as above); this is the first point all Ch.5-10 engines are actually wired together |
| SESS-04 | Foreground service, notification, background continuity | Ch.3 §30, Ch.11 §18 | `session.MonitoringForegroundService` | — | Fixed after real crash on device (Android 16): `foregroundServiceType="health"` requires a body-sensor/Health-Connect permission this app has no reason to hold, and threw `SecurityException` on `startForeground()`. Switched to `"specialUse"` (Android's category for FGS work that doesn't fit a predefined type) plus the required subtype `<property>`. `startForeground()` is now wrapped so a future FGS mismatch fails predictably (Ch.2 §34) instead of crashing the process. |
| SESS-05 | Automatic restart after device reboot, gated on existing onboarding | Ch.11 §19 | `session.BootCompletedReceiver` | — | Done (untested: Android BroadcastReceiver glue) |
| SESS-06 | Overlay/notification permission checks, graceful degradation | Ch.11 §14, Ch.2 §29 | `session.PermissionChecker` | — | Done (untested: Android permission APIs) |

| REP-01 | Session Summary generation from live cycle data (read-only, no recalculation) | Ch.12 §10-16 | `domain.reporting.SessionSummaryAccumulator`, `SessionSummary` | `SessionSummaryAccumulatorTest` | Done |
| REP-02 | Historical session persistence, append-only | Ch.12 §23 | `data.reporting.ReportingRepository`, `SessionSummaryEntity` | — | Done (untested: Room glue) |
| REP-03 | Alert-count / correction-event tracking | Ch.12 §16 | `AdaptiveAlertEngine` (`alertJustAppeared`/`postureJustCorrected`), consumed by the accumulator | `AdaptiveAlertEngineTest`, `SessionSummaryAccumulatorTest` | Done |
| REP-04 | Delete-all-data extends to session history | Ch.4 §35.4 | `ReportingRepository.deleteAllSessions` | — | Done (untested: Room glue) |
| REP-05 | History dashboard, past session summaries | Functional Spec Sys.10, Ch.12 §12-16 | `ui.history.HistoryScreen` | — | Done (untested: Compose UI). Read-only list of `SessionSummary` via `ReportingRepository.getRecentSessions`; per-session score stats, zone-duration breakdown, alert/correction counts. Nothing recalculated |

| SET-01 | Settings screen (sensitivity, profile summary, delete-all-data) | Functional Spec Sys.11, Ch.2 §25, Ch.4 §35.4 | `ui.settings.SettingsScreen`, `data.settings.SettingsRepository` | — | Done (untested: Compose UI). Only exposes what Ch.2 §25 actually allows the user to adjust (sensitivity) plus the already-specified delete-all-data action (Ch.4 §35.4, with a confirmation dialog since it's destructive); no undocumented config constant is surfaced |

| ONB-01 | Welcome, privacy explanation, profile collection | Ch.11 §10-11, Functional Spec Sys.1 | `ui.onboarding.WelcomeScreen`, `ProfileScreen`, `OnboardingViewModel` | — | Done (untested: Compose UI) |
| ONB-02 | Permission explanation + real overlay/notification permission requests | Ch.11 §14 | `ui.onboarding.PermissionExplanationScreen` | — | Done (untested: Compose UI + Activity Result APIs) |
| ONB-03 | Guided baseline capture screen, live sensor-driven | Ch.4 §9-12, Ch.11 §12 | `ui.onboarding.GuidedCalibrationScreen`, `CalibrationCaptureController` | — | Done (untested: Android sensor glue) |
| ONB-04 | Full onboarding flow, one-time only | Ch.11 §10, §15, §20 | `ui.onboarding.OnboardingNavHost`, `MainActivity.AppRoot` | — | Done (untested: Compose Navigation); `MainActivity` checks stored profile+baseline on every launch and never re-shows onboarding once both exist |

| ONB-05 | Live monitoring screen bound to the running session (score, zone, mascot overlay) | Ch.10 §12, Ch.12 §2 | `MainActivity.MonitoringActiveScreen`, `session.SessionManagerHolder` | — | Done (untested: Compose UI). `SessionManagerHolder` makes the foreground service and the UI observe one shared `SessionManager` instance rather than each creating their own, per Ch.2 §14 |
| ONB-06 | Top-level navigation between Monitor/History/Settings once onboarded | Functional Spec Sys.9-11 | `MainActivity.MainTabs` | — | Done (untested: Compose UI). A flat 3-tab bottom-nav switch, not a full `NavHost` back-stack graph — these are peer top-level screens, not a sequential flow (that's what `OnboardingNavHost` is for) |
