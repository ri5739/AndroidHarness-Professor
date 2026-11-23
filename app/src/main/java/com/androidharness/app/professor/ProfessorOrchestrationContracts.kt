package com.androidharness.app.professor

/**
 * Narrow orchestration contract: processing logic can be tested without Android/Room.
 * Production persistence remains the Room-backed ProfessorPersistence implementation.
 */
interface ProfessorQueueStore {
    suspend fun claimNext(scope: ConversationScope): IncomingMessage?
    suspend fun saveDraft(draft: ReplyDraft)
    suspend fun complete(scope: ConversationScope, messageId: String): Boolean
    suspend fun retry(scope: ConversationScope, messageId: String, error: String?): Boolean
}

fun interface ProfessorReplyGenerator {
    suspend fun generateReply(systemPrompt: String, conversation: List<ProfessorConversationTurn>): String
}
