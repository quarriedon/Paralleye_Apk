package sg.paralleye

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import sg.paralleye.config.ParallayeParameters
import sg.paralleye.data.calibration.CalibrationRepository
import sg.paralleye.data.db.ParallayeDatabase
import sg.paralleye.domain.alert.MascotVisibility
import sg.paralleye.session.MonitoringForegroundService
import sg.paralleye.session.MonitoringState
import sg.paralleye.session.SessionManagerHolder
import sg.paralleye.ui.history.HistoryScreen
import sg.paralleye.ui.mascot.MsAngleAngelOverlay
import sg.paralleye.ui.onboarding.OnboardingNavHost
import sg.paralleye.ui.settings.SettingsScreen

/**
 * Ch.11 §15, §20 "Application Startup": on every launch, checks whether onboarding has already
 * completed (a stored profile and original baseline both exist) and routes accordingly — the
 * user is never asked to repeat onboarding once it's done.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                AppRoot()
            }
        }
    }
}

private enum class OnboardingCheck { LOADING, REQUIRED, COMPLETE }

@Composable
private fun AppRoot() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var check by remember { mutableStateOf(OnboardingCheck.LOADING) }

    LaunchedEffect(Unit) {
        val repository = CalibrationRepository(ParallayeDatabase.getInstance(context).calibrationDao())
        val onboarded = withContext(Dispatchers.Default) {
            repository.getActiveProfile() != null && repository.getOriginalBaseline() != null
        }
        check = if (onboarded) OnboardingCheck.COMPLETE else OnboardingCheck.REQUIRED
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        when (check) {
            OnboardingCheck.LOADING -> LoadingScreen()
            OnboardingCheck.REQUIRED -> OnboardingNavHost(onOnboardingComplete = { check = OnboardingCheck.COMPLETE })
            OnboardingCheck.COMPLETE -> MainTabs()
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

private enum class MainTab(val label: String) {
    MONITOR("Monitor"),
    HISTORY("History"),
    SETTINGS("Settings"),
}

/** Functional Spec Systems 9-11: the three top-level screens once onboarding is done. */
@Composable
private fun MainTabs() {
    var selectedTab by remember { mutableStateOf(MainTab.MONITOR) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                MainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = {},
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (selectedTab) {
                MainTab.MONITOR -> MonitoringActiveScreen()
                MainTab.HISTORY -> HistoryScreen()
                MainTab.SETTINGS -> SettingsScreen()
            }
        }
    }
}

/**
 * Ch.11 §15: onboarding already complete — starts the foreground service, then observes the
 * *same* [sg.paralleye.session.SessionManager] instance (via [SessionManagerHolder]) that the
 * service is driving, so the live score/zone and the Ch.10 mascot overlay actually reflect
 * what the background pipeline is doing rather than a disconnected placeholder.
 *
 * Ch.11 §26-29 "Pause"/"Stop": [sg.paralleye.session.SessionManager.pauseMonitoring],
 * `resumeMonitoring` and `completeSession` all existed already but nothing in the app ever
 * called any of them — confirmed there was genuinely no way for the user to pause or stop
 * monitoring short of force-stopping the whole app from Android Settings. Stop tears down the
 * foreground service (through the same `onDestroy` -> `completeSession` path that already
 * existed for system-initiated teardown) and flips [SessionManagerHolder.stoppedByUser] so this
 * screen shows a "Start Monitoring" prompt instead of silently auto-restarting the moment the
 * user switches back to this tab.
 */
@Composable
private fun MonitoringActiveScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var stoppedByUser by remember { mutableStateOf(SessionManagerHolder.stoppedByUser) }

    if (stoppedByUser) {
        StoppedScreen(
            onStart = {
                SessionManagerHolder.clearStoppedByUser()
                stoppedByUser = false
            },
        )
        return
    }

    val sessionManager = remember(stoppedByUser) { SessionManagerHolder.getInstance(context) }
    val cycleResult by sessionManager.cycleResults.collectAsState()
    val monitoringState by sessionManager.monitoringState.collectAsState()

    // Display-only throttle. The pipeline emits at the full Ch.3 sensor sampling rate --
    // correct, and unchanged, for scoring/alerts -- but redrawing the angle number that fast
    // reads as flickery to a human eye even though the underlying value is already smoothed
    // (ComplementaryFilter). This only slows how often the *same* already-correct value gets
    // redrawn as text; it touches no measurement, scoring or alert logic, and the mascot
    // overlay below still binds to the full-rate cycleResult directly.
    var displayedResult by remember { mutableStateOf(cycleResult) }
    LaunchedEffect(Unit) {
        while (true) {
            displayedResult = sessionManager.cycleResults.value
            kotlinx.coroutines.delay(250)
        }
    }

    LaunchedEffect(Unit) {
        val serviceIntent = Intent(context, MonitoringForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(context, serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text("Paralleye is monitoring your posture", style = MaterialTheme.typography.headlineSmall)
            Text(
                "You can put your phone away — Ms Angle Angel will check in if it's worth a stretch.",
                modifier = Modifier.padding(top = 8.dp),
            )
            val result = displayedResult
            if (result != null) {
                Text("Score: ${result.score}", modifier = Modifier.padding(top = 24.dp))
                Text("Angle: %.1f°".format(result.angleDegrees))
                Text("Zone: ${result.zone}")
            }

            Row(modifier = Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                when (monitoringState) {
                    MonitoringState.MONITORING_ACTIVE ->
                        OutlinedButton(onClick = { sessionManager.pauseMonitoring() }) { Text("Pause") }
                    MonitoringState.MONITORING_PAUSED ->
                        Button(onClick = { sessionManager.resumeMonitoring() }) { Text("Resume") }
                    else -> Unit
                }
                if (monitoringState == MonitoringState.MONITORING_ACTIVE || monitoringState == MonitoringState.MONITORING_PAUSED) {
                    OutlinedButton(
                        onClick = {
                            context.stopService(Intent(context, MonitoringForegroundService::class.java))
                            SessionManagerHolder.stopByUser()
                            stoppedByUser = true
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) { Text("Stop") }
                }
            }
            if (monitoringState == MonitoringState.MONITORING_PAUSED) {
                Text(
                    "Paused — no posture tracking or alerts until you resume.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

        MsAngleAngelOverlay(
            visibility = cycleResult?.alertVisibility ?: MascotVisibility.Hidden,
            config = ParallayeParameters.PROVISIONAL.mascot,
            onTapped = { sessionManager.onMascotTapped() },
            modifier = Modifier.align(Alignment.TopEnd),
        )
    }
}

@Composable
private fun StoppedScreen(onStart: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Monitoring is stopped", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Paralleye isn't watching your posture right now.",
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
            )
            Button(onClick = onStart) { Text("Start Monitoring") }
        }
    }
}
