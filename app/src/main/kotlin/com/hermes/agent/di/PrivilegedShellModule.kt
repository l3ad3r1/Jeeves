package com.hermes.agent.di

import com.hermes.agent.domain.device.NoOpPrivilegedShellBackend
import com.hermes.agent.domain.device.PrivilegedShellBackend
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PrivilegedShellModule {

    @Binds
    @Singleton
    abstract fun bindPrivilegedShellBackend(impl: NoOpPrivilegedShellBackend): PrivilegedShellBackend
}
