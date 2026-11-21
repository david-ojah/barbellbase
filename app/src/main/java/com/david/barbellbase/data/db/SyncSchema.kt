package com.david.barbellbase.data.db

import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Schema additions and SQLite triggers that keep [SyncChangeEntity] populated.
 *
 * Every synced table gets insert/update/delete triggers. The delete trigger
 * writes a tombstone so removals can propagate to other devices.
 */
object SyncSchema {

    val syncedTables = listOf(
        "exercises",
        "routines",
        "routine_days",
        "routine_exercises",
        "sessions",
        "set_entries",
        "body_metrics",
        "habit_logs",
    )

    private fun createTableSql(): String = """
        CREATE TABLE IF NOT EXISTS sync_changes (
            entityType TEXT NOT NULL,
            entityId TEXT NOT NULL,
            changedAt INTEGER NOT NULL,
            deleted INTEGER NOT NULL,
            PRIMARY KEY(entityType, entityId)
        )
    """.trimIndent()

    fun triggerSql(table: String): List<String> = listOf(
        """
        CREATE TRIGGER IF NOT EXISTS sync_${table}_insert AFTER INSERT ON $table BEGIN
            INSERT OR REPLACE INTO sync_changes(entityType, entityId, changedAt, deleted)
            VALUES ('$table', CAST(NEW.id AS TEXT), CAST(strftime('%s','now') AS INTEGER) * 1000, 0);
        END
        """.trimIndent(),
        """
        CREATE TRIGGER IF NOT EXISTS sync_${table}_update AFTER UPDATE ON $table BEGIN
            INSERT OR REPLACE INTO sync_changes(entityType, entityId, changedAt, deleted)
            VALUES ('$table', CAST(NEW.id AS TEXT), CAST(strftime('%s','now') AS INTEGER) * 1000, 0);
        END
        """.trimIndent(),
        """
        CREATE TRIGGER IF NOT EXISTS sync_${table}_delete AFTER DELETE ON $table BEGIN
            INSERT OR REPLACE INTO sync_changes(entityType, entityId, changedAt, deleted)
            VALUES ('$table', CAST(OLD.id AS TEXT), CAST(strftime('%s','now') AS INTEGER) * 1000, 1);
        END
        """.trimIndent(),
    )

    fun createTriggers(db: SupportSQLiteDatabase) {
        syncedTables.forEach { table ->
            triggerSql(table).forEach(db::execSQL)
        }
    }

    fun backfill(db: SupportSQLiteDatabase) {
        val now = System.currentTimeMillis()
        syncedTables.forEach { table ->
            db.execSQL(
                "INSERT OR IGNORE INTO sync_changes(entityType, entityId, changedAt, deleted) " +
                    "SELECT '$table', CAST(id AS TEXT), $now, 0 FROM $table",
            )
        }
    }

    fun migrateFrom1To2(db: SupportSQLiteDatabase) {
        db.execSQL(createTableSql())

        // Habit log primary keys become UUID strings so records are stable across devices.
        db.execSQL("ALTER TABLE habit_logs RENAME TO habit_logs_legacy")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS habit_logs (
                id TEXT NOT NULL,
                kind TEXT NOT NULL,
                amountMl INTEGER,
                durationMin INTEGER,
                loggedAt INTEGER NOT NULL,
                source TEXT NOT NULL,
                note TEXT NOT NULL,
                PRIMARY KEY(id)
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO habit_logs (id, kind, amountMl, durationMin, loggedAt, source, note)
            SELECT lower(hex(randomblob(16))), kind, amountMl, durationMin, loggedAt, source, note
            FROM habit_logs_legacy
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE habit_logs_legacy")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_habit_logs_kind ON habit_logs (kind)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_habit_logs_loggedAt ON habit_logs (loggedAt)")

        createTriggers(db)
        backfill(db)
    }
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) = SyncSchema.migrateFrom1To2(db)
}

val SyncDatabaseCallback = object : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        SyncSchema.createTriggers(db)
    }
}
