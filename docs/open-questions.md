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

## Flagged inconsistency — needs an authored answer, not an engineering guess

4. **Angle-Load table vs. Posture Zone boundary revision.** Ch.1's revision note says the
   Green/Yellow boundary moved 15°→20° "matching the pilot-informed update recorded in
   Chapter 5", and Chapter 1's own copy of the angle-load table already reflects this (first
   band is 0°-20°). Whether Chapter 5's independently-authored copy of the same table was
   actually updated to match needs to be checked against Chapter 5 directly when the Angle
   Interpretation Engine is implemented — if it still shows a 15° cut, that's a genuine
   unresolved conflict between two chapters' stated values, not something to silently pick a
   side on.

## Engineering interpretation (not a disputed methodology value)

5. **Angle-load table lookup granularity.** The table in Ch.1 §11 is authored as whole-degree
   bands with integer gaps between them (e.g. "0°–20°" then "21°–25°"), which has no band for
   a continuous value like 20.4°. `AngleLoadTable.loadFor()` floors the input angle to whole
   degrees before lookup. This is a technical implementation detail of applying an
   integer-authored table to continuous sensor output, not a resolution of a disputed
   methodology number — flagged here for visibility, not because it needs sign-off.
