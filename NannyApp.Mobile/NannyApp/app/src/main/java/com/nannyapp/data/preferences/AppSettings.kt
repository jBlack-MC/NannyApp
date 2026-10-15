package com.nannyapp.data.preferences

import android.content.Context
import android.content.res.Configuration
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * Device preferences that belong to the phone, not the account: they survive signing out.
 * Stored in SharedPreferences because the language must be read synchronously before the
 * first screen is drawn (see [localized]).
 */
@Singleton
class AppSettings @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    private val theme = MutableStateFlow(
        runCatching { ThemeMode.valueOf(prefs.getString(KEY_THEME, null) ?: "") }.getOrDefault(ThemeMode.SYSTEM),
    )
    val themeMode: StateFlow<ThemeMode> = theme.asStateFlow()

    private val backgroundSync = MutableStateFlow(prefs.getBoolean(KEY_BACKGROUND_SYNC, true))
    val backgroundSyncEnabled: StateFlow<Boolean> = backgroundSync.asStateFlow()

    /** Empty means "follow the phone". */
    val languageTag: String get() = prefs.getString(KEY_LANGUAGE, "") ?: ""

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME, mode.name).apply()
        theme.value = mode
    }

    fun setLanguageTag(tag: String) {
        prefs.edit().putString(KEY_LANGUAGE, tag).apply()
    }

    fun setBackgroundSync(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BACKGROUND_SYNC, enabled).apply()
        backgroundSync.value = enabled
    }

    companion object {
        private const val FILE = "nannyapp_settings"
        private const val KEY_THEME = "theme_mode"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_BACKGROUND_SYNC = "background_sync"

        /** Language tags the app has translations for, in the order the picker shows them. */
        val SUPPORTED_LANGUAGES = listOf("", "en", "af", "zu")

        /** Wraps a context so its resources use the chosen language; called from attachBaseContext. */
        fun localized(base: Context): Context {
            val tag = base.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY_LANGUAGE, "") ?: ""
            if (tag.isBlank()) return base
            val config = Configuration(base.resources.configuration)
            config.setLocale(Locale.forLanguageTag(tag))
            return base.createConfigurationContext(config)
        }
    }
}
