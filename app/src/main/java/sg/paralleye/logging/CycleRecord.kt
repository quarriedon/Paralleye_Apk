package sg.paralleye.logging

/**
 * Ch.2 §13 "State Immutability and Traceability": one traceable evaluation record per
 * monitoring cycle, carrying the inputs and outputs used for that cycle. Engines populate
 * the fields they own as they run; nothing here recalculates another engine's output.
 *
 * Deliberately holds primitives, not domain types, so this record has no compile-time
 * dependency on the measurement/behaviour engines built in later phases — each engine
 * attaches its own values to an existing record rather than this file depending on them.
 */
data class CycleRecord(
    val timestampMillis: Long,
    val deviceAngleDegrees: Double? = null,
    val neckFlexionDegrees: Double? = null,
    val postureZone: String? = null,
    val angleLoad: Double? = null,
    val activityCategory: String? = null,
    val activityMultiplier: Double? = null,
    val dynamicLoadIncrement: Double? = null,
    val cumulativeLoadBefore: Double? = null,
    val cumulativeLoadAfter: Double? = null,
    val recoveryAmount: Double? = null,
    val score: Int? = null,
    val alertState: String? = null,
)
