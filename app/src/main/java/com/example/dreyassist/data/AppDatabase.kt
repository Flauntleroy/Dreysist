package com.example.dreyassist.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [TransaksiEntity::class, JournalEntity::class, ReminderEntity::class, MemoryEntity::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transaksiDao(): TransaksiDao
    abstract fun journalDao(): JournalDao
    abstract fun reminderDao(): ReminderDao
    abstract fun memoryDao(): MemoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private const val DB_NAME = "dreysist_database"

        // Migration from version 1 to 2
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add any schema changes from v1 to v2
            }
        }

        // Migration from version 2 to 3
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS reminder (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        content TEXT NOT NULL,
                        reminderTime INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        isCompleted INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
            }
        }

        // Migration from version 3 to 4
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS memory (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        content TEXT NOT NULL,
                        category TEXT NOT NULL DEFAULT '',
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        // Migration from version 4 to 5 - Add category to transaksi, recurrence to reminder
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add category column to transaksi
                database.execSQL("ALTER TABLE transaksi ADD COLUMN category TEXT NOT NULL DEFAULT 'OTHER'")
                // Add recurrence columns to reminder
                database.execSQL("ALTER TABLE reminder ADD COLUMN recurrenceType TEXT NOT NULL DEFAULT 'NONE'")
                database.execSQL("ALTER TABLE reminder ADD COLUMN recurrenceInterval INTEGER NOT NULL DEFAULT 1")
            }
        }

        // Migration from version 1 to 5 (full migration)
        private val MIGRATION_1_5 = object : Migration(1, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add category to transaksi
                database.execSQL("ALTER TABLE transaksi ADD COLUMN category TEXT NOT NULL DEFAULT 'OTHER'")
                // Create reminder table with all fields
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS reminder (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        content TEXT NOT NULL,
                        reminderTime INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        isCompleted INTEGER NOT NULL DEFAULT 0,
                        recurrenceType TEXT NOT NULL DEFAULT 'NONE',
                        recurrenceInterval INTEGER NOT NULL DEFAULT 1
                    )
                """.trimIndent())
                // Create memory table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS memory (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        content TEXT NOT NULL,
                        category TEXT NOT NULL DEFAULT '',
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        // Migration from version 2 to 5
        private val MIGRATION_2_5 = object : Migration(2, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE transaksi ADD COLUMN category TEXT NOT NULL DEFAULT 'OTHER'")
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS reminder (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        content TEXT NOT NULL,
                        reminderTime INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        isCompleted INTEGER NOT NULL DEFAULT 0,
                        recurrenceType TEXT NOT NULL DEFAULT 'NONE',
                        recurrenceInterval INTEGER NOT NULL DEFAULT 1
                    )
                """.trimIndent())
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS memory (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        content TEXT NOT NULL,
                        category TEXT NOT NULL DEFAULT '',
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        // Migration from version 3 to 5
        private val MIGRATION_3_5 = object : Migration(3, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE transaksi ADD COLUMN category TEXT NOT NULL DEFAULT 'OTHER'")
                database.execSQL("ALTER TABLE reminder ADD COLUMN recurrenceType TEXT NOT NULL DEFAULT 'NONE'")
                database.execSQL("ALTER TABLE reminder ADD COLUMN recurrenceInterval INTEGER NOT NULL DEFAULT 1")
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS memory (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        content TEXT NOT NULL,
                        category TEXT NOT NULL DEFAULT '',
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME
                )
                .addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_1_5,
                    MIGRATION_2_5,
                    MIGRATION_3_5
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }

        fun closeDatabase() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}

