package com.androidharness.app.professor

/**
 * Domain-only types for Agent Professor. These are deliberately independent of Android UI,
 * persistence, and any messaging SDK so safety policy can be unit tested.
 */
enum class MessagingPlatform {
    WHATSAPP_PERSONAL,
    WHATSAPP_BUSINESS,
    MESSENGER_PERSONAL,
    META_BUSINESS_MESSENGER,
    INSTAGRAM_DM
}

enum class ProfessorMode { OFF, DRAFT_ONLY, SEND_ENABLED }
enum class DraftStatus { PENDING, READY, CANCELLED, SENT, FAILED, NEEDS_REVIEW }
enum class RiskFlag { SENSITIVE_DATA, SCAM, PHISHING, UNKNOWN_RECIPIENT, ATTACHMENT_RISK, LOW_CONFIDENCE }

data class ConversationScope(
    val platform: MessagingPlatform,
    val accountId: String,
    val conversationId: String
) {
    init {
        require(accountId.isNotBlank()) { "Account scope cannot be blank" }
        require(conversationId.isNotBlank()) { "Conversation scope cannot be blank" }
    }
}

data class IncomingMessage(
    val messageId: String,
    val scope: ConversationScope,
    val senderId: String,
    val text: String,
    val receivedAtEpochMillis: Long
) {
    init {
        require(messageId.isNotBlank())
        require(senderId.isNotBlank())
    }
}

data class ReplyDraft(
    val draftId: String,
    val scope: ConversationScope,
    val inReplyToMessageId: String,
    val text: String,
    val status: DraftStatus = DraftStatus.PENDING,
    val riskFlags: Set<RiskFlag> = emptySet(),
    val confidence: Float = 0f
) {
    init {
        require(draftId.isNotBlank())
        require(inReplyToMessageId.isNotBlank())
        require(confidence in 0f..1f)
    }
}

data class PlatformCapabilities(
    val readIncoming: Boolean = false,
    val notificationCapture: Boolean = false,
    val openConversation: Boolean = false,
    val stageDraft: Boolean = false,
    val sendMessage: Boolean = false,
    val verifySentState: Boolean = false,
    val attachments: Boolean = false,
    val officialApiAvailable: Boolean = false
)

data class ProfessorConfig(
    val mode: ProfessorMode = ProfessorMode.OFF,
    val enabledPlatforms: Set<MessagingPlatform> = emptySet(),
    val minimumConfidence: Float = 0.85f,
    val smartDelaySeconds: Int = 8,
    val perContactMessagesPerHour: Int = 12
) {
    init {
        require(minimumConfidence in 0f..1f)
        require(smartDelaySeconds in 0..300)
        require(perContactMessagesPerHour in 1..120)
    }
}

data class SendGateResult(val allowed: Boolean, val reasons: List<String>)

object ProfessorSafetyGate {
    fun evaluate(
        config: ProfessorConfig,
        scope: ConversationScope,
        draft: ReplyDraft,
        capabilities: PlatformCapabilities,
        emergencyStop: Boolean,
        humanTakeover: Boolean,
        recipientVerified: Boolean,
        privacyPassed: Boolean,
        rateLimitPassed: Boolean
    ): SendGateResult {
        val reasons = buildList {
            if (emergencyStop) add("Emergency stop is active")
            if (humanTakeover) add("Human takeover is active for this conversation")
            if (config.mode != ProfessorMode.SEND_ENABLED) add("Send mode is not enabled")
            if (scope.platform !in config.enabledPlatforms) add("Platform is disabled")
            if (draft.scope != scope) add("Draft conversation scope does not match target")
            if (!capabilities.sendMessage) add("Adapter does not support sending")
            if (!capabilities.verifySentState) add("Adapter cannot verify send status")
            if (draft.status != DraftStatus.READY) add("Draft is not ready")
            if (draft.text.isBlank()) add("Draft is empty")
            if (draft.confidence < config.minimumConfidence) add("Confidence is below threshold")
            if (draft.riskFlags.isNotEmpty()) add("Draft has unresolved risk flags")
            if (!recipientVerified) add("Recipient is not verified")
            if (!privacyPassed) add("Privacy checks failed")
            if (!rateLimitPassed) add("Rate limit reached")
        }
        return SendGateResult(reasons.isEmpty(), reasons)
    }
}

enum class MemorySensitivity { ORDINARY, SENSITIVE }

data class ContactMemoryCandidate(
    val scope: ConversationScope,
    val contactId: String,
    val content: String,
    val sensitivity: MemorySensitivity,
    val ownerConsentGranted: Boolean = false
) {
    init {
        require(contactId.isNotBlank())
        require(content.isNotBlank())
    }

    fun canPersist(): Boolean = sensitivity == MemorySensitivity.ORDINARY || ownerConsentGranted
}

/** Scope-aware message identity for deduplication; platform/account isolation is intentional. */
data class MessageDedupKey(val scope: ConversationScope, val messageId: String)
