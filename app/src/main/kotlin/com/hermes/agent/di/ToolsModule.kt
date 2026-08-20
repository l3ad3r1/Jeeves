package com.hermes.agent.di

import com.hermes.agent.data.tool.ToolRegistryImpl
import com.hermes.agent.domain.agent.AgentFeature
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

    @Multibinds
    abstract fun bindFeatures(): Set<AgentFeature>
}

/**
 * Phase 2 tool wiring.
 *
 * Tools are discovered via Hilt multibinding ([Set<Tool>]) where each tool
 * binds itself with `@Binds @IntoSet` in its own file, as well as via
 * modular [AgentFeature] contributions ([Set<AgentFeature>]). Tools are sorted
 * deterministically (category then name) before registration.
 */
@Module
@InstallIn(SingletonComponent::class)
object ToolsModule {

    @Provides
    @Singleton
    fun provideToolRegistry(
        tools: Set<@JvmSuppressWildcards Tool>,
        features: Set<@JvmSuppressWildcards AgentFeature> = emptySet(),
    ): ToolRegistry {
        val registry = ToolRegistryImpl()
        val featureTools = features
            .sortedBy { it.id }
            .flatMap { it.tools() }
        val featureToolNames = featureTools.mapTo(mutableSetOf()) { it.descriptor.name }
        val allTools = (tools.filterNot { it.descriptor.name in featureToolNames } + featureTools)
            .distinctBy { it.descriptor.name }
            .sortedWith(compareBy({ it.descriptor.category }, { it.descriptor.name }))
        allTools.forEach(registry::register)
        return registry
    }
}
