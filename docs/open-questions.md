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

## Client instruction overriding written spec (recorded, not silent)

16. **Mascot anchored right, not left; phone-bezel mockup removed from the actual assets.**
    Ch.10 §38 is unambiguous: "Ms Angle Angel shall always emerge from the upper-left corner
    of the active application screen... shall not appear from the right side." The client
    separately commissioned a full build of this same app from a different, 4-developer team,
    and shared screenshots of that build directly (in-app, on the home screen, and as a
    system overlay drawn on top of a running YouTube video) showing the mascot anchored to
    the **right** edge of the screen every time, with a clean transparent character cutout
    and no phone-shaped frame around her. On explicit instruction ("I need to replicate this
    in our build"), `MsAngleAngelOverlay` and `MascotOverlayController` were both moved to
    `Alignment.TopEnd`/`Gravity.END`. This directly contradicts Ch.10 §38's text — flagged
    here per Ch.2 §3 rather than silently changed, since the written methodology and the
    client's own reference implementation now disagree and only the client can settle which
    one is authoritative going forward. Also cropped the phone-bezel mockup out of the three
    actually-used frames (`ms_angle_angel_03/04/06`) and added alpha transparency via a
    distance-from-background-color threshold, since the source PNGs were fully opaque with no
    alpha channel. This is a semi-automated approximation, not real alpha-matted source art —
    edges (especially hair) may show minor fringing up close, and it was only verified by
    rendering each frame against a solid test background and inspecting the result, not on an
    actual device. If the client's reference app's actual asset files are ever available,
    those should replace this approximation rather than refining it further by hand.

    **Follow-up round, same root cause pattern.** Device testing confirmed the right-edge
    anchor fix worked, but found two more real bugs in the same crop/transparency approach:
    (a) the color-distance alpha threshold left the peel-paper's shadow gradient partially
    opaque — it faded but never fully disappeared, reading as a faint ghost shape. Switched to
    a border-flood-fill approach instead (`scipy.ndimage.label`): only background pixels
    *connected to the image's outer edge* become transparent, with a short geometric distance
    ramp (not a color-distance ramp) for anti-aliasing at the true silhouette edge. This also
    fixes a related failure mode the naive color/saturation approach hit first — treating eye
    whites and teeth as "background" because they're pale and desaturated too, which a pure
    global-color threshold can't distinguish from the real background but a border-connectivity
    check can (they're enclosed by the face, not touching the image edge). (b) the crop was
    re-done tighter (character-only bounding box, no more peel-curl padding) — which
    *narrowed* each frame's aspect ratio, and since both renderers previously forced every
    frame into a fixed **square** box, the narrower image actually rendered smaller than
    before despite the tighter crop, reading as "very tiny." Both `MsAngleAngelOverlay` (now
    a fixed height with width following each frame's own intrinsic aspect ratio) and
    `MascotOverlayController` (now a fixed 85x130dp box, close to but not exactly matching
    each frame's aspect, since a WindowManager window can't cheaply auto-follow aspect ratio
    the way Compose can without resizing it on every frame change) were changed to stop
    forcing a square.

    "Background monitoring doesn't work when minimized" was also reported again after this
    round, for the third time, following a real fix each of the first two times (the FGS-type
    crash, then the missing system overlay). Rather than guess a third specific cause blind,
    added `session.OverlayDiagnosticLog` — a small on-device trail (readable in Settings, or
    at `Android/data/sg.paralleye/files/diagnostics/overlay_diagnostics.txt` via a file
    manager) recording activity start/stop transitions, `initialise()`'s outcome, wake lock
    acquisition, and every app-visibility/mascot-visibility state change the overlay decision
    logic sees — including whether the service's `onDestroy()` ever runs (if the process is
    killed outright rather than stopped gracefully, the file simply stops mid-stream, which is
    itself diagnostic). This sandbox has no physical device to reproduce this on; the next
    report on this specific issue should come with this file's contents rather than another
    guess from either side.

    **Resolved on the first real trace.** The client's exported diagnostic log showed the
    actual cause conclusively, no guessing required: every `WindowManager.addView()` call in
    `MascotOverlayController.createView()` was throwing `RuntimeException: Can't create
    handler inside thread [DefaultDispatcher-worker-N] that has not called Looper.prepare()`
    — repeatedly, on every single attempt, for the entire time this feature has existed.
    `MonitoringForegroundService` drives the overlay from its `Dispatchers.Default` coroutine
    scope (a background thread pool); `WindowManager` and View operations require a thread
    with a prepared `Looper`, which in practice means the main thread. The exception was being
    caught by the controller's own `runCatching` (added defensively, without realizing it
    would end up hiding the actual bug) and logged only to Logcat, invisible without adb — so
    the background overlay had silently never worked, not once, through the FGS-type-crash fix,
    the "add the overlay at all" fix, and the peel/sizing fixes, all of which were real and
    necessary but none of which touched the actual blocker. Fixed by posting every view
    mutation in `MascotOverlayController` through `Handler(Looper.getMainLooper())`, so the
    class is safe to drive from any thread. This is the concrete payoff of adding
    `OverlayDiagnosticLog` instead of guessing a fourth time.

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

   Update after a device-testing report of "~5-6° while held vertical, ~80° while flat on a
   table" (expected 0°/90°): re-derived `DeviceAngleCalculator.calculateAngleDegrees` by hand
   for ideal inputs — `arccos(remapped.y/magnitude)` gives exactly 0.0° for `(x=0,y=+9.80665,
   z=0)` and exactly 90.0° for `(x=0,y=0,z=+9.80665)` — so the formula itself is not the bug;
   a 5-6° reading is what a genuine ~5-6° physical tilt produces (`cos(5.5°) ≈ 0.9954`), and
   nobody holds a phone at a mathematically perfect 90°/0° by feel. Separately, found and
   fixed a real, confirmed bug while investigating: `SensorFrameworkEngine.
   onScreenOrientationChanged` (and `ScreenOrientation.fromSurfaceRotation`, which converts
   Android's `Display.getRotation()` into the enum this class expects) were both written and
   unit-tested but never called by anything — `screenOrientation` was permanently stuck at
   `PORTRAIT` regardless of how the phone was actually held, so any sample taken while
   rotated used the wrong axis remap (see SENS-10 in traceability.md). Now wired via a
   `DisplayManager.DisplayListener` in `SessionManager`. This would explain large,
   orientation-correlated errors, not a flat 5-10° offset in portrait. See item 17 below for
   what a flat, orientation-independent offset most likely is instead.

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

17. **Accelerometer zero-offset/bias, no calibration step.** After ruling out the formula
    (item 6, verified correct for ideal input) and fixing the orientation-wiring bug above,
    a flat-on-a-table reading of ~80° instead of ~90° (and a small vertical offset) that
    persists on a *specific physical device* in a *fixed orientation* is most consistent with
    ordinary commodity accelerometer bias — consumer MEMS accelerometers commonly read a few
    degrees off true without per-device calibration, which is exactly why most tilt-sensing
    apps ship a "lay flat and tap to zero" or "hold upright and tap to zero" calibration step.
    Nothing in the source documents describes such a step, and none exists in this app —
    `CalibrationRepository`'s "baseline" (Ch.4) is the user's neck/posture profile from
    onboarding, not a sensor zero-offset. Added raw accelerometer x/y/z plus
    `screenOrientation` to the throttled `OverlayDiagnosticLog` cycle line (`SessionManager.
    onSample`) so a future report can be checked against actual raw sensor values instead of
    guessed at blind. Building a real zero-offset calibration feature would be a genuine new
    feature beyond what's specified, not a bug fix — flagged here rather than built
    speculatively; needs a client decision before implementing.

5. **Angle-load table lookup granularity.** The table in Ch.1 §11 is authored as whole-degree
   bands with integer gaps between them (e.g. "0°–20°" then "21°–25°"), which has no band for
   a continuous value like 20.4°. `AngleLoadTable.loadFor()` floors the input angle to whole
   degrees before lookup. This is a technical implementation detail of applying an
   integer-authored table to continuous sensor output, not a resolution of a disputed
   methodology number — flagged here for visibility, not because it needs sign-off.
