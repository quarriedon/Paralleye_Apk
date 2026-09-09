package sg.paralleye.data.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import sg.paralleye.config.SensitivityLevel

private val Context.settingsDataStore: androidx.datastore.core.DataStore<Preferences> by
    preferencesDataStore(name = "paralleye_settings")

/**
 * The `datastore-preferences` dependency was already present but unused — nothing in the app
 * persisted a user-facing setting anywhere; [sg.paralleye.session.SessionManager] hard-coded
 * [SensitivityLevel.MEDIUM] instead of reading a real choice. This is the first thing to
 * actually use it: currently just Ch.2 §25's sensitivity level, the one approved behavioural
 * parameter exposed as user-adjustable, but the natural home for any future settings-screen
 * value that isn't itself a disputed methodology number (Ch.2 §25 already limits it to that).
 */
class SettingsRepository(private val context: Context) {

    val sensitivityLevel: Flow<SensitivityLevel> =
        context.settingsDataStore.data.map { preferences ->
            preferences[SENSITIVITY_KEY]?.let { stored ->
                runCatching { SensitivityLevel.valueOf(stored) }.getOrNull()
            } ?: SensitivityLevel.MEDIUM
        }

    suspend fun setSensitivityLevel(level: SensitivityLevel) {
        context.settingsDataStore.edit { preferences -> preferences[SENSITIVITY_KEY] = level.name }
    }

    companion object {
        private val SENSITIVITY_KEY = stringPreferencesKey("sensitivity_level")
    }
}
