package sg.paralleye.data.calibration

import sg.paralleye.data.db.dao.CalibrationDao
import sg.paralleye.data.db.entities.BaselineProfileEntity
import sg.paralleye.data.db.entities.UserProfileEntity
import sg.paralleye.domain.calibration.BaselineProfile
import sg.paralleye.domain.calibration.BaselineQuality
import sg.paralleye.domain.calibration.Gender
import sg.paralleye.domain.calibration.UserProfile
import sg.paralleye.domain.measurement.ScreenOrientation
import java.util.UUID

/**
 * Ch.4 §14/§24: original baseline is never overwritten. Ch.4 §33: a profile edit creates a
 * new version rather than mutating history. Ch.4 §35: distinct reset operations, each with a
 * different, deliberately narrow blast radius.
 */
class CalibrationRepository(private val dao: CalibrationDao) {

    suspend fun saveProfile(profile: UserProfile) {
        dao.deactivateAllUserProfiles()
        dao.insertUserProfile(
            UserProfileEntity(
                profileId = UUID.randomUUID().toString(),
                createdAtEpochMillis = System.currentTimeMillis(),
                ageYears = profile.ageYears,
                gender = profile.gender.name,
                heightCm = profile.heightCm,
                isActive = true,
            ),
        )
    }

    suspend fun getActiveProfile(): UserProfile? =
        dao.getActiveUserProfile()?.let {
            UserProfile(ageYears = it.ageYears, gender = Gender.valueOf(it.gender), heightCm = it.heightCm)
        }

    suspend fun saveOriginalBaseline(baseline: BaselineProfile) {
        require(baseline.isOriginalBaseline) { "saveOriginalBaseline requires isOriginalBaseline=true; use a dedicated path for non-original records if ever needed" }
        dao.insertBaseline(baseline.toEntity(isArchived = false))
    }

    suspend fun getOriginalBaseline(): BaselineProfile? = dao.getOriginalBaseline()?.toDomain()

    /** Ch.4 §35.3 "Start New Baseline Assessment": archives the current original, then saves the new one as original. */
    suspend fun startNewBaselineAssessment(newBaseline: BaselineProfile) {
        require(newBaseline.isOriginalBaseline)
        dao.archiveCurrentOriginalBaseline()
        dao.insertBaseline(newBaseline.toEntity(isArchived = false))
    }

    suspend fun getArchivedBaselines(): List<BaselineProfile> =
        dao.getAllBaselines().filter { !it.isOriginalBaseline || it.isArchived }.map { it.toDomain() }

    /** Ch.4 §35.4 "Delete All Data": profile + baseline. Session/history deletion is Ch.12's Reporting repository's concern. */
    suspend fun deleteAllCalibrationData() {
        dao.deleteAllUserProfiles()
        dao.deleteAllBaselines()
    }

    private fun BaselineProfile.toEntity(isArchived: Boolean) = BaselineProfileEntity(
        baselineId = baselineId,
        createdAtEpochMillis = createdAtEpochMillis,
        profileVersion = profileVersion,
        guidedBaselineDeviceAngleDegrees = guidedBaselineDeviceAngleDegrees,
        captureOrientation = captureOrientation.name,
        captureDurationMillis = captureDurationMillis,
        validSampleCount = validSampleCount,
        quality = quality.name,
        methodologyVersion = methodologyVersion,
        configurationVersion = configurationVersion,
        isOriginalBaseline = isOriginalBaseline,
        isArchived = isArchived,
    )

    private fun BaselineProfileEntity.toDomain() = BaselineProfile(
        baselineId = baselineId,
        createdAtEpochMillis = createdAtEpochMillis,
        profileVersion = profileVersion,
        guidedBaselineDeviceAngleDegrees = guidedBaselineDeviceAngleDegrees,
        captureOrientation = ScreenOrientation.valueOf(captureOrientation),
        captureDurationMillis = captureDurationMillis,
        validSampleCount = validSampleCount,
        quality = BaselineQuality.valueOf(quality),
        methodologyVersion = methodologyVersion,
        configurationVersion = configurationVersion,
        isOriginalBaseline = isOriginalBaseline,
    )
}
