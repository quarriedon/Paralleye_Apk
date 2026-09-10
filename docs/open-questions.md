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

## Build infrastructure (not a methodology question)

15. **Unstable debug-build signing key across CI runs, now fixed.** Found while investigating a
    user report that two already-pushed, CI-green fixes (the mascot mirror direction, the
    background overlay) still weren't visible after reinstalling. The project had no checked-in
    signing config, so debug builds fell back to AGP's default: generate
    `~/.android/debug.keystore` with a fresh random key if one doesn't already exist. Every
    GitHub Actions run is a fresh, ephemeral runner with no such file, so every CI build was
    signed with a *different* random key. Sideloading a new debug APK over an install with a
    different signature silently fails — Android just refuses the update — leaving whatever was
    already installed untouched. From the outside that looks exactly like "the fix isn't
    landing," run after run, with no error surfaced anywhere in this conversation to catch it.
    Fixed by generating `app/debug.keystore` once (same conventions AGP's own default uses --
    zero security value, never used for release) and pointing `buildTypes.debug` at it
    explicitly, so every future CI build is signed identically and updates install in place.
    The build that introduced this fix still needs one full uninstall to get onto the new
    stable key; nothing after it should.

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

    Separately: every `ms_angle_angel_*.png` frame has a full phone-bezel mockup illustration
    baked into it (the source art shows "how it'd look on an iPhone" rather than being a
    transparent, screen-ready sprite). Confirmed as leftover concept/presentation art, not
    anything Ch.10 specifies — the app itself already is the phone, so this draws a
    redundant phone-in-a-phone. Left in place (a pixel-crop of production art risked
    cutting into the character or the peel edge without a proper alpha-matted source);
    the real fix is requesting a transparent-background export of the same six frames from
    whoever produced the art. Not the same bug as the mirror-direction fix — see traceability
    ALERT-05 for that.

## Built since the gaps below were first flagged

12. **Settings screen (Functional Spec System 11) — now built** (`ui.settings.SettingsScreen`,
    `data.settings.SettingsRepository`). Covers sensitivity (Low/Medium/High, persisted via
    DataStore and now actually read by `SessionManager` instead of the old hardcoded
    `MEDIUM`), a read-only profile summary, and delete-all-data (behind a confirmation
    dialog) wired to the reset operations Ch.4 §35 already specified. Still missing:
    notification preferences and permission status — neither is specified anywhere beyond
    "should exist" in the Functional Spec, so not guessed at rather than left out.

13. **History dashboard (Functional Spec System 10) — now built** (`ui.history.HistoryScreen`).
    Lists past sessions (most recent first) with score stats, zone-duration breakdown and
    alert/correction counts via `ReportingRepository.getRecentSessions`. Still missing:
    daily/weekly/monthly rollups and baseline-comparison views — the Functional Spec doesn't
    specify their exact shape, so a flat session list ships now rather than a guessed layout.

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

14. **OEM battery-manager whitelisting, beyond what code can request.** Fixing the reported
    "monitoring stops when backgrounded" bug required three things: `MascotOverlayController`
    (the mascot was never actually drawn outside the app — see traceability.md SESS-07, by far
    the biggest part of what the user was seeing), a held `PARTIAL_WAKE_LOCK` so non-wakeup
    accelerometer/gyroscope events keep arriving with the screen off (SESS-04), and a
    Settings-screen prompt for the standard Android
    `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` exemption (SET-01). None of this is
    verifiable from this sandbox (no physical device). What remains genuinely open: Samsung
    (the device from the earlier crash report), Xiaomi, Huawei and others layer a *second*,
    OEM-specific battery manager on top of stock Android's — the standard exemption dialog
    above does not reach it, and a user sometimes also has to manually add the app to a
    device-specific "never sleeping apps"/"auto-start" allowlist in the OEM's own settings
    (not the stock Android battery settings) for a foreground service to reliably survive
    hours in the background. There's no single API to request this across OEMs. Needs
    physical-device confirmation on the user's own Samsung device: check Settings → Apps →
    Paralleye → Battery, and separately Samsung's device-care "Sleeping apps"/"Never sleeping
    apps" list, after granting the in-app exemption above. The held wake lock is also a real,
    disclosed battery-life cost for the duration of every monitoring session — not something
    the source documents settle either way, so flagged here rather than silently accepted.

## Engineering interpretation (not a disputed methodology value)

5. **Angle-load table lookup granularity.** The table in Ch.1 §11 is authored as whole-degree
   bands with integer gaps between them (e.g. "0°–20°" then "21°–25°"), which has no band for
   a continuous value like 20.4°. `AngleLoadTable.loadFor()` floors the input angle to whole
   degrees before lookup. This is a technical implementation detail of applying an
   integer-authored table to continuous sensor output, not a resolution of a disputed
   methodology number — flagged here for visibility, not because it needs sign-off.
