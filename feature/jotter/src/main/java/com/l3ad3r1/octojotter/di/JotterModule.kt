package com.l3ad3r1.octojotter.di

import android.content.Context
import com.l3ad3r1.octojotter.data.local.AppDatabase
import com.l3ad3r1.octojotter.data.local.NoteDao
import com.l3ad3r1.octojotter.data.local.PluginDao
import com.l3ad3r1.octojotter.data.remote.GithubApiService
import com.l3ad3r1.octojotter.data.remote.RetrofitClient
import com.l3ad3r1.octojotter.data.remote.TokenManager
import com.l3ad3r1.octojotter.data.repository.NoteRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Contributes Octo Jotter's singletons to the host's single Hilt graph
 * (`HermesApp` is the only `@HiltAndroidApp`).
 *
 * These bindings are additive: Jotter's own UI still constructs `NoteRepository`
 * directly in `NoteViewModel` and `SyncWorker`, so behaviour is unchanged. They exist
 * so the Hermes agent can inject `NoteRepository` for the Phase 6 `create_note` tool.
 *
 * Deliberately NOT bound here:
 *  - `OkHttpClient`. `:app`'s NetworkModule already provides one into
 *    `SingletonComponent`; binding a second would be a duplicate-binding compile error.
 *    Jotter keeps its own private client inside [RetrofitClient] (its own interceptors
 *    and timeouts). Consolidating the two clients is a separate, behaviour-changing
 *    decision — see PROGRESS.md.
 *
 * Jotter keeps its own Room database file (`AppDatabase`), separate from
 * `HermesDatabase`; see the Phase 5 decision in PROGRESS.md.
 */
@Module
@InstallIn(SingletonComponent::class)
object JotterModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        AppDatabase.getDatabase(context)

    @Provides
    @Singleton
    fun provideNoteDao(db: AppDatabase): NoteDao = db.noteDao()

    @Provides
    @Singleton
    fun providePluginDao(db: AppDatabase): PluginDao = db.pluginDao()

    @Provides
    @Singleton
    fun provideTokenManager(@ApplicationContext context: Context): TokenManager =
        TokenManager(context)

    @Provides
    @Singleton
    fun provideGithubApiService(): GithubApiService = RetrofitClient.githubApiService

    @Provides
    @Singleton
    fun provideNoteRepository(
        noteDao: NoteDao,
        githubApiService: GithubApiService,
        tokenManager: TokenManager,
    ): NoteRepository = NoteRepository(noteDao, githubApiService, tokenManager)
}
