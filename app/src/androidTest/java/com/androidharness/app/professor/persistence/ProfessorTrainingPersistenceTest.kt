package com.androidharness.app.professor.persistence

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.androidharness.app.data.db.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfessorTrainingPersistenceTest {
    private lateinit var database: AppDatabase
    private lateinit var dao: ProfessorPersistenceDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.professorPersistenceDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun lessonEditsAndDeletionRemainInAuditHistory() = runBlocking {
        dao.insertTrainingLesson(lesson("lesson-1", "Be concise"))
        assertEquals(1, dao.approveTrainingLessonWithAudit("lesson-1", 10L))
        assertEquals(1, dao.editTrainingLessonWithAudit("lesson-1", "Be clear and concise", 20L))
        dao.deleteTrainingLessonWithAudit("lesson-1", 30L)

        val events = dao.trainingAuditFlow("lesson-1").first()
        assertEquals(listOf("DELETED", "EDITED", "APPROVED"), events.map { it.action })
        assertEquals("Be concise", events[1].ruleBefore)
        assertEquals("Be clear and concise", events[1].ruleAfter)
        assertEquals(2, events[1].revision)
        assertEquals(null, dao.trainingLesson("lesson-1"))
    }

    @Test
    fun resetWritesAuditEventsBeforeRemovingLessons() = runBlocking {
        dao.insertTrainingLesson(lesson("lesson-reset", "Use a friendly tone"))
        dao.clearTrainingLessonsWithAudit(50L)

        val events = dao.trainingAuditFlow("lesson-reset").first()
        assertEquals(1, events.size)
        assertEquals("RESET", events.single().action)
        assertEquals("Use a friendly tone", events.single().ruleBefore)
        assertTrue(dao.trainingLessonsSnapshot().isEmpty())
    }

    @Test
    fun auditTimelineIsBoundedToLatestOneHundredEvents() = runBlocking {
        repeat(105) { index ->
            dao.insertTrainingAudit(
                ProfessorTrainingAuditEntity(
                    auditId = "audit-$index",
                    lessonId = "lesson-$index",
                    action = "APPROVED",
                    revision = 1,
                    ruleBefore = "rule",
                    ruleAfter = "rule",
                    occurredAt = index.toLong(),
                )
            )
        }

        val events = dao.trainingAuditFlowAll().first()
        assertEquals(100, events.size)
        assertEquals(104L, events.first().occurredAt)
        assertEquals(5L, events.last().occurredAt)
    }

    @Test
    fun trainingHistoryOnlyIncludesGlobalAndExactConversationScope() = runBlocking {
        dao.insertTrainingTurn(turn("global", null, null, null, 1L))
        dao.insertTrainingTurn(turn("matching", "WHATSAPP", "account-a", "chat-a", 2L))
        dao.insertTrainingTurn(turn("other-account", "WHATSAPP", "account-b", "chat-a", 3L))
        dao.insertTrainingTurn(turn("other-chat", "WHATSAPP", "account-a", "chat-b", 4L))

        val scoped = dao.recentTrainingTurnsForScope("WHATSAPP", "account-a", "chat-a")
        assertEquals(setOf("global", "matching"), scoped.map { it.turnId }.toSet())

        val globalOnly = dao.recentTrainingTurnsForScope(null, null, null)
        assertEquals(listOf("global"), globalOnly.map { it.turnId })
    }

    private fun lesson(id: String, rule: String) = ProfessorTrainingLessonEntity(
        lessonId = id,
        rule = rule,
        scopePlatform = null,
        accountId = null,
        conversationId = null,
        createdAt = 1L,
        updatedAt = 1L,
    )

    private fun turn(
        id: String,
        platform: String?,
        account: String?,
        conversation: String?,
        createdAt: Long,
    ) = ProfessorTrainingTurnEntity(
        turnId = id,
        role = "OWNER",
        text = "training message",
        createdAt = createdAt,
        scopePlatform = platform,
        accountId = account,
        conversationId = conversation,
    )
}
