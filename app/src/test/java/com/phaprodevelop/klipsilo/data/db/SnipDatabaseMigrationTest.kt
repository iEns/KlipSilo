package com.phaprodevelop.klipsilo.data.db

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SnipDatabaseMigrationTest {
    private val openedDatabases = mutableListOf<SnipDatabase>()

    @After
    fun tearDown() {
        openedDatabases.forEach { it.close() }
        openedDatabases.clear()
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase(DB_FROM_V6)
        context.deleteDatabase(DB_FROM_V5)
    }

    @Test
    fun migrate_fromVersion6_addsPinLocationAndLockColumns_preservesRow() {
        // Arrange: v6 has no isPinned / lat / lng / isLocked (those land in 7, 8, 10).
        createPrePinDatabase(DB_FROM_V6, version = 6, insertLegacyRow = true)

        // Act
        val migrated = openWithMigrations(DB_FROM_V6)
        val sqlite = migrated.openHelper.readableDatabase

        // Assert
        assertEquals(10, sqlite.version)
        assertPinnedLocationAndLockColumns(tableInfo(sqlite))
        sqlite.query(
            "SELECT isPinned, isLocked, latitude, longitude, contentType, previewText " +
                "FROM clipboard_items WHERE id = 1",
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
            assertEquals(0, cursor.getInt(1))
            assertTrue(cursor.isNull(2))
            assertTrue(cursor.isNull(3))
            assertEquals("TEXT", cursor.getString(4))
            assertEquals("hello", cursor.getString(5))
        }
    }

    @Test
    fun migrate_fromVersion5_noOpMigrations_stillOpensAt10() {
        // Arrange: 5→6 is a no-op (same pre-pin table). 8→9 is also a no-op later in the chain.
        createPrePinDatabase(DB_FROM_V5, version = 5, insertLegacyRow = true)

        // Act
        val migrated = openWithMigrations(DB_FROM_V5)
        val sqlite = migrated.openHelper.readableDatabase

        // Assert
        assertTrue(migrated.isOpen)
        assertEquals(10, sqlite.version)
        assertPinnedLocationAndLockColumns(tableInfo(sqlite))
        sqlite.query("SELECT COUNT(*) FROM clipboard_items").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
        }
    }

    private fun openWithMigrations(name: String): SnipDatabase {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db =
            Room.databaseBuilder(context, SnipDatabase::class.java, name)
                .addMigrations(*SnipDatabase.ALL_MIGRATIONS)
                .allowMainThreadQueries()
                .build()
        openedDatabases += db
        // Force open so migrations run before assertions.
        db.openHelper.writableDatabase
        return db
    }

    private fun createPrePinDatabase(
        name: String,
        version: Int,
        insertLegacyRow: Boolean,
    ) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase(name)
        val openHelper =
            FrameworkSQLiteOpenHelperFactory().create(
                SupportSQLiteOpenHelper.Configuration.builder(context)
                    .name(name)
                    .callback(
                        object : SupportSQLiteOpenHelper.Callback(version) {
                            override fun onCreate(db: SupportSQLiteDatabase) {
                                db.execSQL(PRE_PIN_CREATE_TABLE)
                                if (insertLegacyRow) {
                                    db.execSQL(PRE_PIN_INSERT_ROW)
                                }
                            }

                            override fun onUpgrade(
                                db: SupportSQLiteDatabase,
                                oldVersion: Int,
                                newVersion: Int,
                            ) {
                                error("Unexpected upgrade $oldVersion -> $newVersion")
                            }
                        },
                    )
                    .build(),
            )
        openHelper.writableDatabase.close()
        openHelper.close()
        val dbFile = context.getDatabasePath(name)
        assertTrue("Expected legacy database at ${dbFile.absolutePath}", dbFile.exists())
    }

    private fun tableInfo(db: SupportSQLiteDatabase): Map<String, ColumnMeta> {
        val columns = mutableMapOf<String, ColumnMeta>()
        db.query("PRAGMA table_info(clipboard_items)").use { cursor ->
            val nameIdx = cursor.getColumnIndex("name")
            val typeIdx = cursor.getColumnIndex("type")
            val notNullIdx = cursor.getColumnIndex("notnull")
            val defaultIdx = cursor.getColumnIndex("dflt_value")
            while (cursor.moveToNext()) {
                val defaultValue =
                    if (cursor.isNull(defaultIdx)) {
                        null
                    } else {
                        cursor.getString(defaultIdx)
                    }
                columns[cursor.getString(nameIdx)] =
                    ColumnMeta(
                        type = cursor.getString(typeIdx),
                        notNull = cursor.getInt(notNullIdx) != 0,
                        defaultValue = defaultValue,
                    )
            }
        }
        return columns
    }

    private fun assertPinnedLocationAndLockColumns(columns: Map<String, ColumnMeta>) {
        val isPinned = columns["isPinned"]
        assertNotNull(isPinned)
        assertEquals("INTEGER", isPinned!!.type.uppercase())
        assertTrue(isPinned.notNull)
        assertEquals("0", isPinned.defaultValue)

        val latitude = columns["latitude"]
        assertNotNull(latitude)
        assertEquals("REAL", latitude!!.type.uppercase())
        assertFalse(latitude.notNull)
        assertNull(latitude.defaultValue)

        val longitude = columns["longitude"]
        assertNotNull(longitude)
        assertEquals("REAL", longitude!!.type.uppercase())
        assertFalse(longitude.notNull)
        assertNull(longitude.defaultValue)

        val isLocked = columns["isLocked"]
        assertNotNull(isLocked)
        assertEquals("INTEGER", isLocked!!.type.uppercase())
        assertTrue(isLocked.notNull)
        assertEquals("0", isLocked.defaultValue)

        assertTrue(columns.containsKey("id"))
        assertTrue(columns.containsKey("contentType"))
        assertTrue(columns.containsKey("textContent"))
        assertTrue(columns.containsKey("filePath"))
        assertTrue(columns.containsKey("previewText"))
        assertTrue(columns.containsKey("timestamp"))
    }

    private data class ColumnMeta(
        val type: String,
        val notNull: Boolean,
        val defaultValue: String?,
    )

    companion object {
        private const val DB_FROM_V6 = "migration-v6-to-10"
        private const val DB_FROM_V5 = "migration-v5-to-10"

        private val PRE_PIN_CREATE_TABLE =
            """
            CREATE TABLE clipboard_items (
              id INTEGER NOT NULL PRIMARY KEY,
              contentType TEXT NOT NULL,
              textContent TEXT,
              filePath TEXT,
              previewText TEXT NOT NULL,
              timestamp INTEGER NOT NULL
            )
            """.trimIndent()

        private const val PRE_PIN_INSERT_ROW =
            "INSERT INTO clipboard_items " +
                "(id, contentType, textContent, filePath, previewText, timestamp) " +
                "VALUES (1, 'TEXT', 'hello', NULL, 'hello', 123)"
    }
}
