package io.github.mkmanishkandel.paisaaayo.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Which language announcements are spoken in. */
enum class SpeechLanguageSetting { SAME_AS_APP, NEPALI, ENGLISH }

data class Settings(
    val onboardingDone: Boolean = false,
    /** "ne" or "en"; empty means "follow the phone". Mirrors the per-app locale for background use. */
    val appLanguage: String = "",
    val speechLanguage: SpeechLanguageSetting = SpeechLanguageSetting.SAME_AS_APP,
    val useRecordedVoice: Boolean = true,
    val announceDebits: Boolean = false,
    val announceSource: Boolean = false,
    val raiseVolume: Boolean = true,
    val keepAliveNotification: Boolean = true,
    val readSms: Boolean = true,
    val mutedPackages: Set<String> = emptySet(),
    val retentionDays: Int = 90,
    val appLock: Boolean = false,
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val onboardingDone = booleanPreferencesKey("onboarding_done")
        val appLanguage = stringPreferencesKey("app_language")
        val speechLanguage = stringPreferencesKey("speech_language")
        val useRecordedVoice = booleanPreferencesKey("use_recorded_voice")
        val announceDebits = booleanPreferencesKey("announce_debits")
        val announceSource = booleanPreferencesKey("announce_source")
        val raiseVolume = booleanPreferencesKey("raise_volume")
        val keepAlive = booleanPreferencesKey("keep_alive_notification")
        val readSms = booleanPreferencesKey("read_sms")
        val mutedPackages = stringSetPreferencesKey("muted_packages")
        val retentionDays = intPreferencesKey("retention_days")
        val appLock = booleanPreferencesKey("app_lock")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { p ->
        val defaults = Settings()
        Settings(
            onboardingDone = p[Keys.onboardingDone] ?: defaults.onboardingDone,
            appLanguage = p[Keys.appLanguage] ?: defaults.appLanguage,
            speechLanguage = p[Keys.speechLanguage]
                ?.let { runCatching { SpeechLanguageSetting.valueOf(it) }.getOrNull() }
                ?: defaults.speechLanguage,
            useRecordedVoice = p[Keys.useRecordedVoice] ?: defaults.useRecordedVoice,
            announceDebits = p[Keys.announceDebits] ?: defaults.announceDebits,
            announceSource = p[Keys.announceSource] ?: defaults.announceSource,
            raiseVolume = p[Keys.raiseVolume] ?: defaults.raiseVolume,
            keepAliveNotification = p[Keys.keepAlive] ?: defaults.keepAliveNotification,
            readSms = p[Keys.readSms] ?: defaults.readSms,
            mutedPackages = p[Keys.mutedPackages] ?: defaults.mutedPackages,
            retentionDays = p[Keys.retentionDays] ?: defaults.retentionDays,
            appLock = p[Keys.appLock] ?: defaults.appLock,
        )
    }

    suspend fun setOnboardingDone(done: Boolean) = context.dataStore.edit { it[Keys.onboardingDone] = done }
    suspend fun setAppLanguage(tag: String) = context.dataStore.edit { it[Keys.appLanguage] = tag }
    suspend fun setSpeechLanguage(value: SpeechLanguageSetting) = context.dataStore.edit { it[Keys.speechLanguage] = value.name }
    suspend fun setUseRecordedVoice(value: Boolean) = context.dataStore.edit { it[Keys.useRecordedVoice] = value }
    suspend fun setAnnounceDebits(value: Boolean) = context.dataStore.edit { it[Keys.announceDebits] = value }
    suspend fun setAnnounceSource(value: Boolean) = context.dataStore.edit { it[Keys.announceSource] = value }
    suspend fun setRaiseVolume(value: Boolean) = context.dataStore.edit { it[Keys.raiseVolume] = value }
    suspend fun setKeepAlive(value: Boolean) = context.dataStore.edit { it[Keys.keepAlive] = value }
    suspend fun setReadSms(value: Boolean) = context.dataStore.edit { it[Keys.readSms] = value }
    suspend fun setRetentionDays(value: Int) = context.dataStore.edit { it[Keys.retentionDays] = value }
    suspend fun setAppLock(value: Boolean) = context.dataStore.edit { it[Keys.appLock] = value }

    suspend fun setPackageMuted(packageName: String, muted: Boolean) = context.dataStore.edit {
        val current = it[Keys.mutedPackages] ?: emptySet()
        it[Keys.mutedPackages] = if (muted) current + packageName else current - packageName
    }
}
