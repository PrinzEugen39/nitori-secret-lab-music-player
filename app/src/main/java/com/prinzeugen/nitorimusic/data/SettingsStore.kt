package com.prinzeugen.nitorimusic.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.prinzeugen.nitorimusic.domain.model.ListOptions
import com.prinzeugen.nitorimusic.domain.model.SortType
import com.prinzeugen.nitorimusic.domain.model.ViewLayout
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {

    private val shuffleSpreadKey = floatPreferencesKey("shuffle_spread")
    private val albumsSortKey = stringPreferencesKey("albums_sort")
    private val albumsReverseKey = booleanPreferencesKey("albums_reverse")
    private val albumsLayoutKey = stringPreferencesKey("albums_layout")

    private val tracksSortKey = stringPreferencesKey("tracks_sort")
    private val tracksReverseKey = booleanPreferencesKey("tracks_reverse")
    private val tracksLayoutKey = stringPreferencesKey("tracks_layout")

    /** 0 = acak murni, 1 = sebar merata per artis. */
    val shuffleSpread: Flow<Float> =
        context.dataStore.data.map { it[shuffleSpreadKey] ?: DEFAULT_SPREAD }

    suspend fun setShuffleSpread(value: Float) {
        context.dataStore.edit { it[shuffleSpreadKey] = value.coerceIn(0f, 1f) }
    }

    fun getAlbumsOptions(): Flow<ListOptions> = context.dataStore.data.map { prefs ->
        ListOptions(
            sortType = prefs[albumsSortKey]?.let { runCatching { SortType.valueOf(it) }.getOrNull() } ?: SortType.BY_NAME,
            reverse = prefs[albumsReverseKey] ?: false,
            layout = prefs[albumsLayoutKey]?.let { runCatching { ViewLayout.valueOf(it) }.getOrNull() } ?: ViewLayout.GRID,
        )
    }

    suspend fun setAlbumsOptions(options: ListOptions) {
        context.dataStore.edit { prefs ->
            prefs[albumsSortKey] = options.sortType.name
            prefs[albumsReverseKey] = options.reverse
            prefs[albumsLayoutKey] = options.layout.name
        }
    }

    fun getTracksOptions(): Flow<ListOptions> = context.dataStore.data.map { prefs ->
        ListOptions(
            sortType = prefs[tracksSortKey]?.let { runCatching { SortType.valueOf(it) }.getOrNull() } ?: SortType.BY_NAME,
            reverse = prefs[tracksReverseKey] ?: false,
            layout = prefs[tracksLayoutKey]?.let { runCatching { ViewLayout.valueOf(it) }.getOrNull() } ?: ViewLayout.LIST,
        )
    }

    suspend fun setTracksOptions(options: ListOptions) {
        context.dataStore.edit { prefs ->
            prefs[tracksSortKey] = options.sortType.name
            prefs[tracksReverseKey] = options.reverse
            prefs[tracksLayoutKey] = options.layout.name
        }
    }

    companion object {
        const val DEFAULT_SPREAD = 0.7f
    }
}

