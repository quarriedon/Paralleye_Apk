package sg.paralleye.ui.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings as AndroidSettings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sg.paralleye.config.SensitivityLevel
import sg.paralleye.data.calibration.CalibrationRepository
import sg.paralleye.data.db.ParallayeDatabase
import sg.paralleye.data.reporting.ReportingRepository
import sg.paralleye.data.settings.SettingsRepository
import sg.paralleye.domain.calibration.UserProfile
import sg.paralleye.session.PermissionChecker

/**
 * Functional Spec System 11. Everything on this screen is either Ch.2 §25's one approved
 * user-adjustable behavioural parameter (sensitivity) or a data-management action the
 * governance chapters already specify (Ch.4 §35.4 "Delete All Data") — nothing here exposes a
 * raw, undocumented config constant, per Ch.2 §25's own limit on what sensitivity may touch.
 */
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsRepository = remember { SettingsRepository(context) }
    val sensitivity by settingsRepository.sensitivityLevel.collectAsState(initial = SensitivityLevel.MEDIUM)

    var profile by remember { mutableStateOf<UserProfile?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var deleteComplete by remember { mutableStateOf(false) }
    var batteryOptimizationExempt by remember { mutableStateOf(PermissionChecker.isIgnoringBatteryOptimizations(context)) }

    LaunchedEffect(Unit) {
        val calibrationRepository = CalibrationRepository(ParallayeDatabase.getInstance(context).calibrationDao())
        profile = withContext(Dispatchers.Default) { calibrationRepository.getActiveProfile() }
    }

    // The exemption dialog is a separate system Activity -- re-check when the user comes back
    // to this screen rather than only once on first composition.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                batteryOptimizationExempt = PermissionChecker.isIgnoringBatteryOptimizations(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall)

        Text(
            "Sensitivity",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 24.dp, bottom = 4.dp),
        )
        Text(
            "How readily Paralleye reacts to posture load and how often Ms Angle Angel reappears. " +
                "Never changes what your phone actually measures.",
            style = MaterialTheme.typography.bodySmall,
        )
        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SensitivityLevel.entries.forEach { level ->
                FilterChip(
                    selected = sensitivity == level,
                    onClick = { scope.launch { settingsRepository.setSensitivityLevel(level) } },
                    label = { Text(level.name.lowercase().replaceFirstChar { it.uppercase() }) },
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp))

        Text(
            "Background reliability",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        if (batteryOptimizationExempt) {
            Text(
                "Paralleye is exempt from battery optimization. Monitoring can keep running with " +
                    "the app closed or the screen off.",
                style = MaterialTheme.typography.bodySmall,
            )
        } else {
            Text(
                "Your phone's battery manager can stop Paralleye from monitoring once it's no " +
                    "longer on screen. Exempting it from battery optimization keeps monitoring " +
                    "running in the background.",
                style = MaterialTheme.typography.bodySmall,
            )
            Button(
                onClick = {
                    val intent = Intent(
                        AndroidSettings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:${context.packageName}"),
                    )
                    context.startActivity(intent)
                },
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text("Exempt from battery optimization")
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp))

        Text("Profile", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
        val currentProfile = profile
        if (currentProfile != null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("${currentProfile.ageYears} years old")
                    Text(currentProfile.gender.name.lowercase().replaceFirstChar { it.uppercase() })
                    Text("${currentProfile.heightCm} cm")
                }
            }
        } else {
            Text("No profile on file.", style = MaterialTheme.typography.bodySmall)
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp))

        Text("Data", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
        OutlinedButton(
            onClick = { showDeleteConfirm = true },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        ) {
            Text("Delete all data")
        }
        if (deleteComplete) {
            Text(
                "All profile, calibration and session history has been deleted.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete all data?") },
            text = {
                Text(
                    "This permanently removes your profile, baseline calibration and full session " +
                        "history from this device. You'll need to onboard again to keep using Paralleye. " +
                        "This cannot be undone.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    scope.launch {
                        val database = ParallayeDatabase.getInstance(context)
                        withContext(Dispatchers.Default) {
                            CalibrationRepository(database.calibrationDao()).deleteAllCalibrationData()
                            ReportingRepository(database.reportingDao()).deleteAllSessions()
                        }
                        profile = null
                        deleteComplete = true
                    }
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } },
        )
    }
}
