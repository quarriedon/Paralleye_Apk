package sg.paralleye.session

import android.content.Context
import sg.paralleye.config.ParallayeParameters
import sg.paralleye.data.calibration.CalibrationRepository
import sg.paralleye.data.db.ParallayeDatabase
import sg.paralleye.data.reporting.ReportingRepository
import sg.paralleye.data.settings.SettingsRepository

/**
 * Process-wide singleton so [MonitoringForegroundService] and the UI (`MonitoringActiveScreen`)
 * observe the *same* [SessionManager] instance and its `cycleResults` stream, rather than each
 * creating their own — there must be one authoritative live session per Ch.2 §14 "Single
 * Calculation Path Principle" discipline.
 */
object SessionManagerHolder {
    @Volatile private var instance: SessionManager? = null

    // Set when the user explicitly stops monitoring (MainActivity's Stop button) so the Monitor
    // screen's auto-start doesn't silently undo it the next time that screen composes (e.g.
    // switching tabs and back) -- confirmed there was previously no user-facing way to stop or
    // pause monitoring at all short of force-stopping the whole app from Android Settings.
    @Volatile var stoppedByUser: Boolean = false
        private set

    fun getInstance(context: Context): SessionManager =
        instance ?: synchronized(this) {
            instance ?: run {
                val appContext = context.applicationContext
                val database = ParallayeDatabase.getInstance(appContext)
                SessionManager(
                    context = appContext,
                    calibrationRepository = CalibrationRepository(database.calibrationDao()),
                    reportingRepository = ReportingRepository(database.reportingDao()),
                    settingsRepository = SettingsRepository(appContext),
                    params = ParallayeParameters.PROVISIONAL,
                ).also { instance = it }
            }
        }

    /**
     * The running [SessionManager] is unusable after [SessionManager.completeSession] (its
     * coroutine scope is cancelled) -- call this once the foreground service has actually been
     * stopped so a later [getInstance] builds a fresh instance instead of reusing the dead one.
     */
    fun stopByUser() {
        stoppedByUser = true
        synchronized(this) { instance = null }
    }

    /** Called when the user explicitly starts monitoring again after stopping it. */
    fun clearStoppedByUser() {
        stoppedByUser = false
    }
}
