# Paralleye

A native Android app (Kotlin, minSdk 26) that passively monitors smartphone posture using
motion sensors only — no camera, microphone, or facial recognition — built against the
PARALLEYE Technical Methodology (12 chapters) and Functional Specification in
`docs/source-materials/`.

## Status

All 12 methodology chapters plus onboarding are implemented and wired together end to end:
onboarding → guided calibration → foreground-service monitoring → the full behavioural
pipeline (Sensor Framework → Angle Interpretation → Activity → Dynamic Load → Recovery →
Scoring → Adaptive Alert) → session reporting. See `docs/traceability.md` for a
requirement-by-requirement breakdown of what's implemented, and `docs/open-questions.md` for
methodology points flagged as unresolved, deferred, or needing physical-device confirmation.

## Getting an APK

This project's sandbox build environment can't reach `dl.google.com`, so the Android SDK
can't be resolved locally there. Two ways to actually get an APK:

1. **CI (no local setup needed):** every push to `main` triggers `.github/workflows/build.yml`,
   which builds a debug APK on a GitHub-hosted runner and uploads it as an artifact. Go to the
   [Actions tab](../../actions), open the latest successful "Build debug APK" run, and download
   `paralleye-debug-apk` from the run's artifacts.
2. **Locally (e.g. on a Mac with Android Studio):**
   ```
   ./gradlew assembleDebug
   ```
   The APK lands at `app/build/outputs/apk/debug/app-debug.apk`.

## What to test on a real device

- **Onboarding → calibration → monitoring** end to end from a fresh install.
- **Device-angle convention** (`docs/open-questions.md` #6): hold the phone upright — the
  angle should read near 0° — then tilt it toward flat/horizontal and confirm it increases
  toward ~90°, in portrait and both landscape orientations. This was resolved by reasoning
  about the source formula's contradiction with its own stated behaviour, not by testing on
  hardware, so it's the single highest-priority thing to verify.
- **Mascot corner-peel behaviour**: Ms Angle Angel should always emerge from the upper-left
  corner, at increasing visibility as posture score drops, and never move elsewhere on screen.
- **Background monitoring**: posture load should keep accumulating with the app backgrounded
  or the screen off, and the foreground-service notification should stay present.
- **Permissions**: denying overlay or notification permission during onboarding should be
  handled gracefully, not crash.

## Project layout

- `app/src/main/java/sg/paralleye/config/` — the central versioned parameter configuration
  every engine reads from (Ch.2 §23).
- `app/src/main/java/sg/paralleye/domain/` — pure, unit-tested behavioural logic, organised by
  methodology domain (`measurement`, `calibration`, `behaviour`, `alert`, `reporting`).
- `app/src/main/java/sg/paralleye/sensors/`, `session/` — the Android-framework glue
  (SensorManager, foreground service, permissions) that the pure domain logic doesn't depend on.
- `app/src/main/java/sg/paralleye/ui/` — Compose screens (onboarding, the mascot overlay).
- `app/src/main/java/sg/paralleye/data/` — Room persistence.
- `docs/source-materials/` — the original PARALLEYE spec documents this build is based on.
- `docs/traceability.md`, `docs/open-questions.md` — living requirement-traceability and
  open-methodology-question registers, updated alongside the code rather than after the fact.

## Running tests

```
./gradlew testDebugUnitTest
```

Pure domain logic (angle calculation, filtering, all six behavioural engines, the alert state
machine, session summary accumulation) is unit-tested without the Android framework. Code that
directly wraps Android APIs (SensorManager, Room, foreground services, Compose UI) is not —
that tier of testing needs a physical device or emulator, which this build environment doesn't
have access to.
