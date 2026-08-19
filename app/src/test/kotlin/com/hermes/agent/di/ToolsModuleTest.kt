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
        val registry = ToolsModule.provideToolRegistry(emptySet())
        assertEquals(0, registry.all().size)
    }
}
