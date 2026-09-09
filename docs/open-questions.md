# PARALLEYE — Open Methodology Questions

Per Technical Methodology Ch.2 §26 ("No Hidden Placeholder Principle"): where a required
formula or parameter is unresolved in the source documents, the implementation may build the
component architecture and use a clearly labelled temporary value, but the placeholder must be
listed here and the UI must never imply scientific validation where none exists.

## Unresolved — implemented as disabled/configurable, not guessed

1. **Device-angle → neck-flexion transformation.** Ch.3/4/5 leave the exact formula, the
   reference convention (θ vs 90°−θ), and the coefficient (0.4 vs 0.5) unresolved, and
   explicitly say to keep this modular/disabled rather than invent a value. Implemented as
   `ParallayeParameters.neckFlexionTransform: NeckFlexionTransformConfig?`, defaulted to
   `null` (not computed). No engine reads this field yet.

2. **Sensor-fusion method.** Ch.3 does not specify complementary filter vs. Kalman filter, nor
   the tuning coefficients. Implementation choice documented at the point of implementation
   (Sensor Framework module) rather than here, since it's a technical means to a specified
   end (a stable device angle), not a disputed methodology value.

3. **Baseline statistical method.** Ch.4 leaves mean / median / trimmed-mean undecided.
   Defaulted to mean (`CalibrationConfig.baselineStatistic`), marked
   `ENGINEERING_DEFAULT_UNVALIDATED`, swappable via config.

## Resolved

10. **Frame-based vs. timestamp-normalised load accumulation (Ch.7 vs Ch.2 §11).** Ch.7's
    literal formula accumulates a fixed frame factor per cycle with no explicit time term,
    while Ch.2 §11 separately mandates that the frame factor "must not assume every callback
    represents an identical duration" and should be scaled relative to the intended sampling
    rate. `DynamicLoadEngine.calculateIncrement` scales the configured frame factor by
    `actualIntervalMillis / intendedIntervalMillis` before use — at the intended sampling rate
    this is numerically identical to Ch.7's literal formula, and at any other effective rate
    it satisfies Ch.2 §11's explicit instruction. Not a disputed methodology number: Ch.2 §11
    itself describes exactly this technique as an acceptable robust implementation.

## Checked, no conflict found

4. **Angle-Load table vs. Posture Zone boundary revision.** Ch.1's revision note says the
   Green/Yellow boundary moved 15°→20°. Checked directly against Chapter 5 §41-§42 (Angle
   Interpretation Engine implementation): Chapter 5's own copies of both the posture-zone
   table (Green 0°-20°, Yellow 21°-25°, Red 26°+) and the angle-load table (identical bands
   and values to Ch.1 §11) already reflect the revised boundary. No conflict — both chapters
   agree. (An earlier research pass flagged this as unresolved without checking Ch.5 directly;
   corrected here after reading Ch.5 in full.)

## Deliberately scoped out of this MVP build (not a silent omission)

8. **Passive observation (Ch.4 §7.2, Model 2's second half).** Ch.4 §7.1 selects "guided
   calibration followed by passive observation" as the approved model, but §7.2 explicitly
   allows falling back to "the guided baseline with reduced confidence" while supplementary
   data is still pending. This build implements guided baseline capture only
   (`BaselineCaptureSession`) — background passive observation across days, activity-specific
   baselines (§21), and portrait/landscape usage baselines (§22) are not implemented. The
   `BaselineProfile` data class only carries the fields a guided-only capture actually
   populates, rather than adding always-null fields for the unimplemented parts (per §13's own
   instruction not to populate fields with fabricated values). Flagged for a follow-up build,
   not silently dropped.

9. **Behavioural Adaptation State (Ch.4 §25-26).** Ch.4 §26 explicitly permits a "basic
   approved adaptation strategy" for the first build: store the baseline, use fixed load/score
   calculations, use the baseline only for progress comparison, keep sensitivity presets. This
   build does exactly that — no `BehaviourAdaptationState` progressive-goal structure exists
   yet, consistent with §26's own MVP allowance rather than the more advanced adaptation the
   chapter describes as a later increment.

## Deferred UI polish (not fabricated, not yet built)

11. **Full corner-peel frame animation (Ch.10 §25, §40, character/animation specs).** The
    character spec describes a rigged, 30fps, ease-in-out animated corner-peel entrance with
    inter-frame tweening. Ch.10 §25/§40 explicitly mark the peel *animation* as optional (the
    fixed corner *location* and progressive emergence are mandatory, and are implemented).
    This build maps each `AlertLevel` to one settled static frame from the provided asset
    pack (Peek→frame 03, Peel→frame 04, Full Alert→frame 06) and crossfades between them
    (`MsAngleAngelOverlay`), rather than a full custom-rigged animation — a reasonable MVP
    reading of §40's explicit optionality, not a silent shortcut. Upgrading to true
    per-frame/Lottie animation is a follow-up, not a correctness gap.

## Not yet built (Functional Spec systems)

12. **Settings screen (Functional Spec System 11).** Sensitivity levels
    (Low/Medium/High), notification preferences, permission status, privacy info, and the
    data-reset actions (`CalibrationRepository`/`ReportingRepository` already implement the
    underlying reset/delete operations — Ch.4 §35) all lack a UI. Sensitivity is currently
    hardcoded to `MEDIUM` in `SessionManager`. Not fabricated as done anywhere — flagged here
    as the clearest remaining gap against the Functional Spec.

13. **History dashboard (Functional Spec System 10).** Daily/weekly/monthly summaries and
    baseline comparison views have no UI — `ReportingRepository.getAllSessions()`/
    `getRecentSessions()` return the stored data, but nothing renders it yet.

## Needs physical-device confirmation

6. **Device-angle axis convention (Ch.3 §7-§10).** The source formula θ = arccos(Z/|v|),
   taken with Android's standard accelerometer frame (Z = out of the screen face), reads
   ≈90° when the phone is held vertically and ≈0° flat on a table — the opposite of the
   spec's own stated anchors ("phone held vertically corresponds to 0°... increases as the
   phone tilts backward toward horizontal"). `DeviceAngleCalculator` instead computes the
   angle from the accelerometer's Y component (up-the-screen axis) after remapping for
   screen orientation, which does satisfy both stated anchors. This is documented in the
   class's KDoc and must be confirmed against the Ch.3 §8 physical test positions (vertical,
   mild tilt, ~45°, near-horizontal, portrait/landscape-left/landscape-right) on a real
   device — sideload the build and check that holding the phone upright reads near 0° and
   tilting it flat increases toward ~90°, in all three supported orientations.

7. **Gyroscope-to-angle-change approximation.** The complementary filter's "gyroscope
   change" term is approximated as the integrated angular velocity around the remapped X
   axis (`SensorFrameworkEngine.computeGyroChangeDegrees`) rather than a full 3D rotation
   integration, since the source documents only describe a simplified 1D conceptual filter.
   This is a reasonable approximation for a single-axis tilt estimate but should be checked
   for drift/inaccuracy during extended physical-device testing (Ch.3 §35.4).

## Engineering interpretation (not a disputed methodology value)

5. **Angle-load table lookup granularity.** The table in Ch.1 §11 is authored as whole-degree
   bands with integer gaps between them (e.g. "0°–20°" then "21°–25°"), which has no band for
   a continuous value like 20.4°. `AngleLoadTable.loadFor()` floors the input angle to whole
   degrees before lookup. This is a technical implementation detail of applying an
   integer-authored table to continuous sensor output, not a resolution of a disputed
   methodology number — flagged here for visibility, not because it needs sign-off.
