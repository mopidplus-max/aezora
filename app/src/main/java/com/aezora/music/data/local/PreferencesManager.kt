package com.aezora.music.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.aezora.music.domain.model.AppTheme
import com.aezora.music.domain.model.AudioQuality
import com.aezora.music.domain.model.SpeedMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "aezora_prefs")

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val ds = context.dataStore

    companion object {
        val YANDEX_TOKEN = stringPreferencesKey("yandex_token")
        val APP_THEME = stringPreferencesKey("app_theme")
        val DOWNLOAD_QUALITY = stringPreferencesKey("download_quality")
        val STREAM_QUALITY = stringPreferencesKey("stream_quality")
        val SPEED_MODE = stringPreferencesKey("speed_mode")
        val PERSIST_SPEED = booleanPreferencesKey("persist_speed")
        val EQ_PRESET = stringPreferencesKey("eq_preset")
        val DOWNLOAD_TO_FOLDER = booleanPreferencesKey("download_to_folder")
    }

    val yandexToken: Flow<String> = ds.data.map { it[YANDEX_TOKEN] ?: "" }
    val appTheme: Flow<AppTheme> = ds.data.map {
        AppTheme.valueOf(it[APP_THEME] ?: AppTheme.BLUE_PURPLE.name)
    }
    val downloadQuality: Flow<AudioQuality> = ds.data.map {
        AudioQuality.valueOf(it[DOWNLOAD_QUALITY] ?: AudioQuality.NORMAL.name)
    }
    val streamQuality: Flow<AudioQuality> = ds.data.map {
        AudioQuality.valueOf(it[STREAM_QUALITY] ?: AudioQuality.NORMAL.name)
    }
    val speedMode: Flow<SpeedMode> = ds.data.map {
        SpeedMode.valueOf(it[SPEED_MODE] ?: SpeedMode.NORMAL.name)
    }
    val persistSpeed: Flow<Boolean> = ds.data.map { it[PERSIST_SPEED] ?: false }
    val eqPreset: Flow<String> = ds.data.map { it[EQ_PRESET] ?: "Flat" }

    suspend fun setYandexToken(token: String) {
        ds.edit { it[YANDEX_TOKEN] = token }
    }

    suspend fun setAppTheme(theme: AppTheme) {
        ds.edit { it[APP_THEME] = theme.name }
    }

    suspend fun setDownloadQuality(q: AudioQuality) {
        ds.edit { it[DOWNLOAD_QUALITY] = q.name }
    }

    suspend fun setStreamQuality(q: AudioQuality) {
        ds.edit { it[STREAM_QUALITY] = q.name }
    }

    suspend fun setSpeedMode(mode: SpeedMode) {
        ds.edit { it[SPEED_MODE] = mode.name }
    }

    suspend fun setPersistSpeed(persist: Boolean) {
        ds.edit { it[PERSIST_SPEED] = persist }
    }

    suspend fun setEqPreset(name: String) {
        ds.edit { it[EQ_PRESET] = name }
    }
}
