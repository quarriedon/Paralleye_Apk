package sg.paralleye.session

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Debugging aid, not part of the PARALLEYE methodology: writes a small, readable trail to a
 * file under the app's external files directory (browsable via the phone's own file manager,
 * no adb/PC needed) — the same workaround already used for [sg.paralleye.ParallayeApplication]'s
 * crash reporter, for the same reason: this environment has no way to reproduce on-device-only
 * bugs directly, and guessing at a cause without seeing what actually happened has a poor track
 * record. Originally just the background-mascot-overlay decision path (name kept for that),
 * now also carries a throttled angle/load/score trail from [SessionManager.onSample] for
 * diagnosing score/recovery reports the same way. Callers throttle their own entries (state
 * changes only, or time-throttled) so this stays small and readable rather than flooding.
 */
object OverlayDiagnosticLog {
    private const val MAX_LINES = 200
    private val lines = ArrayDeque<String>()
    @Volatile private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    @Synchronized
    fun log(message: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())
        lines.addLast("$timestamp $message")
        while (lines.size > MAX_LINES) lines.removeFirst()
        writeToFile()
    }

    private fun writeToFile() {
        val context = appContext ?: return
        runCatching {
            val dir = context.getExternalFilesDir("diagnostics") ?: return
            dir.mkdirs()
            File(dir, "overlay_diagnostics.txt").writeText(lines.joinToString("\n"))
        }
    }
}
