package com.androidharness.app.data.db

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfessorTrainingMigrationTest {
    @Test
    fun migrations30Through33CreateCompatibleTrainingSchema() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(null)
            .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) = Unit
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            })
            .build()
        val helper = FrameworkSQLiteOpenHelperFactory().create(configuration)
        try {
            val db = helper.writableDatabase
            db.execSQL(
                "CREATE TABLE professor_training_lessons (" +
                    "lessonId TEXT NOT NULL PRIMARY KEY, rule TEXT NOT NULL, scopePlatform TEXT, " +
                    "accountId TEXT, conversationId TEXT, state TEXT NOT NULL, createdAt INTEGER NOT NULL, " +
                    "updatedAt INTEGER NOT NULL, approvedAt INTEGER)"
            )
            db.execSQL(
                "CREATE TABLE professor_training_turns (" +
                    "turnId TEXT NOT NULL PRIMARY KEY, role TEXT NOT NULL, text TEXT NOT NULL, createdAt INTEGER NOT NULL)"
            )

            AppDatabase.MIGRATION_30_31.migrate(db)
            AppDatabase.MIGRATION_31_32.migrate(db)
            AppDatabase.MIGRATION_32_33.migrate(db)

            assertTrue(columns(db, "professor_training_lessons").contains("revision"))
            assertEquals(
                setOf("turnId", "role", "text", "createdAt", "scopePlatform", "accountId", "conversationId"),
                columns(db, "professor_training_turns"),
            )
            assertEquals(
                setOf("auditId", "lessonId", "action", "revision", "ruleBefore", "ruleAfter", "occurredAt"),
                columns(db, "professor_training_audit"),
            )
            assertTrue(indexExists(db, "index_professor_training_audit_lessonId"))
            assertTrue(indexExists(db, "index_professor_training_audit_occurredAt"))
        } finally {
            helper.close()
        }
    }

    private fun columns(db: SupportSQLiteDatabase, table: String): Set<String> =
        db.query("PRAGMA table_info(`$table`)").use { cursor ->
            buildSet {
                val nameColumn = cursor.getColumnIndexOrThrow("name")
                while (cursor.moveToNext()) add(cursor.getString(nameColumn))
            }
        }

    private fun indexExists(db: SupportSQLiteDatabase, name: String): Boolean =
        db.query(
            "SELECT name FROM sqlite_master WHERE type='index' AND name=?",
            arrayOf(name),
        ).use { it.moveToFirst() }
}
