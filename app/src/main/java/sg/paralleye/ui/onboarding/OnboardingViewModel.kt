package sg.paralleye.ui.onboarding

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import sg.paralleye.config.ParallayeParameters
import sg.paralleye.data.calibration.CalibrationRepository
import sg.paralleye.data.db.ParallayeDatabase
import sg.paralleye.domain.calibration.BaselineProfile
import sg.paralleye.domain.calibration.Gender
import sg.paralleye.domain.calibration.ProfileValidationResult
import sg.paralleye.domain.calibration.UserProfile
import sg.paralleye.domain.calibration.UserProfileValidator

/**
 * Ch.11 §10-12, Functional Spec System 1 "User Management": owns the onboarding flow's state
 * across its screens. Profile persistence and baseline finalisation call straight into
 * [CalibrationRepository] — this view model performs no calculation of its own.
 */
class OnboardingViewModel(application: Application) : AndroidViewModel(application) {

    val params: ParallayeParameters = ParallayeParameters.PROVISIONAL
    val repository = CalibrationRepository(ParallayeDatabase.getInstance(application).calibrationDao())

    var ageInput by mutableStateOf("")
    var gender by mutableStateOf(Gender.PREFER_NOT_TO_SAY)
    var heightInput by mutableStateOf("")
    var profileValidationErrors by mutableStateOf<List<String>>(emptyList())

    fun submitProfile(onSaved: () -> Unit) {
        val age = ageInput.toIntOrNull()
        val height = heightInput.toIntOrNull()
        if (age == null || height == null) {
            profileValidationErrors = listOf("Enter a valid age and height in whole numbers.")
            return
        }
        val profile = UserProfile(ageYears = age, gender = gender, heightCm = height)
        val validation = UserProfileValidator.validate(profile)
        if (validation is ProfileValidationResult.Invalid) {
            profileValidationErrors = validation.problems
            return
        }
        profileValidationErrors = emptyList()
        viewModelScope.launch {
            repository.saveProfile(profile)
            onSaved()
        }
    }

    fun saveBaseline(baseline: BaselineProfile, onSaved: () -> Unit) {
        viewModelScope.launch {
            repository.saveOriginalBaseline(baseline)
            onSaved()
        }
    }
}
