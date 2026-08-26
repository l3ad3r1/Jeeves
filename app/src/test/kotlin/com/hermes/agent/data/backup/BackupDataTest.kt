package com.hermes.agent.data.backup

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupDataTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `schema five serializes credentials as blank by default`() {
        val encoded = json.encodeToString(BackupData(settings = SettingsBackup(cloudEnabled = true)))
        val decoded = json.decodeFromString<BackupData>(encoded)

        assertEquals(5, decoded.schemaVersion)
        assertTrue(decoded.settings!!.cloudEnabled)
        assertTrue(decoded.settings!!.cloudApiKey.isBlank())
        assertTrue(decoded.settings!!.auxApiKey.isBlank())
    }

    @Test
    fun `legacy schema four note receives safe defaults`() {
        val decoded = json.decodeFromString<BackupData>(
            """{"schemaVersion":4,"notes":[{"title":"Old","content":"body"}]}""",
        )

        assertEquals(4, decoded.schemaVersion)
        val note = decoded.notes.single()
        assertNull(note.repository)
        assertNull(note.deletedAt)
        assertFalse(note.encrypted)
        assertFalse(note.needsSync)
    }
}
