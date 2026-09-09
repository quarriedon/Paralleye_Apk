package sg.paralleye.session

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.core.content.ContextCompat

/**
 * Ch.11 §14 "Permission Management": monitoring must not begin until required permissions are
 * available, and the user must be informed rather than the app silently degrading (Ch.2 §29
 * "Graceful Degradation Principle" — never fabricate unavailable data).
 */
object PermissionChecker {

    /** Ch.11 §14: overlay permission — required for the Ch.10 mascot overlay. */
    fun hasOverlayPermission(context: Context): Boolean = Settings.canDrawOverlays(context)

    /** Android 13+ requires runtime notification permission for the foreground-service notification. */
    fun hasNotificationPermission(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

    /**
     * Ch.6 §12/Ch.2 §8: usage-access permission for foreground-app category signals. Not
     * required to start monitoring — per Ch.2 §29, its absence degrades Activity classification
     * to Unknown rather than blocking posture monitoring itself.
     */
    fun hasUsageAccessPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** Permissions monitoring cannot start without, per Ch.11 §14. */
    fun missingRequiredPermissions(context: Context): List<String> = buildList {
        if (!hasOverlayPermission(context)) add(Manifest.permission.SYSTEM_ALERT_WINDOW)
        if (!hasNotificationPermission(context)) add(Manifest.permission.POST_NOTIFICATIONS)
    }
}
