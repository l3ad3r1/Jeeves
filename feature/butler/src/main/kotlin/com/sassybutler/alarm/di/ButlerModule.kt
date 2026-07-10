package com.sassybutler.alarm.di

import android.content.Context
import com.sassybutler.alarm.AlarmScheduler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Contributes Sassy Butler's singletons to the host's single Hilt graph
 * (`HermesApp` is the only `@HiltAndroidApp`).
 *
 * Additive only: Butler's Activities and services keep constructing what they need
 * directly, so behaviour is unchanged. [AlarmScheduler] is bound so the Hermes agent
 * can inject it for the Phase 6 `set_alarm` tool.
 *
 * Deliberately NOT bound here:
 *  - `TtsEngine`. Its `init` block calls `initSession()`, which reads the entire ~92 MB
 *    ONNX model via `context.assets.open(...).readBytes()` and builds an ORT session
 *    synchronously. A `@Singleton @Provides` binding would therefore load 92 MB on
 *    whatever thread first injected it — potentially the main thread. Exposing TTS as a
 *    shared service requires a lazy, off-main-thread guard; that is Phase 6 work.
 *  - `AlarmStore`. It is a stateless Kotlin `object` whose functions already take a
 *    `Context` parameter, so there is nothing to inject.
 */
@Module
@InstallIn(SingletonComponent::class)
object ButlerModule {

    @Provides
    @Singleton
    fun provideAlarmScheduler(@ApplicationContext context: Context): AlarmScheduler =
        AlarmScheduler(context)
}
