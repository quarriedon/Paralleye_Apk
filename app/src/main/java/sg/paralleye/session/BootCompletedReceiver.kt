package sg.paralleye.session

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import sg.paralleye.data.calibration.CalibrationRepository
import sg.paralleye.data.db.ParallayeDatabase

/**
 * Ch.11 §19 "Automatic Startup After Device Restart" / Ch.3 §29: restarts monitoring after
 * boot only where onboarding is already complete and the required permissions are already
 * granted — never fabricates readiness, per Ch.2 §29.
 */
class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val repository = CalibrationRepository(ParallayeDatabase.getInstance(context).calibrationDao())
                val hasProfile = repository.getActiveProfile() != null
                val hasBaseline = repository.getOriginalBaseline() != null
                val hasPermissions = PermissionChecker.missingRequiredPermissions(context).isEmpty()

                if (hasProfile && hasBaseline && hasPermissions) {
                    ContextCompat.startForegroundService(context, Intent(context, MonitoringForegroundService::class.java))
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
