package com.l3ad3r1.octojotter.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NoteBackupExtensionsTest {

    @Test
    fun `locked and encrypted notes never enter a Gist backup`() {
        assertNull(NoteEntity(title = "Locked", content = "secret", locked = true).toBackupOrNull())
        assertNull(NoteEntity(title = "Encrypted", content = "cipher", encrypted = true).toBackupOrNull())
    }

    @Test
    fun `note backup preserves trash repository and sync metadata`() {
        val note = NoteEntity(
            title = "Repo note",
            content = "body",
            repository = "owner/repo",
            path = "notes/item.md",
            sha = "abc123",
            deletedAt = 1234L,
            pendingRemoteDelete = true,
            remoteUpdatedAt = "2026-07-14T00:00:00Z",
            lastSyncedContentHash = "hash",
            conflictState = "CONFLICT",
            conflictedRemoteContent = "remote body",
            conflictedRemoteModifiedAt = 5678L,
            needsSync = true,
            lastModifiedLocally = 9999L,
        )

        val restored = checkNotNull(note.toBackupOrNull()).toRestoredEntity()

        assertEquals(note.title, restored.title)
        assertEquals(note.content, restored.content)
        assertEquals(note.repository, restored.repository)
        assertEquals(note.path, restored.path)
        assertEquals(note.sha, restored.sha)
        assertEquals(note.deletedAt, restored.deletedAt)
        assertEquals(note.pendingRemoteDelete, restored.pendingRemoteDelete)
        assertEquals(note.remoteUpdatedAt, restored.remoteUpdatedAt)
        assertEquals(note.lastSyncedContentHash, restored.lastSyncedContentHash)
        assertEquals(note.conflictState, restored.conflictState)
        assertEquals(note.conflictedRemoteContent, restored.conflictedRemoteContent)
        assertEquals(note.conflictedRemoteModifiedAt, restored.conflictedRemoteModifiedAt)
        assertEquals(note.needsSync, restored.needsSync)
        assertEquals(note.lastModifiedLocally, restored.lastModifiedLocally)
    }
}
