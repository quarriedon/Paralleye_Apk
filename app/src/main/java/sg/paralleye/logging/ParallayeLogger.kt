package sg.paralleye.logging

import android.util.Log

/**
 * Ch.2 §33 "Logging Principle": structured technical logging for cycle data, monitoring-state
 * changes, permissions and errors. Must never carry typed content, messages, video content,
 * private screen content, passwords or personal identifiers beyond what's strictly needed —
 * [redactingFields] exists so a caller cannot accidentally pass through a raw string field
 * that might contain any of that.
 */
object ParallayeLogger {

    private const val TAG = "Paralleye"

    /** Release builds should set this to false to reduce logging per Ch.2 §33. */
    var verboseLoggingEnabled: Boolean = true

    fun cycle(record: CycleRecord) {
        if (!verboseLoggingEnabled) return
        Log.d(TAG, "cycle t=${record.timestampMillis} angle=${record.deviceAngleDegrees} " +
            "zone=${record.postureZone} load=${record.angleLoad} activity=${record.activityCategory} " +
            "mult=${record.activityMultiplier} dLoad=${record.dynamicLoadIncrement} " +
            "cumBefore=${record.cumulativeLoadBefore} cumAfter=${record.cumulativeLoadAfter} " +
            "recovery=${record.recoveryAmount} score=${record.score} alert=${record.alertState}")
    }

    fun monitoringStateChange(from: String, to: String) {
        Log.i(TAG, "monitoringState $from -> $to")
    }

    fun permissionStatus(permission: String, granted: Boolean) {
        Log.i(TAG, "permission $permission granted=$granted")
    }

    fun error(component: String, message: String, throwable: Throwable? = null) {
        Log.e(TAG, "[$component] $message", throwable)
    }

    /**
     * Explicit reminder at every call site of what must never reach a log line: typed content,
     * messages, video/screen content, passwords, or personal identifiers beyond what's needed.
     * Callers pass only field names, never the underlying values, to document what was withheld.
     */
    fun redactingFields(vararg fieldNames: String): String = "[redacted: ${fieldNames.joinToString()}]"
}
