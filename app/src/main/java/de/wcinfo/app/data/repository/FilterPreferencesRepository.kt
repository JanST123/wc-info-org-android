package de.wcinfo.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import de.wcinfo.app.data.model.ToiletFilterSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.filterPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "toilet_filter_settings")

class FilterPreferencesRepository(
    private val context: Context,
    private val dataStore: DataStore<Preferences> = context.filterPreferencesDataStore
) {

    object Keys {
        val SHOW_CLOSED = booleanPreferencesKey("show_closed")
        val SHOW_NON_PUBLIC = booleanPreferencesKey("show_non_public")
        val SHOW_NON_WHEELCHAIR_ACCESSIBLE = booleanPreferencesKey("show_non_wheelchair_accessible")
        val SHOW_WITHOUT_CHANGING_TABLE = booleanPreferencesKey("show_without_changing_table")
        val SHOW_WITHOUT_GENDER_SEPARATION = booleanPreferencesKey("show_without_gender_separation")
        val SHOW_WITHOUT_EURO_KEY = booleanPreferencesKey("show_without_euro_key")
        val ALLOW_NON_PUBLIC_FALLBACK = booleanPreferencesKey("allow_non_public_fallback")
        val MAX_PUBLIC_DISTANCE_METERS = intPreferencesKey("max_public_distance_meters")
    }

    val filterSettingsFlow: Flow<ToiletFilterSettings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            ToiletFilterSettings(
                showClosed = preferences[Keys.SHOW_CLOSED] ?: false,
                showNonPublic = preferences[Keys.SHOW_NON_PUBLIC] ?: false,
                showNonWheelchairAccessible = preferences[Keys.SHOW_NON_WHEELCHAIR_ACCESSIBLE] ?: true,
                showWithoutChangingTable = preferences[Keys.SHOW_WITHOUT_CHANGING_TABLE] ?: true,
                showWithoutGenderSeparation = preferences[Keys.SHOW_WITHOUT_GENDER_SEPARATION] ?: true,
                showWithoutEuroKey = preferences[Keys.SHOW_WITHOUT_EURO_KEY] ?: true,
                allowNonPublicFallback = preferences[Keys.ALLOW_NON_PUBLIC_FALLBACK] ?: true,
                maxPublicDistanceMeters = preferences[Keys.MAX_PUBLIC_DISTANCE_METERS] ?: 500
            )
        }

    suspend fun getFilterSettings(): ToiletFilterSettings {
        return filterSettingsFlow.first()
    }

    suspend fun saveFilterSettings(settings: ToiletFilterSettings) {
        dataStore.edit { preferences ->
            preferences[Keys.SHOW_CLOSED] = settings.showClosed
            preferences[Keys.SHOW_NON_PUBLIC] = settings.showNonPublic
            preferences[Keys.SHOW_NON_WHEELCHAIR_ACCESSIBLE] = settings.showNonWheelchairAccessible
            preferences[Keys.SHOW_WITHOUT_CHANGING_TABLE] = settings.showWithoutChangingTable
            preferences[Keys.SHOW_WITHOUT_GENDER_SEPARATION] = settings.showWithoutGenderSeparation
            preferences[Keys.SHOW_WITHOUT_EURO_KEY] = settings.showWithoutEuroKey
            preferences[Keys.ALLOW_NON_PUBLIC_FALLBACK] = settings.allowNonPublicFallback
            preferences[Keys.MAX_PUBLIC_DISTANCE_METERS] = settings.maxPublicDistanceMeters
        }
    }
}
