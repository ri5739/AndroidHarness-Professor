package com.androidharness.app.professor

import com.androidharness.app.professor.persistence.ProfessorPersistence
import kotlinx.coroutines.CancellationException
import java.util.UUID

fun interface ProfessorContextProvider {
    suspend fun conversation(scope: ConversationScope, incoming: IncomingMessage): List<ProfessorConversationTurn>
}

fun interface ProfessorPromptProvider {
    fun prompt(scope: ConversationScope, incoming: IncomingMessage): String
}

class ProfessorOrchestrator(
    private val persistence: ProfessorQueueStore,
    private val ai: ProfessorReplyGenerator,
    private val contextProvider: ProfessorContextProvider,
    private val promptProvider: ProfessorPromptProvider,
) {
    suspend fun processOne(scope: ConversationScope): ReplyDraft? {
        val incoming = persistence.claimNext(scope) ?: return null
        try {
            val turns = contextProvider.conversation(scope, incoming)
            require(turns.isNotEmpty()) { "Context provider returned no turns" }
            require(turns.last().role == ProfessorTurnRole.USER) {
                "Context must end with the incoming user message"
            }
            require(turns.last().text == incoming.text) {
                "Context must end with the claimed message; scope/context mismatch"
            }
            val reply = ai.generateReply(promptProvider.prompt(scope, incoming), turns)
            val draft = ReplyDraft(
                draftId = UUID.randomUUID().toString(),
                scope = scope,
                inReplyToMessageId = incoming.messageId,
                text = reply,
                status = DraftStatus.NEEDS_REVIEW,
                riskFlags = setOf(RiskFlag.LOW_CONFIDENCE),
                confidence = REVIEW_ONLY_CONFIDENCE,
            )
            persistence.saveDraft(draft)
            check(persistence.complete(scope, incoming.messageId)) {
                "Incoming message could not be marked completed after draft save"
            }
            return draft
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            persistence.retry(scope, incoming.messageId, failure.message ?: failure.javaClass.simpleName)
            return null
        }
    }

    companion object {
        const val REVIEW_ONLY_CONFIDENCE = 0.50f
    }
}
