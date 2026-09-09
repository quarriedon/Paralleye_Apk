package sg.paralleye.domain.behaviour

/**
 * Ch.1 §15-16: Dynamic Load Increment = Angle Load × Activity Multiplier × Frame Factor.
 *
 * Resolves the open conflict between Ch.7's pure frame-based formula and Ch.2 §11's
 * "Time and Frame Independence Principle" (the frame factor must behave consistently across
 * different sensor sampling rates and must not assume every callback represents the same
 * duration): [frameFactor] is scaled by the ratio of the actual elapsed interval to the
 * intended interval implied by the configured sampling rate, before being multiplied in.
 * At the intended sampling rate this reduces to exactly Ch.7's literal formula; at any other
 * effective rate it keeps real elapsed exposure consistent, per Ch.2 §11's explicit
 * instruction ("A robust implementation may... scale the frame factor relative to the
 * intended sampling rate"). This is an engineering reconciliation of two governance
 * requirements, not a disputed methodology number.
 */
object DynamicLoadEngine {
    fun calculateIncrement(
        angleLoad: Double,
        activityMultiplier: Double,
        frameFactor: Double,
        actualIntervalMillis: Double,
        intendedIntervalMillis: Double,
    ): Double {
        if (actualIntervalMillis <= 0.0 || intendedIntervalMillis <= 0.0) return 0.0
        val normalisedFrameFactor = frameFactor * (actualIntervalMillis / intendedIntervalMillis)
        return angleLoad * activityMultiplier * normalisedFrameFactor
    }
}

/**
 * Ch.1 §16: cumulative load is the principal exposure state. Session-boundary decisions
 * (when it resets, persists, or carries forward) belong to Session Management (Ch.11) — this
 * class is a plain, session-agnostic accumulator with a zero floor (shared with Recovery,
 * Ch.8 §Rule 7), owned and reset by whoever manages the session lifecycle.
 */
class CumulativeLoadEngine {
    var currentLoad: Double = 0.0
        private set

    fun addIncrement(increment: Double): Double {
        currentLoad = (currentLoad + increment).coerceAtLeast(0.0)
        return currentLoad
    }

    fun subtract(amount: Double): Double {
        currentLoad = (currentLoad - amount).coerceAtLeast(0.0)
        return currentLoad
    }

    fun reset(to: Double = 0.0) {
        currentLoad = to.coerceAtLeast(0.0)
    }
}
