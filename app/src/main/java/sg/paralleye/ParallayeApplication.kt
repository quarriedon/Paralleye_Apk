package sg.paralleye

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Process
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess
import sg.paralleye.session.AppVisibilityTracker

/**
 * Debugging aid, not part of the PARALLEYE methodology: captures uncaught exceptions to a file
 * under the app's external files directory (browsable via the phone's own file manager, no
 * adb/PC needed) and hands off to [CrashReportActivity] so the trace is also readable directly
 * on screen, in place of the default system "keeps stopping" dialog.
 */
class ParallayeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(AppVisibilityLifecycleCallbacks())
        val appContext = this
        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            val traceText = formatTrace(throwable)
            runCatching { writeCrashLog(appContext, traceText) }

            runCatching {
                val intent = Intent(appContext, CrashReportActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    putExtra(CrashReportActivity.EXTRA_STACK_TRACE, traceText)
                }
                appContext.startActivity(intent)
            }

            Process.killProcess(Process.myPid())
            exitProcess(1)
        }
    }

    private fun formatTrace(throwable: Throwable): String {
        val versionName = runCatching {
            packageManager.getPackageInfo(packageName, 0).versionName
        }.getOrDefault("unknown")

        val header = buildString {
            appendLine("Paralleye crash log")
            appendLine("Time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
            appendLine("App version: $versionName")
            appendLine()
        }
        val stackWriter = StringWriter()
        throwable.printStackTrace(PrintWriter(stackWriter))
        return header + stackWriter.toString()
    }

    private fun writeCrashLog(context: Application, traceText: String) {
        val dir = context.getExternalFilesDir("crash_logs") ?: return
        dir.mkdirs()
        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
        File(dir, "crash_$timestamp.txt").writeText(traceText)
    }
}

/** Only `onActivityStarted`/`onActivityStopped` matter to [AppVisibilityTracker]; the rest are no-ops. */
private class AppVisibilityLifecycleCallbacks : Application.ActivityLifecycleCallbacks {
    override fun onActivityStarted(activity: Activity) = AppVisibilityTracker.onActivityStarted()
    override fun onActivityStopped(activity: Activity) = AppVisibilityTracker.onActivityStopped()
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}
