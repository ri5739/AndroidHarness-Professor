package com.androidharness.app.professor

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.androidharness.app.data.db.AppDatabase
import com.androidharness.app.professor.persistence.ProfessorTrainingRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfessorTrainingCoachIntegrationTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: ProfessorTrainingRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        var nextId = 0
        repository = ProfessorTrainingRepository(
            dao = database.professorPersistenceDao(),
            now = { 100L + nextId },
            newId = { "training-" + (++nextId) },
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun aiCoreFailureKeepsOwnerTurnAndStoresSafeFallback() = runBlocking {
        val coach = ProfessorTrainingCoach(
            ai = ProfessorReplyGenerator { _, _ -> error("provider unavailable") },
            repository = repository,
        )

        val result = coach.teach("উত্তর সংক্ষিপ্ত রাখবে")

        assertEquals("AI Core generation failed", result.rejectedReason)
        val history = repository.conversationHistory().first()
        assertEquals(listOf("OWNER", "ASSISTANT"), history.map { it.role })
        assertTrue(history.last().text.contains("AI Core"))
        assertTrue(repository.lessons().first().isEmpty())
    }

    @Test
    fun secretLikeAiLessonIsRejectedAndNeverPersisted() = runBlocking {
        val coach = ProfessorTrainingCoach(
            ai = ProfessorReplyGenerator { _, _ -> "LESSON: password is secret123" },
            repository = repository,
        )

        val result = coach.teach("একটি নিয়ম শেখাও")

        assertEquals("Unsafe or empty lesson proposal", result.rejectedReason)
        assertTrue(repository.lessons().first().isEmpty())
        assertFalse(repository.conversationHistory().first().any { it.text.contains("secret123") })
    }

    @Test
    fun secretLikeOwnerMessageIsNotPersisted() = runBlocking {
        val coach = ProfessorTrainingCoach(
            ai = ProfessorReplyGenerator { _, _ -> "NO_LESSON: ok" },
            repository = repository,
        )

        val result = coach.teach("password is supersecret123")

        assertTrue(result.rejectedReason!!.contains("Secret-like"))
        val history = repository.conversationHistory().first()
        assertEquals(1, history.size)
        assertEquals("ASSISTANT", history.single().role)
        assertFalse(history.single().text.contains("supersecret123"))
    }

    @Test
    fun simulationAiFailureReturnsNoSendSafePreview() = runBlocking {
        val coach = ProfessorTrainingCoach(
            ai = ProfessorReplyGenerator { _, _ -> error("provider unavailable") },
            repository = repository,
        )
        val scope = ConversationScope(MessagingPlatform.WHATSAPP_PERSONAL, "account", "chat")

        val result = coach.simulate(scope, "Hello")

        assertFalse(result.sentExternally)
        assertTrue(result.requiresOwnerApproval)
        assertTrue(result.replyPreview.contains("preview"))
    }
}
