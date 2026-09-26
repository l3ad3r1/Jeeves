package com.l3ad3r1.octojotter.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.l3ad3r1.octojotter.data.local.AppDatabase
import com.l3ad3r1.octojotter.data.local.RepoPreferences
import com.l3ad3r1.octojotter.data.remote.RetrofitClient
import com.l3ad3r1.octojotter.data.remote.TokenManager
import com.l3ad3r1.octojotter.data.repository.NoteRepository
import kotlinx.coroutines.flow.first

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)
        val noteDao = database.noteDao()
        val githubApiService = RetrofitClient.githubApiService
        val tokenManager = TokenManager(applicationContext)
        val repository = NoteRepository(noteDao, githubApiService, tokenManager)

        val steps = mutableListOf<suspend () -> kotlin.Result<*>>(
            { repository.syncPendingRemoteDeletes() },
            { repository.pullFromGithub() },
            { repository.pushToGithub() },
        )
        // Keep the selected repository in sync too, not just Gists — this is what
        // "Sync Now" already does, and repo-backed vault notes are the app's
        // primary use case, so they shouldn't only sync when the user remembers
        // to tap the button.
        val repoPath = RepoPreferences(applicationContext).selectedRepository.first()
        if (!repoPath.isNullOrBlank()) {
            steps += { repository.pullFromRepository(repoPath) }
            steps += { repository.pushToRepository(repoPath) }
        }

        // Stop at the first failure, as before.
        for (step in steps) {
            val error = step().exceptionOrNull() ?: continue
            val message = error.message.orEmpty()
            return if (message.contains("No GitHub token", ignoreCase = true) || message.contains("401", ignoreCase = true)) {
                Result.failure()
            } else {
                Result.retry()
            }
        }
        return Result.success()
    }
}
