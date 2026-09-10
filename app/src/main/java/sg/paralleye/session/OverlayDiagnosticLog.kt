package sg.paralleye.session

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Debugging aid, not part of the PARALLEYE methodology: writes a small, readable trail of the
 * background-mascot-overlay decision path to a file under the app's external files directory
 * (browsable via the phone's own file manager, no adb/PC needed) — the same workaround already
 * used for [sg.paralleye.ParallayeApplication]'s crash reporter, for the same reason: this
 * environment has no way to reproduce "the overlay doesn't appear when backgrounded" on a real
 * device, and guessing at a cause without seeing what actually happened has a poor track record.
 * Logs only on a state change (app visibility flips, or the mascot's own visibility changes),
 * not every sensor cycle, to stay small and readable rather than flooding the file.
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
