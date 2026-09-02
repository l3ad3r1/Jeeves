package com.hermes.agent.data.agent.agents

import com.hermes.agent.data.plugin.InProcessPluginSandbox
import com.hermes.agent.data.plugins.WeatherPlugin
import com.hermes.agent.data.tool.ToolRegistryImpl
import com.hermes.agent.domain.plugin.LogLevel
import com.hermes.agent.domain.plugin.PluginContext
import com.hermes.agent.domain.plugin.PluginLifecycleResult
import com.hermes.agent.domain.tool.Tool
import com.hermes.agent.domain.tool.ToolDescriptor
import com.hermes.agent.domain.tool.ToolRegistry
import com.hermes.agent.domain.tool.ToolResult
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonElement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the per-agent tool capability access policy:
 * - Every registered tool is granted to at least one agent.
 * - All 5 agent roles receive the exact expected tool sets.
 * - Runtime tools loaded dynamically via InProcessPluginSandbox reach agent descriptor lists.
 * - Dangerous tool families (shell, termux, app_*, device_settings) are restricted to authorized roles.
 */
class AgentToolAccessTest {

    private class StubTool(
        override val descriptor: ToolDescriptor,
    ) : Tool {
        override suspend fun execute(arguments: Map<String, JsonElement>): ToolResult =
            ToolResult.ok("ok")
    }

    private fun stub(name: String, category: String, capabilities: Set<String> = emptySet()) = StubTool(
        ToolDescriptor(
            name = name,
            description = "stub",
            parameters = emptyList(),
            category = category,
            capabilities = capabilities,
        )
    )

    private fun sampleRegistry(): ToolRegistry {
        val registry = ToolRegistryImpl()
        val tools = listOf(
            stub("get_current_datetime", "information", setOf("time")),
            stub("calculator", "productivity", setOf("calculator")),
            stub("web_search", "information", setOf("web")),
            stub("web_fetch", "information", setOf("web")),
            stub("notify", "communication", setOf("notification")),
            stub("device_settings", "device", setOf("device_settings")),
            stub("notes", "productivity", setOf("notes")),
            stub("search_conversations", "information", setOf("conversation_search")),
            stub("calendar", "productivity", setOf("calendar")),
            stub("alarm", "device", setOf("device_alarm")),
            stub("navigation", "device", setOf("navigation")),
            stub("communication", "communication", setOf("phone")),
            stub("contact_lookup", "communication", setOf("contacts")),
            stub("media_control", "device", setOf("media")),
            stub("device_control", "device", setOf("device_control")),
            stub("skill_manager", "productivity", setOf("skills")),
            stub("memory", "productivity", setOf("user_memory")),
            stub("scheduler", "productivity", setOf("scheduler")),
            stub("shell", "system", setOf("shell")),
            stub("termux", "system", setOf("termux")),
            stub("todo", "productivity", setOf("common", "todo")),
            stub("speak", "communication", setOf("voice")),
            stub("clarify", "communication", setOf("common", "clarify")),
            stub("delegate", "productivity", setOf("delegate")),
            stub("generate_image", "creative", setOf("media_generation")),
            stub("app_launch", "automation", setOf("app_automation")),
            stub("app_analyze_screen", "automation", setOf("app_automation")),
            stub("app_tap", "automation", setOf("app_automation")),
            stub("app_swipe", "automation", setOf("app_automation")),
            stub("app_type", "automation", setOf("app_automation")),
            stub("create_note", "productivity", setOf("documents")),
            stub("search_notes", "productivity", setOf("documents")),
            stub("set_alarm", "productivity", setOf("notes_and_reminders")),
            stub("bookmarks", "productivity", setOf("bookmarks")),
            stub("mood", "productivity", setOf("mood")),
        )
        tools.forEach(registry::register)
        return registry
    }

    private val agents = listOf(
        ConversationalAgent(), ProductivityAgent(), ResearchAgent(),
        DeviceControlAgent(), CreativeAgent(),
    )

    private fun grantedAnywhere(tool: String): Boolean {
        val registry = sampleRegistry()
        return agents.any { agent -> agent.availableTools(registry).any { it.name == tool } }
    }

    @Test
    fun `new v0_7_x tools are granted to at least one agent`() {
        for (tool in listOf("todo", "clarify", "delegate", "speak", "generate_image", "web_fetch")) {
            assertTrue("'$tool' is not granted to any agent — the LLM can never call it", grantedAnywhere(tool))
        }
    }

