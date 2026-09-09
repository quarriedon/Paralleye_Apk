package sg.paralleye

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

/**
 * Not part of the PARALLEYE methodology — a debugging aid only. Shown in place of the normal
 * system "keeps stopping" dialog after an uncaught exception (see [ParallayeApplication]), so
 * the crash can be read and screenshotted directly on-device without needing adb/a PC.
 */
class CrashReportActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val trace = intent.getStringExtra(EXTRA_STACK_TRACE) ?: "No stack trace captured."
        setContent {
            MaterialTheme {
                CrashScreen(trace)
            }
        }
    }

    companion object {
        const val EXTRA_STACK_TRACE = "stack_trace"
    }
}

@Composable
private fun CrashScreen(trace: String) {
    Surface(modifier = Modifier.fillMaxSize()) {
        SelectionContainer {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
                    .horizontalScroll(rememberScrollState()),
            ) {
                Text("Paralleye crashed — details below", style = MaterialTheme.typography.titleMedium)
                Text(
                    trace,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }
}
