package sg.paralleye.domain.measurement

/**
 * Ch.3 §27 "Confidence or Quality State". Behavioural layers (Ch.5+) must accumulate load
 * only when quality is [VALID] — every other state exists so a sensor update can be measured
 * without being trusted for exposure accounting.
 */
enum class MeasurementQuality {
    VALID,
    STABILISING,
    TRANSIENT_MOVEMENT,
    ORIENTATION_TRANSITION,
    LOW_CONFIDENCE,
    SENSOR_UNAVAILABLE,
    UNSUPPORTED_DEVICE_STATE,
    INVALID;

    /** Ch.3 §27: "Later behavioural layers should accumulate load only when the measurement state is valid." */
    val isBehaviourallyValid: Boolean get() = this == VALID
}
