package sg.paralleye.session

import android.content.Context
import sg.paralleye.config.ParallayeParameters
import sg.paralleye.data.calibration.CalibrationRepository
import sg.paralleye.data.db.ParallayeDatabase
import sg.paralleye.data.reporting.ReportingRepository

/**
 * Process-wide singleton so [MonitoringForegroundService] and the UI (`MonitoringActiveScreen`)
 * observe the *same* [SessionManager] instance and its `cycleResults` stream, rather than each
 * creating their own — there must be one authoritative live session per Ch.2 §14 "Single
 * Calculation Path Principle" discipline.
 */
object SessionManagerHolder {
    @Volatile private var instance: SessionManager? = null

    fun getInstance(context: Context): SessionManager =
        instance ?: synchronized(this) {
            instance ?: run {
                val appContext = context.applicationContext
                val database = ParallayeDatabase.getInstance(appContext)
                SessionManager(
                    context = appContext,
                    calibrationRepository = CalibrationRepository(database.calibrationDao()),
                    reportingRepository = ReportingRepository(database.reportingDao()),
                    params = ParallayeParameters.PROVISIONAL,
                ).also { instance = it }
            }
        }
}
