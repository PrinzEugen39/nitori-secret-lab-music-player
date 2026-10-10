package com.prinzeugen.nitorimusic.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
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
    private val albumsColumnsKey = intPreferencesKey("albums_columns")

    private val tracksSortKey = stringPreferencesKey("tracks_sort")
    private val tracksReverseKey = booleanPreferencesKey("tracks_reverse")
    private val tracksLayoutKey = stringPreferencesKey("tracks_layout")
    private val tracksColumnsKey = intPreferencesKey("tracks_columns")

    private val lastMediaIdKey = stringPreferencesKey("last_media_id")
    private val lastIndexKey = intPreferencesKey("last_index")
    private val lastPositionMsKey = longPreferencesKey("last_position_ms")

    private val audioFocusKey = booleanPreferencesKey("audio_focus_enabled")
    private val pauseOnHeadsetDisconnectKey = booleanPreferencesKey("pause_on_headset_disconnect")
    private val gaplessEnabledKey = booleanPreferencesKey("gapless_enabled")

    val audioFocusEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[audioFocusKey] ?: true }

    suspend fun setAudioFocusEnabled(enabled: Boolean) {
        context.dataStore.edit { it[audioFocusKey] = enabled }
    }

    val pauseOnHeadsetDisconnect: Flow<Boolean> =
        context.dataStore.data.map { it[pauseOnHeadsetDisconnectKey] ?: true }

    suspend fun setPauseOnHeadsetDisconnect(enabled: Boolean) {
        context.dataStore.edit { it[pauseOnHeadsetDisconnectKey] = enabled }
    }

    val gaplessEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[gaplessEnabledKey] ?: true }

    suspend fun setGaplessEnabled(enabled: Boolean) {
        context.dataStore.edit { it[gaplessEnabledKey] = enabled }
    }

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
            gridColumns = (prefs[albumsColumnsKey] ?: 2).coerceIn(2, 5),
        )
    }

    suspend fun setAlbumsOptions(options: ListOptions) {
        context.dataStore.edit { prefs ->
            prefs[albumsSortKey] = options.sortType.name
            prefs[albumsReverseKey] = options.reverse
            prefs[albumsLayoutKey] = options.layout.name
            prefs[albumsColumnsKey] = options.gridColumns.coerceIn(2, 5)
        }
    }

    fun getTracksOptions(): Flow<ListOptions> = context.dataStore.data.map { prefs ->
        ListOptions(
            sortType = prefs[tracksSortKey]?.let { runCatching { SortType.valueOf(it) }.getOrNull() } ?: SortType.BY_NAME,
            reverse = prefs[tracksReverseKey] ?: false,
            layout = prefs[tracksLayoutKey]?.let { runCatching { ViewLayout.valueOf(it) }.getOrNull() } ?: ViewLayout.LIST,
            gridColumns = (prefs[tracksColumnsKey] ?: 2).coerceIn(2, 5),
        )
    }

    suspend fun setTracksOptions(options: ListOptions) {
        context.dataStore.edit { prefs ->
            prefs[tracksSortKey] = options.sortType.name
            prefs[tracksReverseKey] = options.reverse
            prefs[tracksLayoutKey] = options.layout.name
            prefs[tracksColumnsKey] = options.gridColumns.coerceIn(2, 5)
        }
    }

    data class LastPlayback(
        val mediaId: String?,
        val index: Int,
        val positionMs: Long,
    )

    fun getLastPlayback(): Flow<LastPlayback> = context.dataStore.data.map { prefs ->
        LastPlayback(
            mediaId = prefs[lastMediaIdKey],
            index = prefs[lastIndexKey] ?: 0,
            positionMs = prefs[lastPositionMsKey] ?: 0L,
        )
    }

    suspend fun saveLastPlayback(mediaId: String?, index: Int, positionMs: Long) {
        context.dataStore.edit { prefs ->
            if (mediaId != null) prefs[lastMediaIdKey] = mediaId else prefs.remove(lastMediaIdKey)
            prefs[lastIndexKey] = index
            prefs[lastPositionMsKey] = positionMs
        }
    }

    companion object {
        const val DEFAULT_SPREAD = 0.7f
    }
}

