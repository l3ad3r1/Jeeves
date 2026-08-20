package com.hermes.agent.di

import com.hermes.agent.domain.tool.Tool
import com.hermes.agent.domain.tool.ToolDescriptor
import com.hermes.agent.domain.tool.ToolResult
import kotlinx.serialization.json.JsonElement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ToolsModuleTest {

    private class StubTool(
        override val descriptor: ToolDescriptor,
    ) : Tool {
        override suspend fun execute(arguments: Map<String, JsonElement>): ToolResult =
            ToolResult.ok("ok")
    }

    private fun stub(name: String, category: String) = StubTool(
        ToolDescriptor(
            name = name,
            description = "Stub tool",
            parameters = emptyList(),
            category = category,
        )
    )

    @Test
    fun provideToolRegistry_registersAllToolsAndSortsByCategoryThenName() {
        val tools = setOf<Tool>(
            stub("zeta", "information"),
            stub("alpha", "information"),
            stub("beta", "automation"),
            stub("gamma", "device"),
            stub("aardvark", "device"),
        )

        val registry = ToolsModule.provideToolRegistry(tools)

        assertEquals(5, registry.all().size)
        assertNotNull(registry.byName("zeta"))
        assertNotNull(registry.byName("alpha"))
        assertNotNull(registry.byName("beta"))
        assertNotNull(registry.byName("gamma"))
        assertNotNull(registry.byName("aardvark"))

        val sortedNames = registry.all().map { it.descriptor.name }
        val expectedNames = listOf(
            "beta",     // automation
            "aardvark", // device
            "gamma",    // device
            "alpha",    // information
            "zeta",     // information
        )
        assertEquals(expectedNames, sortedNames)
    }

    @Test
    fun provideToolRegistry_emptySetReturnsEmptyRegistry() {
        val registry = ToolsModule.provideToolRegistry(emptySet(), emptySet())
        assertEquals(0, registry.all().size)
    }

    @Test
    fun provideToolRegistry_registersToolsContributedByAgentFeatures() {
        val directTools = setOf<Tool>(
            stub("direct_tool", "productivity"),
        )
        val feature = object : com.hermes.agent.domain.agent.AgentFeature {
            override val id: String = "test_feature"
            override fun tools(): List<Tool> = listOf(
                stub("feature_tool_b", "information"),
                stub("feature_tool_a", "information"),
            )
            override fun promptFragment(): String? = null
            override fun backupContributions(): List<com.hermes.agent.domain.agent.BackupContribution> = emptyList()
            override fun entries(): List<com.hermes.agent.domain.agent.NavEntry> = emptyList()
        }

        val registry = ToolsModule.provideToolRegistry(directTools, setOf(feature))

        assertEquals(3, registry.all().size)
        val sortedNames = registry.all().map { it.descriptor.name }
        assertEquals(listOf("feature_tool_a", "feature_tool_b", "direct_tool"), sortedNames)
    }

    @Test
    fun provideToolRegistry_featureToolOverridesSameNamedCoreTool() {
        val coreSpeak = stub("speak", "communication")
        val featureSpeak = stub("speak", "feature")
        val feature = object : com.hermes.agent.domain.agent.AgentFeature {
            override val id: String = "butler"
            override fun tools(): List<Tool> = listOf(featureSpeak)
            override fun promptFragment(): String? = null
            override fun backupContributions(): List<com.hermes.agent.domain.agent.BackupContribution> = emptyList()
            override fun entries(): List<com.hermes.agent.domain.agent.NavEntry> = emptyList()
        }

        val registry = ToolsModule.provideToolRegistry(setOf(coreSpeak), setOf(feature))

        assertEquals(1, registry.all().size)
        assertEquals(featureSpeak, registry.byName("speak"))
    }
}
