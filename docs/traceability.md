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

Rows for Calibration, Angle Interpretation, Activity, Dynamic Load, Recovery, Scoring,
Adaptive Alert, Session Management, Reporting and Onboarding modules are added as each is
implemented in later commits.
