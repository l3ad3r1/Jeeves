package com.hermes.agent.data.local

import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HermesDatabaseMigrationTest {
    private var helper: SupportSQLiteOpenHelper? = null

    @After
    fun closeDatabase() {
        helper?.close()
    }

    @Test
    fun `migration 8 to 9 creates durable plan schema`() {
        val configuration = SupportSQLiteOpenHelper.Configuration.builder(
            ApplicationProvider.getApplicationContext(),
        ).name(null).callback(object : SupportSQLiteOpenHelper.Callback(8) {
            override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) = Unit
            override fun onUpgrade(
                db: androidx.sqlite.db.SupportSQLiteDatabase,
                oldVersion: Int,
                newVersion: Int,
            ) = Unit
        }).build()
        helper = FrameworkSQLiteOpenHelperFactory().create(configuration)
        val database = checkNotNull(helper).writableDatabase

        HermesDatabase.MIGRATION_8_9.migrate(database)

        val tables = mutableSetOf<String>()
        database.query(
            "SELECT name FROM sqlite_master WHERE type = 'table' " +
                "AND name IN ('execution_plans', 'execution_steps')",
        ).use { cursor ->
            while (cursor.moveToNext()) tables += cursor.getString(0)
        }
        assertEquals(setOf("execution_plans", "execution_steps"), tables)

        val indices = mutableSetOf<String>()
        database.query("PRAGMA index_list('execution_steps')").use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            while (cursor.moveToNext()) indices += cursor.getString(nameIndex)
        }
        assertTrue("index_execution_steps_planId" in indices)
        assertTrue("index_execution_steps_planId_position" in indices)

        database.query("PRAGMA foreign_key_list('execution_steps')").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("execution_plans", cursor.getString(cursor.getColumnIndexOrThrow("table")))
        }
    }

    @Test
    fun `migration 9 to 10 creates the activity ledger`() {
        val configuration = SupportSQLiteOpenHelper.Configuration.builder(
            ApplicationProvider.getApplicationContext(),
        ).name(null).callback(object : SupportSQLiteOpenHelper.Callback(9) {
            override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) = Unit
            override fun onUpgrade(
                db: androidx.sqlite.db.SupportSQLiteDatabase,
                oldVersion: Int,
                newVersion: Int,
            ) = Unit
        }).build()
        helper = FrameworkSQLiteOpenHelperFactory().create(configuration)
        val database = checkNotNull(helper).writableDatabase

        HermesDatabase.MIGRATION_9_10.migrate(database)

        val tables = mutableSetOf<String>()
        database.query(
            "SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'activity_ledger'",
        ).use { cursor ->
            while (cursor.moveToNext()) tables += cursor.getString(0)
        }
        assertEquals(setOf("activity_ledger"), tables)

        val indices = mutableSetOf<String>()
        database.query("PRAGMA index_list('activity_ledger')").use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            while (cursor.moveToNext()) indices += cursor.getString(nameIndex)
        }
        assertTrue("index_activity_ledger_timestamp" in indices)
    }

    @Test
    fun `migration 14 to 15 adds nullable message evidence state`() {
        val configuration = SupportSQLiteOpenHelper.Configuration.builder(
            ApplicationProvider.getApplicationContext(),
        ).name(null).callback(object : SupportSQLiteOpenHelper.Callback(14) {
            override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE messages (id TEXT NOT NULL PRIMARY KEY)")
                db.execSQL("INSERT INTO messages (id) VALUES ('existing-message')")
            }

            override fun onUpgrade(
                db: androidx.sqlite.db.SupportSQLiteDatabase,
                oldVersion: Int,
                newVersion: Int,
            ) = Unit
        }).build()
        helper = FrameworkSQLiteOpenHelperFactory().create(configuration)
        val database = checkNotNull(helper).writableDatabase

        HermesDatabase.MIGRATION_14_15.migrate(database)

        val columns = mutableSetOf<String>()
        database.query("PRAGMA table_info('messages')").use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            while (cursor.moveToNext()) columns += cursor.getString(nameIndex)
        }
        assertTrue("evidence_state" in columns)
        database.query("SELECT evidence_state FROM messages WHERE id = 'existing-message'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertTrue(cursor.isNull(0))
        }
    }

    @Test
    fun `migration 18 to 19 adds attachment columns to messages`() {
        val configuration = SupportSQLiteOpenHelper.Configuration.builder(
            ApplicationProvider.getApplicationContext(),
        ).name(null).callback(object : SupportSQLiteOpenHelper.Callback(18) {
            override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE messages (
                        id TEXT NOT NULL PRIMARY KEY,
                        conversation_id TEXT NOT NULL,
                        role TEXT NOT NULL,
                        content TEXT NOT NULL,
                        agent_role TEXT,
                        timestamp INTEGER NOT NULL,
                        tokens INTEGER NOT NULL DEFAULT 0,
                        is_on_device INTEGER NOT NULL DEFAULT 1,
                        evidence_state TEXT
                    )
                    """.trimIndent()
                )
                db.execSQL("INSERT INTO messages (id, conversation_id, role, content, timestamp) VALUES ('msg-1', 'conv-1', 'user', 'hello', 1000)")
            }

            override fun onUpgrade(
                db: androidx.sqlite.db.SupportSQLiteDatabase,
                oldVersion: Int,
                newVersion: Int,
            ) = Unit
        }).build()
        helper = FrameworkSQLiteOpenHelperFactory().create(configuration)
        val database = checkNotNull(helper).writableDatabase

        HermesDatabase.MIGRATION_18_19.migrate(database)

        val columns = mutableSetOf<String>()
        database.query("PRAGMA table_info('messages')").use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            while (cursor.moveToNext()) columns += cursor.getString(nameIndex)
        }
        assertTrue("attachment_uri" in columns)
        assertTrue("attachment_mime_type" in columns)

        database.query("SELECT attachment_uri, attachment_mime_type FROM messages WHERE id = 'msg-1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertTrue(cursor.isNull(0))
            assertTrue(cursor.isNull(1))
        }
    }
}
