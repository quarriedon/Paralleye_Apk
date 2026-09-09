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

Rows for the Sensor Framework, Calibration, Angle Interpretation, Activity, Dynamic Load,
Recovery, Scoring, Adaptive Alert, Session Management, Reporting and Onboarding modules are
added as each is implemented in later commits.
