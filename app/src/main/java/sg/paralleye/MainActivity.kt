package sg.paralleye

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import sg.paralleye.data.calibration.CalibrationRepository
import sg.paralleye.data.db.ParallayeDatabase
import sg.paralleye.session.MonitoringForegroundService
import sg.paralleye.ui.onboarding.OnboardingNavHost

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
            OnboardingCheck.COMPLETE -> MonitoringActiveScreen()
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

/**
 * Ch.11 §15: onboarding already complete — starts the foreground service and shows a minimal
 * status screen. A live dashboard bound to SessionManager's cycle-result stream (score, zone,
 * mascot state) is a follow-up; this proves the pipeline runs end-to-end from app launch.
 */
@Composable
private fun MonitoringActiveScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(Unit) {
        val serviceIntent = Intent(context, MonitoringForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(context, serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Paralleye is monitoring your posture", style = MaterialTheme.typography.headlineSmall)
        Text(
            "You can put your phone away — Ms Angle Angel will check in if it's worth a stretch.",
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
