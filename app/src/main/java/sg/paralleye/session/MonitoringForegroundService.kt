package sg.paralleye.session

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import sg.paralleye.MainActivity
import sg.paralleye.R
import sg.paralleye.config.ParallayeParameters
import sg.paralleye.domain.alert.MascotVisibility
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
    private var wakeLock: PowerManager.WakeLock? = null
    private var mascotOverlayController: MascotOverlayController? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        // Ch.2 §34 "Error-Handling Principle": a foreground-service-type/permission mismatch
        // (Android version-dependent, easy to get wrong, and it's already bitten this app once)
        // must fail predictably rather than crash the whole process.
        try {
            startForeground(NOTIFICATION_ID, buildNotification())
        } catch (error: Exception) {
            ParallayeLogger.error("MonitoringForegroundService", "startForeground failed", error)
            stopSelf()
            return
        }

        // README "Background monitoring": non-wakeup sensors (accelerometer/gyroscope) stop
        // delivering events once the CPU suspends under Doze/light-sleep with the screen off --
        // a foreground service alone does not prevent that. A held PARTIAL_WAKE_LOCK is the
        // deliberate battery-for-reliability tradeoff this requires; see docs/open-questions.md.
        wakeLock = (getSystemService(POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$packageName:monitoring")
            .apply { setReferenceCounted(false); acquire() }

        val overlayController = MascotOverlayController(applicationContext, ParallayeParameters.PROVISIONAL.mascot)
        mascotOverlayController = overlayController

        val manager = SessionManagerHolder.getInstance(applicationContext)
        sessionManager = manager
        overlayController.onTapped = { manager.onMascotTapped() }

        serviceScope.launch {
            when (val outcome = manager.initialise()) {
                InitialisationOutcome.Ready -> {
                    manager.startMonitoring()
                    observeMascotOverlay(manager, overlayController)
                }
                else -> {
                    ParallayeLogger.error("MonitoringForegroundService", "Initialisation did not reach Ready: $outcome")
                    stopSelf()
                }
            }
        }
    }

    /**
     * Ch.10 §12, §38: the mascot must reach the user even while [sg.paralleye.MainActivity]
     * isn't visible -- previously it only ever rendered inside that Activity's own Compose
     * tree ([sg.paralleye.ui.mascot.MsAngleAngelOverlay]), so alerts were silently invisible
     * the moment the app was backgrounded despite the pipeline still running. Shown here only
     * while the app is *not* visible, so the two presentations are never both on screen.
     */
    private fun observeMascotOverlay(manager: SessionManager, overlayController: MascotOverlayController) {
        serviceScope.launch {
            combine(manager.cycleResults, AppVisibilityTracker.isAppVisible) { cycle, appVisible -> cycle to appVisible }
                .collect { (cycle, appVisible) ->
                    if (appVisible) {
                        overlayController.hide()
                    } else {
                        overlayController.update(cycle?.alertVisibility ?: MascotVisibility.Hidden)
                    }
                }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        sessionManager?.completeSession()
        mascotOverlayController?.hide()
        wakeLock?.let { if (it.isHeld) it.release() }
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
