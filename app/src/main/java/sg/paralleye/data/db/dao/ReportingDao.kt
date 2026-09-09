package sg.paralleye.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import sg.paralleye.data.db.entities.SessionSummaryEntity

@Dao
interface ReportingDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSession(session: SessionSummaryEntity)

    @Query("SELECT * FROM session_summary ORDER BY startEpochMillis DESC")
    suspend fun getAllSessions(): List<SessionSummaryEntity>

    @Query("SELECT * FROM session_summary ORDER BY startEpochMillis DESC LIMIT :limit")
    suspend fun getRecentSessions(limit: Int): List<SessionSummaryEntity>

    /** Ch.4 §35.4 "Delete All Data" extends to session history. */
    @Query("DELETE FROM session_summary")
    suspend fun deleteAllSessions()
}
