package sg.paralleye.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import sg.paralleye.data.db.dao.CalibrationDao
import sg.paralleye.data.db.entities.BaselineProfileEntity
import sg.paralleye.data.db.entities.UserProfileEntity

/**
 * Single local Room database for PARALLEYE (Ch.4 §37, Ch.11/§12 persistence requirements).
 * Reporting entities (Ch.12) are added to this same database in that module rather than a
 * separate one, per Ch.2 §14 "Single Calculation Path Principle" style discipline — one
 * authoritative local store, not several competing ones.
 */
@Database(
    entities = [UserProfileEntity::class, BaselineProfileEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class ParallayeDatabase : RoomDatabase() {
    abstract fun calibrationDao(): CalibrationDao

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