    @Test
    fun `conversational agent exposes the core new tools`() {
        val names = ConversationalAgent().availableTools(sampleRegistry()).map { it.name }
        for (tool in listOf("todo", "clarify", "delegate", "speak", "generate_image", "web_fetch")) {
            assertTrue("conversational agent missing '$tool'", names.contains(tool))
        }
    }

    @Test
    fun `creative agent exposes generate_image`() {
        val names = CreativeAgent().availableTools(sampleRegistry()).map { it.name }
        assertTrue(names.contains("generate_image"))
    }

    @Test
    fun `cross-feature tools are granted to at least one agent`() {
        for (tool in listOf("create_note", "set_alarm", "search_notes")) {
            assertTrue("'$tool' is not granted to any agent — the LLM can never call it", grantedAnywhere(tool))
        }
    }

    @Test
    fun `all five roles receive their exact expected tool counts and sets`() {
        val registry = sampleRegistry()

        val convTools = ConversationalAgent().availableTools(registry).map { it.name }.toSet()
        assertEquals(33, convTools.size)
        assertFalse("ConversationalAgent must not have calendar", convTools.contains("calendar"))
        assertFalse("ConversationalAgent must not have device_settings", convTools.contains("device_settings"))

        val prodTools = ProductivityAgent().availableTools(registry).map { it.name }.toSet()
        assertEquals(22, prodTools.size)
        assertTrue("ProductivityAgent must have calendar", prodTools.contains("calendar"))
        assertTrue("ProductivityAgent must have create_note", prodTools.contains("create_note"))
        assertTrue("ProductivityAgent must have set_alarm", prodTools.contains("set_alarm"))
        assertTrue("ProductivityAgent must have search_notes", prodTools.contains("search_notes"))

        val resTools = ResearchAgent().availableTools(registry).map { it.name }.toSet()
        assertEquals(11, resTools.size)
        val expectedRes = setOf("todo", "clarify", "web_search", "web_fetch", "search_conversations", "memory", "notes", "skill_manager", "calculator", "delegate", "bookmarks")
        assertEquals(expectedRes, resTools)

        val devTools = DeviceControlAgent().availableTools(registry).map { it.name }.toSet()
        assertEquals(19, devTools.size)
        assertTrue("DeviceControlAgent must have device_settings", devTools.contains("device_settings"))
        assertTrue("DeviceControlAgent must have shell", devTools.contains("shell"))
        assertTrue("DeviceControlAgent must have app_launch", devTools.contains("app_launch"))

        val creativeTools = CreativeAgent().availableTools(registry).map { it.name }.toSet()
        assertEquals(11, creativeTools.size)
        val expectedCreative = setOf("todo", "clarify", "memory", "notes", "search_conversations", "skill_manager", "generate_image", "web_search", "web_fetch", "speak", "bookmarks")
        assertEquals(expectedCreative, creativeTools)
    }

    @Test
    fun `dangerous tool families are only accessible to authorized roles`() {
        val registry = sampleRegistry()
        val dangerousTools = listOf("shell", "termux", "app_launch", "app_analyze_screen", "app_tap", "app_swipe", "app_type", "device_settings")

        val prodTools = ProductivityAgent().availableTools(registry).map { it.name }
        val resTools = ResearchAgent().availableTools(registry).map { it.name }
        val creativeTools = CreativeAgent().availableTools(registry).map { it.name }

        for (tool in dangerousTools) {
            assertFalse("ProductivityAgent must not have access to '$tool'", prodTools.contains(tool))
            assertFalse("ResearchAgent must not have access to '$tool'", resTools.contains(tool))
            assertFalse("CreativeAgent must not have access to '$tool'", creativeTools.contains(tool))
        }

        val devTools = DeviceControlAgent().availableTools(registry).map { it.name }
        for (tool in dangerousTools) {
            assertTrue("DeviceControlAgent must have access to '$tool'", devTools.contains(tool))
        }
    }

    @Test
    fun `runtime tool registered via InProcessPluginSandbox reaches agent descriptor list`() = runTest {
        val registry = ToolRegistryImpl()
        val sandbox = InProcessPluginSandbox(registry)
        val weatherPlugin = WeatherPlugin()

        val context = object : PluginContext {
            override fun log(tag: String, level: LogLevel, message: String, throwable: Throwable?) {}
            override suspend fun hostSetting(key: String): String? = null
            override fun hostAppVersion(): Int = 1
        }

        val result = sandbox.load(weatherPlugin, context)
        assertEquals(PluginLifecycleResult.Success, result)

        val convTools = ConversationalAgent().availableTools(registry).map { it.name }
        assertTrue("Conversational agent must reach runtime plugin tool 'weather_get'", convTools.contains("weather_get"))
    }

