package com.androidharness.app.professor

import org.junit.Assert.*
import org.junit.Test

class ProfessorAdvancedPoliciesTest {
    private val scope = ConversationScope(MessagingPlatform.WHATSAPP_PERSONAL, "account-a", "chat-a")

    @Test fun identityMappingRequiresOwnerConfirmationAndExactScope() {
        val mapping = ProfessorIdentityMapping(
            scope.platform, scope.accountId, scope.conversationId, "contact-1", "Alice", false
        )
        assertFalse(mapping.isUsableFor(scope))
        assertTrue(mapping.copy(ownerConfirmed = true).isUsableFor(scope))
        assertFalse(mapping.copy(ownerConfirmed = true, accountId = "account-b").isUsableFor(scope))
    }

    @Test fun rateLimiterIsScopedAndUsesSlidingWindow() {
        val limiter = ProfessorSlidingWindowRateLimiter(maxEvents = 2, windowMillis = 1_000)
        assertTrue(limiter.tryAcquire(scope, 1_000))
        assertTrue(limiter.tryAcquire(scope, 1_500))
        assertFalse(limiter.tryAcquire(scope, 1_700))
        assertTrue(limiter.tryAcquire(scope.copy(accountId = "account-b"), 1_700))
        assertTrue(limiter.tryAcquire(scope, 2_501))
    }

    @Test fun historyMergeIsChronologicalDeduplicatedAndScopeIsolated() {
        val incoming = ProfessorConversationTurnRecord(scope, "in-1", ProfessorTurnDirection.INCOMING, "Hi", 100, "notification")
        val outgoing = ProfessorConversationTurnRecord(scope, "out-1", ProfessorTurnDirection.OUTGOING, "Hello", 200, "owner-confirmed-export")
        val foreign = outgoing.copy(turnId = "foreign", scope = scope.copy(platform = MessagingPlatform.INSTAGRAM_DM))
        val merged = ProfessorConversationHistory.merge(scope, listOf(outgoing), listOf(incoming, outgoing, foreign))
        assertEquals(listOf("in-1", "out-1"), merged.map { it.turnId })
        assertEquals(ProfessorTurnDirection.OUTGOING, merged.last().direction)
    }

    @Test fun attachmentPolicyNeverClaimsExtractionWithoutVerifiedAdapter() {
        val pdf = ProfessorAttachmentPolicy.assess("application/pdf", "statement.pdf")
        assertEquals(ProfessorAttachmentKind.PDF, pdf.kind)
        assertFalse(pdf.canExtractText)
        assertTrue(pdf.requiresOwnerReview)
        assertEquals(ProfessorAttachmentKind.IMAGE, ProfessorAttachmentPolicy.assess("image/jpeg", "photo.jpg").kind)
    }

    @Test fun trainingLessonRequiresExplicitApprovalAndRejectsSecrets() {
        val proposed = ProfessorTrainingLesson("lesson-1", "Use a friendly tone", null)
        assertEquals(ProfessorTrainingLessonState.PROPOSED, proposed.state)
        assertEquals(ProfessorTrainingLessonState.APPROVED, proposed.activate(123L).state)
        val secret = ProfessorTrainingLesson("lesson-2", "Password is abc123", null)
        try {
            secret.activate(123L)
            fail("Secret-bearing lesson must be rejected")
        } catch (_: IllegalArgumentException) {
            // expected
        }
    }
}
