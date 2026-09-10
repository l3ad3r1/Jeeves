package com.l3ad3r1.octojotter.data.repository

import com.l3ad3r1.octojotter.data.local.NoteDao
import com.l3ad3r1.octojotter.data.local.NoteEntity
import com.l3ad3r1.octojotter.data.remote.GistFile
import com.l3ad3r1.octojotter.data.remote.GistResponse
import com.l3ad3r1.octojotter.data.remote.GithubApiService
import com.l3ad3r1.octojotter.data.remote.TokenManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException
import java.security.MessageDigest

class NoteRepositorySyncSecurityTest {
    private val dao = mockk<NoteDao>(relaxed = true)
    private val github = mockk<GithubApiService>()
    private val tokenManager = mockk<TokenManager>()
    private val repository = NoteRepository(dao, github, tokenManager)

    init {
        every { tokenManager.getToken() } returns "token"
    }

    @Test
    fun `protected notes are never sent to Gist even if a stale DAO result contains them`() = runTest {
        val locked = NoteEntity(id = 1, title = "locked", content = "secret", locked = true, needsSync = true)
        val encrypted = NoteEntity(id = 2, title = "encrypted", content = "secret", encrypted = true, needsSync = true)
        val ordinary = NoteEntity(id = 3, title = "ordinary", content = "safe", needsSync = true)
        coEvery { dao.getNotesToSync() } returns listOf(locked, encrypted, ordinary)
        coEvery { github.createGist(any(), any()) } returns Response.success(
            GistResponse(id = "new", description = null, updatedAt = null, files = emptyMap())
        )

        assertTrue(repository.pushToGithub().isSuccess)

        coVerify(exactly = 1) { github.createGist(any(), match { request ->
            request.files.values.single().content == "safe"
        }) }
        coVerify(exactly = 0) { github.createGist(any(), match { request ->
            request.files.values.any { it.content == "secret" }
        }) }
    }

    @Test
    fun `local only gist edit remains queued when remote is unchanged`() = runTest {
        val base = "A"
        val local = NoteEntity(
            id = 4,
            gistId = "gist",
            title = "note",
            content = "B",
            needsSync = true,
            lastSyncedContentHash = sha256(base)
        )
        val file = GistFile(filename = "note.md", type = "text/markdown", language = null, rawUrl = null, size = 1, content = base)
        coEvery { github.getGists(any()) } returns Response.success(listOf(GistResponse("gist", null, null, mapOf("note.md" to file))))
        coEvery { github.getGist(any(), "gist") } returns Response.success(GistResponse("gist", null, null, mapOf("note.md" to file)))
        coEvery { dao.getNoteByGistId("gist") } returns local

        assertTrue(repository.pullFromGithub().isSuccess)

        coVerify(exactly = 0) { dao.update(any()) }
    }

    @Test
    fun `unresolved conflict is never overwritten by a later pull`() = runTest {
        val local = NoteEntity(
            id = 5,
            gistId = "gist",
            title = "note",
            content = "local B",
            conflictState = "CONFLICT",
            conflictedRemoteContent = "remote C",
            lastSyncedContentHash = sha256("A")
        )
        val file = GistFile(filename = "note.md", type = "text/markdown", language = null, rawUrl = null, size = 8, content = "remote C")
        coEvery { github.getGists(any()) } returns Response.success(listOf(GistResponse("gist", null, null, mapOf("note.md" to file))))
        coEvery { github.getGist(any(), "gist") } returns Response.success(GistResponse("gist", null, null, mapOf("note.md" to file)))
        coEvery { dao.getNoteByGistId("gist") } returns local

        assertTrue(repository.pullFromGithub().isSuccess)

        coVerify(exactly = 0) { dao.update(any()) }
    }

    @Test
    fun `failed remote deletion retains the tombstone`() = runTest {
        val tombstone = NoteEntity(id = 6, gistId = "gist", title = "deleted", content = "x", deletedAt = 1, pendingRemoteDelete = true)
        coEvery { dao.getPendingRemoteDeletes() } returns listOf(tombstone)
        coEvery { github.deleteGist(any(), "gist") } throws IOException("offline")

        val result = repository.syncPendingRemoteDeletes()

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { dao.delete(tombstone) }
    }

    @Test
    fun `successful remote deletion removes the tombstone`() = runTest {
        val tombstone = NoteEntity(id = 7, gistId = "gist", title = "deleted", content = "x", deletedAt = 1, pendingRemoteDelete = true)
        coEvery { dao.getPendingRemoteDeletes() } returns listOf(tombstone)
        coEvery { github.deleteGist(any(), "gist") } returns Response.success(Unit)

        assertTrue(repository.syncPendingRemoteDeletes().isSuccess)

        coVerify(exactly = 1) { dao.delete(tombstone) }
    }

    private fun sha256(content: String): String =
        MessageDigest.getInstance("SHA-256").digest(content.toByteArray()).joinToString("") { "%02x".format(it) }
}