    /** ConversationalAgent and ProductivityAgent must reach the cross-feature tools. */
    @Test
    fun `agents are granted the cross-feature tools`() {
        val registry = sampleRegistry()
        for (agent in listOf(ConversationalAgent(), ProductivityAgent())) {
            val granted = agent.availableTools(registry).map { it.name }
            for (tool in listOf("create_note", "set_alarm")) {
                assertTrue("${agent.role} is not granted '$tool'", granted.contains(tool))
            }
        }
    }

    @Test
    fun `openclaw tool grants are strictly scoped`() {
        val registry = ToolRegistryImpl()
        registry.register(stub("take_photo", "device", setOf("camera", "deferrable")))
        registry.register(stub("standing_orders", "automation", setOf("standing_orders", "deferrable")))
        registry.register(stub("read_notifications", "system", setOf("notifications_read", "deferrable")))
        registry.register(stub("post_notification", "system", setOf("notifications_post", "deferrable")))
        registry.register(stub("presence", "device", setOf("presence", "deferrable")))

        val conv = ConversationalAgent().availableTools(registry).map { it.name }.toSet()
        val prod = ProductivityAgent().availableTools(registry).map { it.name }.toSet()
        val research = ResearchAgent().availableTools(registry).map { it.name }.toSet()
        val dev = DeviceControlAgent().availableTools(registry).map { it.name }.toSet()
        val creative = CreativeAgent().availableTools(registry).map { it.name }.toSet()

        // take_photo: CONVERSATIONAL & DEVICE_CONTROL only
        assertTrue("take_photo in CONVERSATIONAL", conv.contains("take_photo"))
        assertTrue("take_photo in DEVICE_CONTROL", dev.contains("take_photo"))
        assertFalse("take_photo NOT in PRODUCTIVITY", prod.contains("take_photo"))
        assertFalse("take_photo NOT in RESEARCH", research.contains("take_photo"))
        assertFalse("take_photo NOT in CREATIVE", creative.contains("take_photo"))

        // standing_orders: CONVERSATIONAL only
        assertTrue("standing_orders in CONVERSATIONAL", conv.contains("standing_orders"))
        assertFalse("standing_orders NOT in PRODUCTIVITY", prod.contains("standing_orders"))
        assertFalse("standing_orders NOT in RESEARCH", research.contains("standing_orders"))
        assertFalse("standing_orders NOT in DEVICE_CONTROL", dev.contains("standing_orders"))
        assertFalse("standing_orders NOT in CREATIVE", creative.contains("standing_orders"))

        // read_notifications: CONVERSATIONAL & PRODUCTIVITY only
        assertTrue("read_notifications in CONVERSATIONAL", conv.contains("read_notifications"))
        assertTrue("read_notifications in PRODUCTIVITY", prod.contains("read_notifications"))
        assertFalse("read_notifications NOT in RESEARCH", research.contains("read_notifications"))
        assertFalse("read_notifications NOT in DEVICE_CONTROL", dev.contains("read_notifications"))
        assertFalse("read_notifications NOT in CREATIVE", creative.contains("read_notifications"))

        // post_notification: CONVERSATIONAL & PRODUCTIVITY only
        assertTrue("post_notification in CONVERSATIONAL", conv.contains("post_notification"))
        assertTrue("post_notification in PRODUCTIVITY", prod.contains("post_notification"))
        assertFalse("post_notification NOT in RESEARCH", research.contains("post_notification"))
        assertFalse("post_notification NOT in DEVICE_CONTROL", dev.contains("post_notification"))
        assertFalse("post_notification NOT in CREATIVE", creative.contains("post_notification"))

        // presence: CONVERSATIONAL & PRODUCTIVITY only
        assertTrue("presence in CONVERSATIONAL", conv.contains("presence"))
        assertTrue("presence in PRODUCTIVITY", prod.contains("presence"))
        assertFalse("presence NOT in RESEARCH", research.contains("presence"))
        assertFalse("presence NOT in DEVICE_CONTROL", dev.contains("presence"))
        assertFalse("presence NOT in CREATIVE", creative.contains("presence"))
    }

    // Removed `each new tool name appears in prompt of every role granted it`:
    // agent prompts no longer re-list tools (they arrive as a function schema per
    // turn), and `openclaw tool grants are strictly scoped` above already covers
    // "which role reaches which tool" against the registry — the real contract.
}

