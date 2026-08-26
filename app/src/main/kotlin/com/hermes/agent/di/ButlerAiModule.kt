package com.hermes.agent.di

import com.hermes.agent.data.butler.ButlerAiProviderImpl
import com.jeeves.core.settings.ai.ButlerAiProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class ButlerAiModule {

    @Binds
    abstract fun bindButlerAiProvider(
        impl: ButlerAiProviderImpl
    ): ButlerAiProvider
}
