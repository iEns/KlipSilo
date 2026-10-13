package com.phaprodevelop.klipsilo.data.db

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [ClipboardItem::class], version = 10, exportSchema = true)
abstract class SnipDatabase : RoomDatabase() {
    abstract fun clipboardItemDao(): ClipboardItemDao

    companion object {
        @Volatile
        private var instance: SnipDatabase? = null

        internal val MIGRATION_5_6 =
            object : Migration(5, 6) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    // No schema changes — version bump only
                }
            }

        internal val MIGRATION_6_7 =
            object : Migration(6, 7) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "ALTER TABLE clipboard_items ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0",
                    )
                }
            }

        internal val MIGRATION_7_8 =
            object : Migration(7, 8) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE clipboard_items ADD COLUMN latitude REAL")
                    db.execSQL("ALTER TABLE clipboard_items ADD COLUMN longitude REAL")
                }
            }

        internal val MIGRATION_8_9 =
            object : Migration(8, 9) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    // No schema changes — version bump only
                }
            }

        internal val MIGRATION_9_10 =
            object : Migration(9, 10) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "ALTER TABLE clipboard_items ADD COLUMN isLocked INTEGER NOT NULL DEFAULT 0",
                    )
                }
            }

        internal val ALL_MIGRATIONS =
            arrayOf(
                MIGRATION_5_6,
                MIGRATION_6_7,
                MIGRATION_7_8,
                MIGRATION_8_9,
                MIGRATION_9_10,
            )

        fun getInstance(context: Context): SnipDatabase {
            return instance ?: synchronized(this) {
                val builder =
                    Room.databaseBuilder(
                        context.applicationContext,
                        SnipDatabase::class.java,
                        "snip_database",
                    )
                        .addMigrations(*ALL_MIGRATIONS)
                val debuggable =
                    context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
                if (debuggable) {
                    builder.fallbackToDestructiveMigration(dropAllTables = true)
                }
                val instance = builder.build()
                this.instance = instance
                instance
            }
        }

        fun createInMemory(context: Context): SnipDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                SnipDatabase::class.java,
            )
                .allowMainThreadQueries()
                .addMigrations(*ALL_MIGRATIONS)
                .build()
        }

        fun resetInstance() {
            synchronized(this) {
                val current = instance
                instance = null
                current?.close()
            }
        }
    }
}
