package sg.paralleye.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import sg.paralleye.data.db.dao.CalibrationDao
import sg.paralleye.data.db.dao.ReportingDao
import sg.paralleye.data.db.entities.BaselineProfileEntity
import sg.paralleye.data.db.entities.SessionSummaryEntity
import sg.paralleye.data.db.entities.UserProfileEntity

/**
 * Single local Room database for PARALLEYE (Ch.4 §37, Ch.12 §23 persistence requirements) —
 * one authoritative local store per Ch.2 §14 "Single Calculation Path Principle" discipline,
 * not several competing ones.
 */
@Database(
    entities = [UserProfileEntity::class, BaselineProfileEntity::class, SessionSummaryEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class ParallayeDatabase : RoomDatabase() {
    abstract fun calibrationDao(): CalibrationDao
    abstract fun reportingDao(): ReportingDao

    companion object {
        @Volatile private var instance: ParallayeDatabase? = null

        fun getInstance(context: Context): ParallayeDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ParallayeDatabase::class.java,
                    "paralleye.db",
                ).build().also { instance = it }
            }
    }
}
