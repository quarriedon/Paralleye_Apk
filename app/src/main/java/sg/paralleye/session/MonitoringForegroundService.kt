package sg.paralleye.session

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import sg.paralleye.MainActivity
import sg.paralleye.R
import sg.paralleye.config.ParallayeParameters
import sg.paralleye.data.calibration.CalibrationRepository
import sg.paralleye.data.db.ParallayeDatabase
import sg.paralleye.data.reporting.ReportingRepository
import sg.paralleye.logging.ParallayeLogger

/**
 * Ch.3 §30, Ch.11 §14/§18: the foreground service PARALLEYE's continuous background sensor
 * monitoring runs within, per Android's background-execution rules. Owns one [SessionManager]
 * for the process lifetime of the service; does not perform any behavioural calculation
 * itself (that's entirely [SessionManager] and the engines it coordinates).
 */
class MonitoringForegroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var sessionManager: SessionManager? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        val database = ParallayeDatabase.getInstance(applicationContext)
        val calibrationRepository = CalibrationRepository(database.calibrationDao())
        val reportingRepository = ReportingRepository(database.reportingDao())
        val manager = SessionManager(applicationContext, calibrationRepository, reportingRepository, ParallayeParameters.PROVISIONAL)
        sessionManager = manager

        serviceScope.launch {
            when (val outcome = manager.initialise()) {
                InitialisationOutcome.Ready -> manager.startMonitoring()
                else -> {
                    ParallayeLogger.error("MonitoringForegroundService", "Initialisation did not reach Ready: $outcome")
                    stopSelf()
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        sessionManager?.completeSession()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.monitoring_notification_channel_name),
                NotificationManager.IMPORTANCE_MIN,
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    /** Ch.2 §28: the lock-screen notification must not expose posture details. */
    private fun buildNotification(): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.monitoring_notification_title))
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "paralleye_monitoring"
        private const val NOTIFICATION_ID = 1001
    }
}
