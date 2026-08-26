package com.hermes.agent.di

import com.hermes.agent.data.jotter.JotterAiProviderImpl
import com.jeeves.core.settings.ai.JotterAiProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class JotterAiModule {

    @Binds
    abstract fun bindJotterAiProvider(
        impl: JotterAiProviderImpl
    ): JotterAiProvider
}
