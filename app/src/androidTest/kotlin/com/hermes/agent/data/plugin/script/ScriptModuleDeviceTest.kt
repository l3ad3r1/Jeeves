package com.hermes.agent.data.plugin.script

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hermes.agent.data.local.HermesDatabase
import com.hermes.agent.data.repository.BookmarkRepositoryImpl
import com.hermes.agent.data.repository.NotesRepositoryImpl
import com.hermes.agent.data.repository.TodoRepositoryImpl
import com.hermes.agent.domain.model.TaskPriority
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * On-device proof that a published script module actually works end to end:
 * the real manifest JSON from the module repo -> the real [ScriptPluginEngine]
 * sandbox -> the real [ScriptPluginHostImpl] -> real Room / real network.
 *
 * Deliberately does not involve the LLM. The agent only decides *whether* to
 * call a tool; everything below that decision is what these tests cover, so a
 * missing or broken model cannot make the module look broken.
 *
 * Uses an in-memory database rather than the installed app's, so running this
 * never reads or writes the device owner's real notes, todos, or bookmarks.
 */
@RunWith(AndroidJUnit4::class)
class ScriptModuleDeviceTest {

    private lateinit var db: HermesDatabase
    private lateinit var engine: ScriptPluginEngine

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            HermesDatabase::class.java,
        ).build()
        engine = ScriptPluginEngine()
        engine.host = ScriptPluginHostImpl(
            notes = NotesRepositoryImpl(db.noteDao()),
            todos = TodoRepositoryImpl(db.todoTaskDao()),
            bookmarks = BookmarkRepositoryImpl(db.bookmarkDao()),
            okHttpClient = OkHttpClient(),
        )
    }

    @After
    fun tearDown() = db.close()

    private fun load(manifestJson: String): ScriptPluginManifest {
        val manifest = ScriptPluginManifest.json.decodeFromString(
            ScriptPluginManifest.serializer(), manifestJson,
        )
        val failures = runBlocking {
            engine.reload(
                listOf(
                    ScriptPluginEngine.PluginSpec(
                        id = manifest.id,
                        source = manifest.main,
                        permissions = manifest.permissions.toSet(),
                    ),
                ),
            )
        }
        assertTrue("module failed to load: $failures", failures.isEmpty())
        return manifest
    }

    /**
     * Reads from the *test* APK's assets. [ApplicationProvider] hands back the
     * app-under-test context, whose asset table does not contain anything this
     * test module ships.
     */
    private fun asset(name: String): String =
        InstrumentationRegistry.getInstrumentation().context
            .assets.open(name).bufferedReader().use { it.readText() }

    @Test
    fun dailyDigest_reads_real_room_data_through_the_host() = runBlocking {
        // Seed through the same repositories the host reads from.
        val todos = TodoRepositoryImpl(db.todoTaskDao())
        val notes = NotesRepositoryImpl(db.noteDao())
        todos.create(
            title = "Overdue device-test task",
            priority = TaskPriority.HIGH,
            dueDateMs = System.currentTimeMillis() - 86_400_000,
        )
        notes.create(title = "Device-test starred note").also { notes.toggleStar(it.id) }

        val manifest = load(asset("modules/daily-digest.json"))
        val out = engine.execute(manifest.id, "daily_digest", emptyMap()).getOrThrow()

        println("DEVICE-TEST daily_digest >>> $out")
        // Proves data.read returned the seeded rows, not "" from a null host.
        assertTrue("digest missing todo: $out", out.contains("Overdue device-test task"))
        assertTrue("digest missing note: $out", out.contains("Device-test starred note"))
        assertTrue("digest missing overdue count: $out", out.contains("1 overdue"))
    }

    @Test
    fun weather_fetches_live_data_over_the_network_through_the_host() = runBlocking {
        val manifest = load(asset("modules/weather.json"))
        val out = engine.execute(
            manifest.id, "weather",
            mapOf("city" to Json.parseToJsonElement("\"Paris\"")),
        ).getOrThrow()

        println("DEVICE-TEST weather >>> $out")
        // Proves hermes.http.get reached open-meteo; a null host would have
        // produced the "bad response from the geocoder" branch instead.
        assertTrue("expected Paris in: $out", out.contains("Paris"))
        assertTrue("expected celsius reading in: $out", out.contains("\u00B0C"))
        assertTrue("unexpected failure branch: $out", !out.contains("Could not"))
    }
}
