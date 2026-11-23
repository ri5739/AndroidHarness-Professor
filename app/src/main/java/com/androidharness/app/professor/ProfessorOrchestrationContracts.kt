package com.androidharness.app.professor

/** Narrow orchestration contract for the durable queue and draft store. */
interface ProfessorQueueStore {
    suspend fun claimNext(scope: ConversationScope, now: Long = System.currentTimeMillis()): IncomingMessage?
    suspend fun saveDraft(draft: ReplyDraft, now: Long = System.currentTimeMillis())
    suspend fun complete(scope: ConversationScope, messageId: String, now: Long = System.currentTimeMillis()): Boolean
    suspend fun retry(scope: ConversationScope, messageId: String, error: String?, now: Long = System.currentTimeMillis()): Boolean
}

fun interface ProfessorReplyGenerator {
    suspend fun generateReply(systemPrompt: String, conversation: List<ProfessorConversationTurn>): String
}
