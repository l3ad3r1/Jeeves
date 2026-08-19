package com.hermes.agent.di

import com.hermes.agent.data.tool.ToolRegistryImpl
import com.hermes.agent.domain.tool.Tool
import com.hermes.agent.domain.tool.ToolRegistry
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ToolsMultibindsModule {
    @Multibinds
    abstract fun bindTools(): Set<Tool>
}

/**
 * Phase 2 tool wiring.
 *
 * Tools are discovered via Hilt multibinding ([Set<Tool>]) where each tool
 * binds itself with `@Binds @IntoSet` in its own file. Tools are sorted
 * deterministically (category then name) before registration.
 */
@Module
@InstallIn(SingletonComponent::class)
object ToolsModule {

    @Provides
    @Singleton
    fun provideToolRegistry(
        tools: Set<@JvmSuppressWildcards Tool>,
    ): ToolRegistry {
        val registry = ToolRegistryImpl()
        tools.sortedWith(
            compareBy({ it.descriptor.category }, { it.descriptor.name })
        ).forEach(registry::register)
        return registry
    }
}

