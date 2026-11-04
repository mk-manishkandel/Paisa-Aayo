package io.github.mkmanishkandel.paisaaayo.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import io.github.mkmanishkandel.paisaaayo.data.Settings
import io.github.mkmanishkandel.paisaaayo.data.SpeechLanguageSetting
import io.github.mkmanishkandel.paisaaayo.parser.SpeechLanguage

object AppLanguage {
    const val NEPALI = "ne"
    const val ENGLISH = "en"

    /** Applies the per-app UI language (recreates activities). */
    fun apply(tag: String) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
    }

    /** Current UI language, usable from services where AppCompat may not be initialised. */
    fun current(context: Context, settings: Settings? = null): String {
        settings?.appLanguage?.takeIf { it.isNotEmpty() }?.let { return it }
        AppCompatDelegate.getApplicationLocales().get(0)?.language?.let { return it }
        return context.resources.configuration.locales[0]?.language ?: ENGLISH
    }

    fun isNepali(context: Context, settings: Settings? = null) = current(context, settings) == NEPALI

    fun speechLanguage(context: Context, settings: Settings): SpeechLanguage = when (settings.speechLanguage) {
        SpeechLanguageSetting.NEPALI -> SpeechLanguage.NEPALI
        SpeechLanguageSetting.ENGLISH -> SpeechLanguage.ENGLISH
        SpeechLanguageSetting.SAME_AS_APP ->
            if (isNepali(context, settings)) SpeechLanguage.NEPALI else SpeechLanguage.ENGLISH
    }
}
