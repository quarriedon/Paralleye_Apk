package sg.paralleye.domain.calibration

/** Ch.4 §5.2: options kept respectful and clearly defined; the MVP does not invent a gender-based posture formula. */
enum class Gender { FEMALE, MALE, OTHER, PREFER_NOT_TO_SAY }

/**
 * Ch.4 §5 profile inputs. Age/gender/height are collected and stored but — per Ch.4 §19 —
 * must not alter the objective live device angle; none of the engines below Sensor Framework
 * read this class directly.
 */
data class UserProfile(
    val ageYears: Int,
    val gender: Gender,
    val heightCm: Int,
) {
    companion object {
        val VALID_AGE_RANGE = 5..120
        /** Ch.4 §5.1 doesn't resolve exact bounds; a broad but plausible human-height range. */
        val VALID_HEIGHT_CM_RANGE = 80..250
    }
}

sealed interface ProfileValidationResult {
    data object Valid : ProfileValidationResult
    data class Invalid(val problems: List<String>) : ProfileValidationResult
}

object UserProfileValidator {
    fun validate(profile: UserProfile): ProfileValidationResult {
        val problems = mutableListOf<String>()
        if (profile.ageYears !in UserProfile.VALID_AGE_RANGE) {
            problems += "Age ${profile.ageYears} outside permitted range ${UserProfile.VALID_AGE_RANGE}"
        }
        if (profile.heightCm !in UserProfile.VALID_HEIGHT_CM_RANGE) {
            problems += "Height ${profile.heightCm}cm outside permitted range ${UserProfile.VALID_HEIGHT_CM_RANGE}"
        }
        return if (problems.isEmpty()) ProfileValidationResult.Valid else ProfileValidationResult.Invalid(problems)
    }
}
