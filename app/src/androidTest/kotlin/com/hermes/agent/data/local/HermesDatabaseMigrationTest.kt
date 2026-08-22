package com.hermes.agent.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Upgrade path for the on-device database.
 *
 * This exists because the failure it catches is invisible everywhere else. A
 * hand-written migration that does not match what Room generates for the
 * entities compiles cleanly, passes every unit test, and installs fine — it
 * only shows up when a real user's existing database is opened, as a failed
 * validation. A clean install cannot catch it either: Room builds the schema
 * from the entity list through `onCreate` and runs no migrations at all.
 *
 * `runMigrationsAndValidate` is the part that matters. It applies the migration
 * and compares the result against Room's own exported expectation, so a column,
 * affinity, nullability, primary key or index that drifts from the entity fails
 * here rather than on a phone.
 */
@RunWith(AndroidJUnit4::class)
class HermesDatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        HermesDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    /**
     * Jeeves takes the five productivity tables in a single step, where Hermes
     * spread the same tables over its versions 14 to 17. The apps own their own
     * schema versions on purpose; what has to match is the table shapes, and
     * those are shared because the entities live in `:core:persistence`.
     */
    @Test
    fun migrate15To16_addsTheProductivityTablesAndKeepsExistingData() {
        val conversationId = "productivity-migration-conversation"

        helper.createDatabase(TEST_DB, 15).use { db ->
            // A row written by the old schema. A destructive migration would
            // still leave a schema that validates, and only this would reveal
            // it, so it is the assertion that actually matters.
            db.execSQL(
                """
                INSERT INTO conversations
                    (id, title, created_at, updated_at, last_message_preview, message_count)
                VALUES (?, 'before productivity upgrade', 1, 1, 'survived', 0)
                """.trimIndent(),
                arrayOf<Any>(conversationId),
            )
        }

        // `validateDroppedTables = false`: the conversation search index is a
        // hand-built FTS4 virtual table, deliberately outside Room's entity
        // list, and the strict check rejects any table it does not recognise.
        // Every entity table is still validated, which is the point.
        val db = helper.runMigrationsAndValidate(
            TEST_DB, 16, false, HermesDatabase.MIGRATION_15_16,
        )

        db.query(
            "SELECT title FROM conversations WHERE id = ?", arrayOf<Any>(conversationId),
        ).use { cursor ->
            assertTrue("the pre-upgrade conversation must survive", cursor.moveToFirst())
            assertEquals("before productivity upgrade", cursor.getString(0))
        }

        listOf("notes", "todo_tasks", "calendar_events", "bookmarks", "mood_entries").forEach { table ->
            db.query(
                "SELECT name FROM sqlite_master WHERE type = 'table' AND name = ?",
                arrayOf<Any>(table),
            ).use { cursor ->
                assertTrue("migration must create $table", cursor.moveToFirst())
            }
        }

        // Declared on the entities as well. An index created only by the
        // migration fails Room's validation above; one declared only on the
        // entity is missing for every upgrading device while looking correct on
        // a fresh install.
        listOf(
            "index_notes_category",
            "index_notes_updatedAt",
            "index_todo_tasks_done",
            "index_todo_tasks_dueDateMs",
            "index_calendar_events_startMs",
            "index_bookmarks_url",
            "index_mood_entries_dateMs",
        ).forEach { index ->
            db.query(
                "SELECT name FROM sqlite_master WHERE type = 'index' AND name = ?",
                arrayOf<Any>(index),
            ).use { cursor ->
                assertTrue("migration must create $index", cursor.moveToFirst())
            }
        }
    }

    private companion object {
        const val TEST_DB = "hermes-migration-test.db"
    }
}
