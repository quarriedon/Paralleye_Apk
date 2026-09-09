package sg.paralleye.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import sg.paralleye.domain.calibration.BaselineCaptureOutcome
import sg.paralleye.domain.calibration.Gender

/** Ch.11 §10, Functional Spec System 1. */
@Composable
fun WelcomeScreen(onContinue: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Welcome to Paralleye", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Paralleye passively monitors your posture using your phone's motion sensors only — " +
                "no camera, no microphone, no facial recognition.",
            modifier = Modifier.padding(top = 16.dp, bottom = 24.dp),
        )
        Button(onClick = onContinue) { Text("Get started") }
    }
}

/** Ch.4 §5, Functional Spec System 1: age/gender/height, validated before saving. */
@Composable
fun ProfileScreen(viewModel: OnboardingViewModel, onSaved: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Tell us a little about you", style = MaterialTheme.typography.headlineSmall)
        Text(
            "This stays on your device and personalises how Paralleye encourages you — it never changes what your phone actually measures.",
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
        )

        OutlinedTextField(
            value = viewModel.ageInput,
            onValueChange = { viewModel.ageInput = it },
            label = { Text("Age") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        )
        OutlinedTextField(
            value = viewModel.heightInput,
            onValueChange = { viewModel.heightInput = it },
            label = { Text("Height (cm)") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        )

        Text("Gender", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(bottom = 4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Gender.entries.forEach { option ->
                FilterChip(
                    selected = viewModel.gender == option,
                    onClick = { viewModel.gender = option },
                    label = { Text(option.name) },
                )
            }
        }

        viewModel.profileValidationErrors.forEach {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 4.dp))
        }

        Button(onClick = { viewModel.submitProfile(onSaved) }, modifier = Modifier.padding(top = 16.dp)) {
            Text("Continue")
        }
    }
}

/** Ch.11 §14: overlay + notification permissions, explained before requesting, checked after. */
@Composable
fun PermissionExplanationScreen(onAllGranted: () -> Unit) {
    val context = LocalContext.current
    var missing by remember { mutableStateOf(sg.paralleye.session.PermissionChecker.missingRequiredPermissions(context)) }

    val overlayLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
    ) {
        missing = sg.paralleye.session.PermissionChecker.missingRequiredPermissions(context)
    }
    val notificationLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
    ) {
        missing = sg.paralleye.session.PermissionChecker.missingRequiredPermissions(context)
    }

    LaunchedEffect(missing) {
        if (missing.isEmpty()) onAllGranted()
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("A couple of permissions", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Paralleye needs permission to display Ms Angle Angel over other apps, and to show " +
                "a quiet ongoing notification while monitoring runs in the background.",
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
        )
        if (!sg.paralleye.session.PermissionChecker.hasOverlayPermission(context)) {
            Button(onClick = {
                overlayLauncher.launch(
                    android.content.Intent(
                        android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        android.net.Uri.parse("package:${context.packageName}"),
                    ),
                )
            }) { Text("Allow display over other apps") }
        }
        if (!sg.paralleye.session.PermissionChecker.hasNotificationPermission(context)) {
            Button(
                onClick = { notificationLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS) },
                modifier = Modifier.padding(top = 8.dp),
            ) { Text("Allow notifications") }
        }
    }
}

/** Ch.4 §9-12: guided baseline capture, live sensor-driven. */
@Composable
fun GuidedCalibrationScreen(viewModel: OnboardingViewModel, onComplete: () -> Unit, onFailed: () -> Unit) {
    val context = LocalContext.current
    val controller = remember { CalibrationCaptureController(context, viewModel.params) }
    val outcome by controller.outcome.collectAsState()

    // Ch.3 §28: sensors must not remain registered once this screen is left, whether capture
    // finished or the user simply navigated away (e.g. system back) mid-capture.
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose { controller.stop() }
    }

    LaunchedEffect(Unit) { controller.start() }

    LaunchedEffect(outcome) {
        val current = outcome
        if (current is BaselineCaptureOutcome.Success) {
            viewModel.saveBaseline(current.profile, onComplete)
        } else if (current is BaselineCaptureOutcome.Failure) {
            onFailed()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Hold your phone as you normally would", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Sit or stand naturally, look at the centre of the screen, and stay still for a few seconds.",
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
        )
        when (val current = outcome) {
            is BaselineCaptureOutcome.InProgress -> {
                LinearProgressIndicator(
                    progress = { current.validSampleCount.toFloat() / current.requiredSamples.toFloat() },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("${current.validSampleCount} / ${current.requiredSamples} stable readings")
            }
            else -> CircularProgressIndicator()
        }
    }
}

@Composable
fun CalibrationFailedScreen(onRetry: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("That didn't quite work", style = MaterialTheme.typography.headlineSmall)
        Text(
            "We couldn't get a stable reading — try holding the phone steadier this time.",
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
        )
        Button(onClick = onRetry) { Text("Try again") }
    }
}

@Composable
fun OnboardingCompleteScreen(onFinish: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("You're all set", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Paralleye will now monitor your posture in the background and check in through Ms Angle Angel when it's worth a stretch.",
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
        )
        Button(onClick = onFinish) { Text("Start monitoring") }
    }
}
