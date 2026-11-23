package com.androidharness.app.professor

import org.junit.Assert.*
import org.junit.Test

class ProfessorOutgoingReplyPolicyTest {
    @Test fun ordinaryConversationIsEligibleForAutomaticReply() {
        val result = ProfessorOutgoingReplyPolicy.assess("হ্যালো! কেমন আছেন? আজ দিনটা কেমন গেল?")
        assertEquals(OutgoingReplyDecision.AUTO_SEND_ELIGIBLE, result.decision)
        assertTrue(result.reasons.isEmpty())
        assertNull(result.safeAcknowledgement)
    }

    @Test fun otpAndPasswordDisclosureRequireOwnerApproval() {
        listOf(
            "আপনার OTP is 482911",
            "Your password is example-secret",
            "The API key: example-key-value"
        ).forEach { text ->
            val result = ProfessorOutgoingReplyPolicy.assess(text)
            assertEquals(OutgoingReplyDecision.OWNER_APPROVAL_REQUIRED, result.decision)
            assertNotNull(result.safeAcknowledgement)
        }
    }

    @Test fun privateContactAndFinancialDetailsRequireOwnerApproval() {
        listOf(
            "আমার নম্বর +8801712345678",
            "Email: private.person@example.com",
            "Bank account number: 123456789012"
        ).forEach { text ->
            assertEquals(
                OutgoingReplyDecision.OWNER_APPROVAL_REQUIRED,
                ProfessorOutgoingReplyPolicy.assess(text).decision
            )
        }
    }

    @Test fun mixedReplyIsHeldAndGetsSafeAcknowledgementInsteadOfUnsafeRedaction() {
        val result = ProfessorOutgoingReplyPolicy.assess(
            "হ্যাঁ, কাল দেখা হবে। তোমার OTP is 482911."
        )
        assertEquals(OutgoingReplyDecision.OWNER_APPROVAL_REQUIRED, result.decision)
        val safeReply = ProfessorOutgoingReplyPolicy.safeReplyForApprovalRequired(result)!!
        assertTrue(safeReply.contains("অনুমতি"))
        assertFalse(safeReply.contains("482911"))
    }

    @Test fun emptyReplyIsNotEligibleForSending() {
        assertEquals(
            OutgoingReplyDecision.OWNER_APPROVAL_REQUIRED,
            ProfessorOutgoingReplyPolicy.assess("  ").decision
        )
    }
}
