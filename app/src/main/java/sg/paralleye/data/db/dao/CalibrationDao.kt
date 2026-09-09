package sg.paralleye.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import sg.paralleye.data.db.entities.BaselineProfileEntity
import sg.paralleye.data.db.entities.UserProfileEntity

@Dao
interface CalibrationDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUserProfile(profile: UserProfileEntity)

    @Query("UPDATE user_profile SET isActive = 0 WHERE isActive = 1")
    suspend fun deactivateAllUserProfiles()

    @Query("SELECT * FROM user_profile WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveUserProfile(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertBaseline(baseline: BaselineProfileEntity)

    @Update
    suspend fun updateBaseline(baseline: BaselineProfileEntity)

    @Query("SELECT * FROM baseline_profile WHERE isOriginalBaseline = 1 AND isArchived = 0 LIMIT 1")
    suspend fun getOriginalBaseline(): BaselineProfileEntity?

    @Query("SELECT * FROM baseline_profile ORDER BY createdAtEpochMillis DESC")
    suspend fun getAllBaselines(): List<BaselineProfileEntity>

    @Query("UPDATE baseline_profile SET isArchived = 1 WHERE isOriginalBaseline = 1 AND isArchived = 0")
    suspend fun archiveCurrentOriginalBaseline()

    /** Ch.4 §35.4 "Delete All Data". */
    @Query("DELETE FROM user_profile")
    suspend fun deleteAllUserProfiles()

    @Query("DELETE FROM baseline_profile")
    suspend fun deleteAllBaselines()
}
