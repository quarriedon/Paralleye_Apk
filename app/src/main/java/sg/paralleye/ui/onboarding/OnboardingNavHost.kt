package sg.paralleye.ui.onboarding

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

private object OnboardingRoute {
    const val WELCOME = "welcome"
    const val PROFILE = "profile"
    const val PERMISSIONS = "permissions"
    const val CALIBRATION = "calibration"
    const val CALIBRATION_FAILED = "calibration_failed"
    const val COMPLETE = "complete"
}

/** Ch.11 §10, §20 "Operational Flow": First Launch -> Onboarding -> Profile -> Calibration -> Ready. */
@Composable
fun OnboardingNavHost(onOnboardingComplete: () -> Unit, navController: NavHostController = rememberNavController()) {
    val viewModel: OnboardingViewModel = viewModel()

    NavHost(navController = navController, startDestination = OnboardingRoute.WELCOME) {
        composable(OnboardingRoute.WELCOME) {
            WelcomeScreen(onContinue = { navController.navigate(OnboardingRoute.PROFILE) })
        }
        composable(OnboardingRoute.PROFILE) {
            ProfileScreen(viewModel, onSaved = { navController.navigate(OnboardingRoute.PERMISSIONS) })
        }
        composable(OnboardingRoute.PERMISSIONS) {
            PermissionExplanationScreen(onAllGranted = { navController.navigate(OnboardingRoute.CALIBRATION) })
        }
        composable(OnboardingRoute.CALIBRATION) {
            GuidedCalibrationScreen(
                viewModel,
                onComplete = { navController.navigate(OnboardingRoute.COMPLETE) },
                onFailed = { navController.navigate(OnboardingRoute.CALIBRATION_FAILED) },
            )
        }
        composable(OnboardingRoute.CALIBRATION_FAILED) {
            CalibrationFailedScreen(onRetry = { navController.navigate(OnboardingRoute.CALIBRATION) })
        }
        composable(OnboardingRoute.COMPLETE) {
            OnboardingCompleteScreen(onFinish = onOnboardingComplete)
        }
    }
}
