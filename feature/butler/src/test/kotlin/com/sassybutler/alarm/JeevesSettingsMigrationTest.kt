package com.sassybutler.alarm

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.jeeves.core.settings.JeevesSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class JeevesSettingsMigrationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun legacyButler() = context.getSharedPreferences("butler_prefs", Context.MODE_PRIVATE)
    private fun legacyVoice() = context.getSharedPreferences("voice_prefs", Context.MODE_PRIVATE)
    private fun unified() = context.getSharedPreferences(JeevesSettings.PREFS, Context.MODE_PRIVATE)

    @Before
    fun clearAllStores() {
        legacyButler().edit().clear().commit()
        legacyVoice().edit().clear().commit()
        unified().edit().clear().commit()
    }

    @Test
    fun `existing butler preferences survive the move to the unified store`() {
        legacyButler().edit()
            .putString("honorific", "Madam")
            .putInt("sass_level", 90)
            .putInt("snooze_minutes", 3)
            .putBoolean("birds_intro", false)
            .putBoolean("voice_enabled", false)
            .putBoolean("haptics", true)
            .putBoolean("snooze_commentary", false)
            .commit()
        legacyVoice().edit().putString("voice_name", "bf_emma").commit()

        // First touch through the public API triggers the migration.
        assertEquals("Madam", ButlerPrefs.honorific(context))
        assertEquals(90, ButlerPrefs.sassLevel(context))
        assertEquals(3, ButlerPrefs.snoozeMinutes(context))
        assertFalse(ButlerPrefs.birdsIntro(context))
        assertFalse(ButlerPrefs.voiceEnabled(context))
        assertTrue(ButlerPrefs.haptics(context))
        assertFalse(ButlerPrefs.snoozeCommentary(context))
        assertEquals("bf_emma", VoiceCatalog.selected(context))
    }

    @Test
    fun `a fresh install keeps the standalone apps' defaults`() {
        assertEquals("Sir", ButlerPrefs.honorific(context))
        assertEquals(45, ButlerPrefs.sassLevel(context))
        assertEquals(10, ButlerPrefs.snoozeMinutes(context))
        assertTrue(ButlerPrefs.birdsIntro(context))
        assertTrue(ButlerPrefs.voiceEnabled(context))
        assertFalse(ButlerPrefs.haptics(context))
        assertTrue(ButlerPrefs.snoozeCommentary(context))
    }

    @Test
    fun `unset legacy keys are not written into the unified store`() {
        legacyButler().edit().putString("honorific", "Boss").commit()

        assertEquals("Boss", ButlerPrefs.honorific(context))
        assertFalse(
            "sass_level was never set by the user and must not be materialised",
            unified().contains(JeevesSettings.KEY_SASS_LEVEL),
        )
        assertEquals("default still applies", 45, ButlerPrefs.sassLevel(context))
    }

    @Test
    fun `migration runs once and never clobbers a newer value`() {
        legacyButler().edit().putString("honorific", "Madam").commit()
        assertEquals("Madam", ButlerPrefs.honorific(context))   // migrates

        ButlerPrefs.setHonorific(context, "Boss")               // user changes it in Jeeves
        legacyButler().edit().putString("honorific", "Sir").commit() // stale legacy write

        assertEquals("Boss", ButlerPrefs.honorific(context))
    }

    @Test
    fun `writes through ButlerPrefs land in the unified store, not the legacy file`() {
        ButlerPrefs.setSnoozeMinutes(context, 7)

        assertEquals(7, unified().getInt(JeevesSettings.KEY_SNOOZE_MINUTES, -1))
        assertFalse(legacyButler().contains("snooze_minutes"))
    }

    @Test
    fun `sass level stays clamped to 0-100 through the delegate`() {
        ButlerPrefs.setSassLevel(context, 500)
        assertEquals(100, ButlerPrefs.sassLevel(context))
        ButlerPrefs.setSassLevel(context, -20)
        assertEquals(0, ButlerPrefs.sassLevel(context))
    }

    @Test
    fun `theme flow emits the current value and then updates`() = runTest {
        JeevesSettings.setThemeMode(context, JeevesSettings.THEME_DARK)
        assertEquals(JeevesSettings.THEME_DARK, JeevesSettings.themeModeFlow(context).first())
    }

    @Test
    fun `theme defaults to system when never chosen`() = runTest {
        assertFalse(JeevesSettings.hasThemeMode(context))
        assertEquals(JeevesSettings.THEME_SYSTEM, JeevesSettings.themeMode(context))
    }

    @Test
    fun `Butler's sheet and the unified settings screen read and write the same values`() {
        // Butler's sheet writes...
        ButlerPrefs.setSassLevel(context, 77)
        ButlerPrefs.setVoiceEnabled(context, false)
        VoiceCatalog.select(context, "bm_lewis")

        // ...the Hermes settings screen sees it.
        assertEquals(77, JeevesSettings.sassLevel(context))
        assertFalse(JeevesSettings.voiceEnabled(context))
        assertEquals("bm_lewis", JeevesSettings.voiceName(context, "bm_george"))

        // And the reverse: the settings screen writes...
        JeevesSettings.setSnoozeMinutes(context, 21)
        JeevesSettings.setHonorific(context, "Madam")

        // ...Butler's alarm path sees it.
        assertEquals(21, ButlerPrefs.snoozeMinutes(context))
        assertEquals("Madam", ButlerPrefs.honorific(context))
    }

    @Test
    fun `appearance font settings persist and flow from the unified store`() = runTest {
        JeevesSettings.setFontFamily(context, JeevesSettings.FONT_SERIF)
        JeevesSettings.setFontScalePercent(context, 115)

        assertEquals(JeevesSettings.FONT_SERIF, JeevesSettings.fontFamily(context))
        assertEquals(JeevesSettings.FONT_SERIF, JeevesSettings.fontFamilyFlow(context).first())
        assertEquals(115, JeevesSettings.fontScalePercent(context))
        assertEquals(115, JeevesSettings.fontScalePercentFlow(context).first())
    }

    @Test
    fun `appearance settings normalise invalid fonts and clamp font size`() {
        JeevesSettings.setFontFamily(context, "comic-sans")
        JeevesSettings.setFontScalePercent(context, 500)

        assertEquals(JeevesSettings.FONT_GEIST, JeevesSettings.fontFamily(context))
        assertEquals(JeevesSettings.MAX_FONT_SCALE_PERCENT, JeevesSettings.fontScalePercent(context))

        JeevesSettings.setFontScalePercent(context, -20)
        assertEquals(JeevesSettings.MIN_FONT_SCALE_PERCENT, JeevesSettings.fontScalePercent(context))
    }
}
