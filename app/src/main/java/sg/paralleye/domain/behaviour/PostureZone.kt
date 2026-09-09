package sg.paralleye.domain.behaviour

import sg.paralleye.config.ZoneThresholds

/** Ch.5 §26-28. */
enum class PostureZone {
    GREEN,
    YELLOW,
    RED;

    companion object {
        /**
         * Ch.5 §11 "Deterministic Processing Principle": pure function of the angle alone.
         * Ch.5 §42-43: full decimal precision, no gaps/overlaps — 20.0° is Green, 20.1° is
         * Yellow, 25.0° is Yellow, 25.1° is Red.
         */
        fun classify(angleDegrees: Double, thresholds: ZoneThresholds): PostureZone = when {
            angleDegrees <= thresholds.greenMaxDegrees -> GREEN
            angleDegrees <= thresholds.yellowMaxDegrees -> YELLOW
            else -> RED
        }
    }
}
