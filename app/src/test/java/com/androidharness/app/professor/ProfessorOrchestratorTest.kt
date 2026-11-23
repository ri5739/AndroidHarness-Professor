package com.androidharness.app.professor

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ProfessorOrchestratorTest {
    private val scope = ConversationScope(MessagingPlatform.WHATSAPP_PERSONAL, "acct", "contact")
    private fun incoming(text: String = "Hi") = IncomingMessage("m1", scope, "sender", text, 100L)

    private class FakeStore(var next: IncomingMessage?) : ProfessorQueueStore {
        val drafts = mutableListOf<ReplyDraft>()
        val completed = mutableListOf<String>()
        val retries = mutableListOf<Pair<String, String?>>()
        override suspend fun claimNext(scope: ConversationScope, now: Long): IncomingMessage? {
            val item = next?.takeIf { it.scope == scope }
            if (item != null) next = null
            return item
        }
        override suspend fun saveDraft(draft: ReplyDraft, now: Long) { drafts += draft }
        override suspend fun complete(scope: ConversationScope, messageId: String, now: Long): Boolean {
            completed += messageId
            return true
        }
        override suspend fun retry(scope: ConversationScope, messageId: String, error: String?, now: Long): Boolean {
            retries += messageId to error
            return true
        }
    }

    @Test fun successfulProcessingSavesReviewDraftAndCompletesQueue() = runBlocking {
        val store = FakeStore(incoming())
        val generator = ProfessorReplyGenerator { _, _ -> "Hello back" }
        val orchestrator = ProfessorOrchestrator(
            store, generator,
            ProfessorContextProvider { _, message -> listOf(ProfessorConversationTurn(ProfessorTurnRole.USER, message.text)) },
            ProfessorPromptProvider { _, _ -> "Reply politely" },
        )
        val draft = orchestrator.processOne(scope)
        assertNotNull(draft)
        assertEquals("Hello back", draft!!.text)
        assertEquals(DraftStatus.NEEDS_REVIEW, draft.status)
        assertTrue(draft.riskFlags.contains(RiskFlag.LOW_CONFIDENCE))
        assertEquals(ProfessorOrchestrator.REVIEW_ONLY_CONFIDENCE, draft.confidence)
        assertEquals(1, store.drafts.size)
        assertEquals(listOf("m1"), store.completed)
        assertTrue(store.retries.isEmpty())
    }

    @Test fun draftIdIsStableForSameMessageAndScopedAcrossContacts() {
        val same = stableDraftId(scope, "m1")
        assertEquals(same, stableDraftId(scope, "m1"))
        assertNotEquals(same, stableDraftId(scope.copy(conversationId = "other"), "m1"))
        assertNotEquals(same, stableDraftId(scope.copy(accountId = "other-account"), "m1"))
        assertNotEquals(same, stableDraftId(scope.copy(platform = MessagingPlatform.INSTAGRAM_DM), "m1"))
    }


    @Test fun incomingRiskSignalsAreAttachedToReviewDraft() = runBlocking {
        val risky = incoming("Urgent: verify your account at https://example.test")
        val store = FakeStore(risky)
        val orchestrator = ProfessorOrchestrator(
            store, ProfessorReplyGenerator { _, _ -> "Please review" },
            ProfessorContextProvider { _, message -> listOf(ProfessorConversationTurn(ProfessorTurnRole.USER, message.text)) },
            ProfessorPromptProvider { _, _ -> "Prompt" },
        )
        val draft = orchestrator.processOne(scope)
        assertNotNull(draft)
        assertTrue(draft!!.riskFlags.contains(RiskFlag.PHISHING))
        assertTrue(draft.riskFlags.contains(RiskFlag.LOW_CONFIDENCE))
    }


    @Test fun contextMismatchDoesNotGenerateAndSchedulesRetry() = runBlocking {
        val store = FakeStore(incoming("actual"))
        var generated = false
        val orchestrator = ProfessorOrchestrator(
            store, ProfessorReplyGenerator { _, _ -> generated = true; "bad" },
            ProfessorContextProvider { _, _ -> listOf(ProfessorConversationTurn(ProfessorTurnRole.USER, "other")) },
            ProfessorPromptProvider { _, _ -> "Prompt" },
        )
        assertNull(orchestrator.processOne(scope))
        assertFalse(generated)
        assertTrue(store.drafts.isEmpty())
        assertTrue(store.completed.isEmpty())
        assertEquals(1, store.retries.size)
        assertEquals("m1", store.retries.first().first)
    }

    @Test fun noQueuedMessageReturnsNullWithoutCallingAi() = runBlocking {
        val store = FakeStore(null)
        var generated = false
        val orchestrator = ProfessorOrchestrator(
            store, ProfessorReplyGenerator { _, _ -> generated = true; "reply" },
            ProfessorContextProvider { _, _ -> emptyList() },
            ProfessorPromptProvider { _, _ -> "Prompt" },
        )
        assertNull(orchestrator.processOne(scope))
        assertFalse(generated)
        assertTrue(store.drafts.isEmpty())
    }
}
