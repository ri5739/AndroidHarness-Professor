package com.androidharness.app.professor

import org.junit.Assert.*
import org.junit.Test

class ProfessorPolicyTest {
    private val scope = ConversationScope(MessagingPlatform.WHATSAPP_PERSONAL, "account-A", "contact-42")

    @Test fun sensitiveMemoryRequiresExplicitOwnerConsent() {
        val candidate = ContactMemoryCandidate(scope, "contact-42", "sensitive detail", MemorySensitivity.SENSITIVE)
        assertFalse(candidate.canPersist())
        assertTrue(candidate.copy(ownerConsentGranted = true).canPersist())
    }

    @Test fun ordinaryMemoryCanPersistWithoutConsentPrompt() {
        val candidate = ContactMemoryCandidate(scope, "contact-42", "likes tea", MemorySensitivity.ORDINARY)
        assertTrue(candidate.canPersist())
    }

    @Test fun scopeSeparatesAccountsAndPlatforms() {
        val otherAccount = scope.copy(accountId = "account-B")
        val otherPlatform = scope.copy(platform = MessagingPlatform.MESSENGER_PERSONAL)
        assertNotEquals(scope, otherAccount)
        assertNotEquals(scope, otherPlatform)
    }

    @Test fun sendGateFailsClosedByDefault() {
        val config = ProfessorConfig()
        val draft = ReplyDraft("draft-1", scope, "message-1", "Hello", confidence = 1f)
        val result = ProfessorSafetyGate.evaluate(
            config, scope, draft, PlatformCapabilities(sendMessage = true, verifySentState = true),
            emergencyStop = false, humanTakeover = false, recipientVerified = true,
            privacyPassed = true, rateLimitPassed = true
        )
        assertFalse(result.allowed)
        assertTrue(result.reasons.isNotEmpty())
    }

    @Test fun sendGateOnlyPassesWhenAllConditionsAreExplicitlySatisfied() {
        val config = ProfessorConfig(
            mode = ProfessorMode.SEND_ENABLED,
            enabledPlatforms = setOf(scope.platform),
            minimumConfidence = .8f
        )
        val draft = ReplyDraft("draft-1", scope, "message-1", "Hello", status = DraftStatus.READY, confidence = .9f)
        val result = ProfessorSafetyGate.evaluate(
            config, scope, draft,
            PlatformCapabilities(sendMessage = true, verifySentState = true),
            emergencyStop = false, humanTakeover = false, recipientVerified = true,
            privacyPassed = true, rateLimitPassed = true
        )
        assertTrue(result.allowed)
    }

    @Test fun riskFlagsBlockSending() {
        val config = ProfessorConfig(ProfessorMode.SEND_ENABLED, setOf(scope.platform))
        val draft = ReplyDraft("draft-1", scope, "message-1", "Maybe click this?", DraftStatus.READY,
            setOf(RiskFlag.PHISHING), confidence = .99f)
        val result = ProfessorSafetyGate.evaluate(
            config, scope, draft, PlatformCapabilities(sendMessage = true, verifySentState = true),
            emergencyStop = false, humanTakeover = false, recipientVerified = true,
            privacyPassed = true, rateLimitPassed = true
        )
        assertFalse(result.allowed)
    }
}
