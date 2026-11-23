package com.androidharness.app.professor

import com.androidharness.app.professor.persistence.ProfessorPersistence
import kotlinx.coroutines.CancellationException

/**
 * Phase 2 processing loop for one queued message.
 *
 * Context must be supplied by a scope-aware provider; this class never looks up another
 * account or conversation. Generated text is always a review draft (never sent).
 */
fun interface ProfessorContextProvider {
    suspend fun conversation(scope: ConversationScope, incoming: IncomingMessage): List<ProfessorConversationTurn>
}

fun interface ProfessorPromptProvider {
    fun prompt(scope: ConversationScope, incoming: IncomingMessage): String
}

class ProfessorOrchestrator(
    private val persistence: ProfessorPersistence,
    private val ai: ProfessorAiAdapter,
    private val contextProvider: ProfessorContextProvider,
    private val promptProvider: ProfessorPromptProvider,
) {
    /**
     * Claims and processes at most one message. Returns its resulting draft, or null when
     * no due message exists. Failures are recorded via the existing retry policy.
     */
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
                draftId = ProfessorPersistence.newDraftId(),
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
            // Do not swallow coroutine cancellation. Interrupted processing is recovered on restart.
            throw cancelled
        } catch (failure: Exception) {
            persistence.retry(scope, incoming.messageId, failure.message ?: failure.javaClass.simpleName)
            return null
        }
    }

    companion object {
        /** No provider confidence signal exists in the current streaming contract. */
        const val REVIEW_ONLY_CONFIDENCE = 0.50f
    }
}
