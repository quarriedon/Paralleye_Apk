package sg.paralleye.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Ch.4 §13/§33: profile is versioned so an edit (§33 "Profile Updates") never silently
 * rewrites history — [isActive] marks which row is current; prior rows are kept, not deleted.
 */
@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val profileId: String,
    val createdAtEpochMillis: Long,
    val ageYears: Int,
    val gender: String,
    val heightCm: Int,
    val isActive: Boolean,
)

/** Ch.4 §13, §36 "Baseline Versioning". [isOriginalBaseline] rows are never overwritten (Ch.4 §14/§24). */
@Entity(tableName = "baseline_profile")
data class BaselineProfileEntity(
    @PrimaryKey val baselineId: String,
    val createdAtEpochMillis: Long,
    val profileVersion: String,
    val guidedBaselineDeviceAngleDegrees: Double,
    val captureOrientation: String,
    val captureDurationMillis: Long,
    val validSampleCount: Int,
    val quality: String,
    val methodologyVersion: String,
    val configurationVersion: String,
    val isOriginalBaseline: Boolean,
    /** Ch.4 §35.3: a superseded baseline is archived, not deleted, when a new assessment is confirmed. */
    val isArchived: Boolean,
)
