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

    fun conversationHistory(): Flow<List<ProfessorTrainingTurnEntity>> = dao.trainingTurnsFlow()

    /** Immutable lesson change history; deletion never erases its audit trail. */
    fun auditHistory(lessonId: String): Flow<List<ProfessorTrainingAuditEntity>> =
        dao.trainingAuditFlow(lessonId)

    fun allAuditHistory(): Flow<List<ProfessorTrainingAuditEntity>> = dao.trainingAuditFlowAll()

    /** Bounded, chronological context for the next owner-training turn. */
    suspend fun recentConversationHistory(scope: ConversationScope? = null): List<ProfessorTrainingTurnEntity> =
        dao.recentTrainingTurnsForScope(
            platform = scope?.platform?.name,
            accountId = scope?.accountId,
            conversationId = scope?.conversationId,
        ).asReversed().filterNot { ProfessorTrainingSecretPolicy.containsSecret(it.text) }

    suspend fun appendTrainingTurn(
        role: String,
        text: String,
        scope: ConversationScope? = null,
        createdAt: Long = now(),
    ): String {
        require(role == "OWNER" || role == "ASSISTANT") { "Training Chat accepts owner and assistant turns only" }
        require(text.isNotBlank()) { "Training message cannot be blank" }
        require(!ProfessorTrainingSecretPolicy.containsSecret(text)) { "Credential-like content cannot be stored in Training Chat history" }
        val id = newId()
        dao.insertTrainingTurn(
            ProfessorTrainingTurnEntity(
                turnId = id,
                role = role,
                text = text.trim(),
                createdAt = createdAt,
                scopePlatform = scope?.platform?.name,
                accountId = scope?.accountId,
                conversationId = scope?.conversationId,
            )
        )
        return id
    }

    suspend fun clearTrainingHistory() = dao.clearTrainingTurns()

    suspend fun edit(lessonId: String, rule: String): Boolean {
        require(rule.isNotBlank()) { "Training rule cannot be blank" }
        require(!ProfessorTrainingSecretPolicy.containsSecret(rule)) { "Credential-like content cannot be stored as a lesson" }
        val current = dao.trainingLesson(lessonId) ?: return false
        if (current.state == ProfessorTrainingLessonState.REJECTED.name) return false
        return dao.editTrainingLessonWithAudit(lessonId, rule.trim(), now()) == 1
    }

    suspend fun resetAllLessons() = dao.clearTrainingLessonsWithAudit(now())

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
        return dao.approveTrainingLessonWithAudit(lessonId, now()) == 1
    }

    suspend fun pause(lessonId: String): Boolean =
        dao.updateTrainingLessonStateWithAudit(lessonId, ProfessorTrainingLessonState.PAUSED.name, now()) == 1

    suspend fun resume(lessonId: String): Boolean {
        val lesson = dao.trainingLesson(lessonId) ?: return false
        require(lesson.state == ProfessorTrainingLessonState.PAUSED.name) { "Only a paused lesson can be resumed" }
        require(!ProfessorTrainingSecretPolicy.containsSecret(lesson.rule)) { "Credential-like content cannot be activated" }
        return dao.updateTrainingLessonStateWithAudit(lessonId, ProfessorTrainingLessonState.APPROVED.name, now()) == 1
    }

    suspend fun reject(lessonId: String): Boolean =
        dao.updateTrainingLessonStateWithAudit(lessonId, ProfessorTrainingLessonState.REJECTED.name, now()) == 1

    suspend fun delete(lessonId: String) = dao.deleteTrainingLessonWithAudit(lessonId, now())

    fun scopeOf(lesson: ProfessorTrainingLessonEntity): ConversationScope? {
        val platform = lesson.scopePlatform ?: return null
        val account = lesson.accountId ?: return null
        val conversation = lesson.conversationId ?: return null
        return runCatching {
            ConversationScope(MessagingPlatform.valueOf(platform), account, conversation)
        }.getOrNull()
    }
}
