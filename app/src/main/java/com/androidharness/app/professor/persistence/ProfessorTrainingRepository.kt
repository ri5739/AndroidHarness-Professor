package com.androidharness.app.professor.persistence

import com.androidharness.app.professor.ConversationScope
import com.androidharness.app.professor.MessagingPlatform
import com.androidharness.app.professor.ProfessorTrainingLessonState
import com.androidharness.app.professor.ProfessorTrainingSecretPolicy
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * Owner-controlled durable training lessons. Proposals are inert until the owner approves them.
 * The repository deliberately does not expose any external-sender write path.
 */
class ProfessorTrainingRepository(
    private val dao: ProfessorPersistenceDao,
    private val now: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) {
    fun lessons(): Flow<List<ProfessorTrainingLessonEntity>> = dao.trainingLessonsFlow()

    /** Returns only approved owner rules that are global or match the exact conversation scope. */
    suspend fun approvedLessonsFor(scope: ConversationScope): List<ProfessorTrainingLessonEntity> =
        dao.approvedTrainingLessonsForScope(
            platform = scope.platform.name,
            accountId = scope.accountId,
            conversationId = scope.conversationId,
        ).filterNot { ProfessorTrainingSecretPolicy.containsSecret(it.rule) }

    suspend fun propose(rule: String, scope: ConversationScope? = null): String {
        require(rule.isNotBlank()) { "Training rule cannot be blank" }
        require(!ProfessorTrainingSecretPolicy.containsSecret(rule)) {
            "Credential-like content cannot be stored as a lesson"
        }
        val timestamp = now()
        val id = newId()
        dao.insertTrainingLesson(
            ProfessorTrainingLessonEntity(
                lessonId = id,
                rule = rule.trim(),
                scopePlatform = scope?.platform?.name,
                accountId = scope?.accountId,
                conversationId = scope?.conversationId,
                state = ProfessorTrainingLessonState.PROPOSED.name,
                createdAt = timestamp,
                updatedAt = timestamp,
            )
        )
        return id
    }

    suspend fun approve(lessonId: String): Boolean {
        val lesson = dao.trainingLesson(lessonId) ?: return false
        require(lesson.state == ProfessorTrainingLessonState.PROPOSED.name) {
            "Only a proposed lesson can be approved"
        }
        require(!ProfessorTrainingSecretPolicy.containsSecret(lesson.rule)) {
            "Credential-like content cannot be activated"
        }
        return dao.approveTrainingLesson(lessonId, now()) == 1
    }

    suspend fun pause(lessonId: String): Boolean =
        dao.updateTrainingLessonState(lessonId, ProfessorTrainingLessonState.PAUSED.name, now()) == 1

    suspend fun reject(lessonId: String): Boolean =
        dao.updateTrainingLessonState(lessonId, ProfessorTrainingLessonState.REJECTED.name, now()) == 1

    suspend fun delete(lessonId: String) = dao.deleteTrainingLesson(lessonId)

    fun scopeOf(lesson: ProfessorTrainingLessonEntity): ConversationScope? {
        val platform = lesson.scopePlatform ?: return null
        val account = lesson.accountId ?: return null
        val conversation = lesson.conversationId ?: return null
        return runCatching {
            ConversationScope(MessagingPlatform.valueOf(platform), account, conversation)
        }.getOrNull()
    }
}
