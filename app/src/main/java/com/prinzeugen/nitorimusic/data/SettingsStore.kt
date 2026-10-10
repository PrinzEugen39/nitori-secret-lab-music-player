package com.prinzeugen.nitorimusic.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {

    private val shuffleSpreadKey = floatPreferencesKey("shuffle_spread")

    /** 0 = acak murni, 1 = sebar merata per artis. */
    val shuffleSpread: Flow<Float> =
        context.dataStore.data.map { it[shuffleSpreadKey] ?: DEFAULT_SPREAD }

    suspend fun setShuffleSpread(value: Float) {
        context.dataStore.edit { it[shuffleSpreadKey] = value.coerceIn(0f, 1f) }
    }

    companion object {
        const val DEFAULT_SPREAD = 0.7f
    }
}
