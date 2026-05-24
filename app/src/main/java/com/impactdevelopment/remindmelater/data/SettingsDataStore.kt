package com.impactdevelopment.remindmelater.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    companion object {
        private val KEY_COMFORT_START = intPreferencesKey("comfort_start")
        private val KEY_COMFORT_END   = intPreferencesKey("comfort_end")
        private val KEY_HAS_ONBOARDED = booleanPreferencesKey("has_onboarded")
        private val KEY_TERMS_ACCEPTED = booleanPreferencesKey("terms_accepted")
    }

    val comfortStart: Flow<Int> = context.dataStore.data
        .map { it[KEY_COMFORT_START] ?: 9 }

    val comfortEnd: Flow<Int> = context.dataStore.data
        .map { it[KEY_COMFORT_END] ?: 21 }

    val hasOnboarded: Flow<Boolean> = context.dataStore.data
        .map { it[KEY_HAS_ONBOARDED] ?: false }

    val termsAccepted: Flow<Boolean> = context.dataStore.data
        .map { it[KEY_TERMS_ACCEPTED] ?: false }

    suspend fun saveComfortHours(start: Int, end: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_COMFORT_START] = start
            prefs[KEY_COMFORT_END]   = end
            prefs[KEY_HAS_ONBOARDED] = true
        }
    }

    suspend fun setTermsAccepted(accepted: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_TERMS_ACCEPTED] = accepted
        }
    }
}
