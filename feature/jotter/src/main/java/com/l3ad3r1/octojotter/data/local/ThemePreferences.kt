package com.l3ad3r1.octojotter.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.jeeves.core.settings.JeevesSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/** Legacy store — retained only so [ThemePreferences.migrateLegacyTheme] can read it once. */
val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_settings")

/**
 * The app's theme mode.
 *
 * Jotter is an integrated part of Jeeves, so the theme is no longer Jotter's private setting:
 * it reads and writes [JeevesSettings], the one settings store. The public API (a
 * `Flow<String>` defaulting to "system", and a suspend setter) is unchanged, so `NoteViewModel`
 * and `MainActivity` needed no edits.
 *
 * The old value lived in a DataStore, which has no synchronous read and so cannot take part in
 * [JeevesSettings]'s on-first-touch migration. [migrateLegacyTheme] moves it across once from a
 * coroutine; `HermesApp` calls it at startup, before any UI can observe the theme.
 */
class ThemePreferences(private val context: Context) {

    companion object {
        val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        const val THEME_SYSTEM = JeevesSettings.THEME_SYSTEM
        const val THEME_LIGHT = JeevesSettings.THEME_LIGHT
        const val THEME_DARK = JeevesSettings.THEME_DARK
    }

    val themeMode: Flow<String> = JeevesSettings.themeModeFlow(context)

    suspend fun setThemeMode(mode: String) = JeevesSettings.setThemeMode(context, mode)

    /**
     * One-time copy of the legacy `theme_settings` DataStore value into the unified store.
     *
     * Guarded by [JeevesSettings.hasThemeMode] rather than a "migrated" flag, so it can never
     * overwrite a choice the user has already made on the new settings screen. A user who never
     * picked a theme has no key in either store and stays on the system default.
     */
    suspend fun migrateLegacyTheme() {
        if (JeevesSettings.hasThemeMode(context)) return
        val legacy = runCatching {
            context.themeDataStore.data.first()[THEME_MODE_KEY]
        }.getOrNull() ?: return
        JeevesSettings.setThemeMode(context, legacy)
    }
}
