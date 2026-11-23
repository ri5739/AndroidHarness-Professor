package com.androidharness.app.professor

/**
 * Explicit owner-confirmed identity mapping. Notification metadata is never treated as
 * proof of account/contact identity; callers must set ownerConfirmed after user action.
 */
data class ProfessorIdentityMapping(
    val platform: MessagingPlatform,
    val accountId: String,
    val conversationId: String,
    val contactId: String,
    val displayName: String,
    val ownerConfirmed: Boolean,
) {
    init {
        require(accountId.isNotBlank())
        require(conversationId.isNotBlank())
        require(contactId.isNotBlank())
        require(displayName.isNotBlank())
    }

    fun isUsableFor(scope: ConversationScope): Boolean =
        ownerConfirmed && scope.platform == platform &&
            scope.accountId == accountId && scope.conversationId == conversationId
}

/** A deterministic sliding-window limiter keyed by the full platform/account/conversation scope. */
class ProfessorSlidingWindowRateLimiter(
    private val maxEvents: Int,
    private val windowMillis: Long,
) {
    init {
        require(maxEvents > 0)
        require(windowMillis > 0)
    }

    private val events = mutableMapOf<ConversationScope, ArrayDeque<Long>>()

    @Synchronized
    fun tryAcquire(scope: ConversationScope, nowMillis: Long): Boolean {
        val queue = events.getOrPut(scope) { ArrayDeque() }
        val cutoff = nowMillis - windowMillis
        while (queue.isNotEmpty() && queue.first() <= cutoff) queue.removeFirst()
        if (queue.size >= maxEvents) return false
        queue.addLast(nowMillis)
        return true
    }

    @Synchronized
    fun clear(scope: ConversationScope) {
        events.remove(scope)
    }
}

/** Conversation turns are only added when obtained from a reliable source; never synthesize outgoing turns. */
enum class ProfessorTurnDirection { INCOMING, OUTGOING }

data class ProfessorConversationTurnRecord(
    val scope: ConversationScope,
    val turnId: String,
    val direction: ProfessorTurnDirection,
    val text: String,
    val timestampMillis: Long,
    val source: String,
) {
    init {
        require(turnId.isNotBlank())
        require(text.isNotBlank())
        require(source.isNotBlank())
    }
}

object ProfessorConversationHistory {
    fun merge(
        expectedScope: ConversationScope,
        existing: List<ProfessorConversationTurnRecord>,
        incoming: List<ProfessorConversationTurnRecord>,
        limit: Int = 50,
    ): List<ProfessorConversationTurnRecord> {
        require(limit > 0)
        return (existing + incoming)
            .filter { it.scope == expectedScope }
            .distinctBy { it.turnId }
            .sortedWith(compareBy<ProfessorConversationTurnRecord> { it.timestampMillis }.thenBy { it.turnId })
            .takeLast(limit)
    }
}

enum class ProfessorAttachmentKind { IMAGE, PDF, DOCUMENT, AUDIO, VIDEO, UNKNOWN }

data class ProfessorAttachmentAssessment(
    val kind: ProfessorAttachmentKind,
    val canExtractText: Boolean,
    val requiresOwnerReview: Boolean,
    val reason: String? = null,
)

object ProfessorAttachmentPolicy {
    fun assess(mimeType: String?, fileName: String?): ProfessorAttachmentAssessment {
        val mime = mimeType?.lowercase().orEmpty()
        val name = fileName?.lowercase().orEmpty()
        return when {
            mime.startsWith("image/") -> ProfessorAttachmentAssessment(
                ProfessorAttachmentKind.IMAGE, canExtractText = false,
                requiresOwnerReview = true, reason = "Image OCR/vision capability must be verified"
            )
            mime == "application/pdf" || name.endsWith(".pdf") -> ProfessorAttachmentAssessment(
                ProfessorAttachmentKind.PDF, canExtractText = false,
                requiresOwnerReview = true, reason = "PDF extraction is not enabled in this adapter"
            )
            mime.startsWith("audio/") -> ProfessorAttachmentAssessment(
                ProfessorAttachmentKind.AUDIO, canExtractText = false,
                requiresOwnerReview = true, reason = "Audio transcription capability must be verified"
            )
            mime.startsWith("video/") -> ProfessorAttachmentAssessment(
                ProfessorAttachmentKind.VIDEO, canExtractText = false,
                requiresOwnerReview = true, reason = "Video processing is unsupported"
            )
            mime.contains("word") || mime.contains("officedocument") ||
                name.endsWith(".doc") || name.endsWith(".docx") ||
                name.endsWith(".txt") || name.endsWith(".rtf") ->
                ProfessorAttachmentAssessment(
                    ProfessorAttachmentKind.DOCUMENT, canExtractText = false,
                    requiresOwnerReview = true, reason = "Document parsing capability must be verified"
                )
            else -> ProfessorAttachmentAssessment(
                ProfessorAttachmentKind.UNKNOWN, canExtractText = false,
                requiresOwnerReview = true, reason = "Unknown attachment type"
            )
        }
    }
}

/** Audit events intentionally store action metadata, never message or attachment contents. */
data class ProfessorAuditEvent(
    val eventId: String,
    val scope: ConversationScope,
    val action: String,
    val outcome: String,
    val timestampMillis: Long,
) {
    init {
        require(eventId.isNotBlank())
        require(action.isNotBlank())
        require(outcome.isNotBlank())
    }
}

enum class ProfessorTrainingLessonState { PROPOSED, APPROVED, PAUSED, REJECTED }

data class ProfessorTrainingLesson(
    val lessonId: String,
    val rule: String,
    val scope: ConversationScope?,
    val state: ProfessorTrainingLessonState = ProfessorTrainingLessonState.PROPOSED,
    val approvedAtMillis: Long? = null,
) {
    init {
        require(lessonId.isNotBlank())
        require(rule.isNotBlank())
    }

    fun activate(nowMillis: Long): ProfessorTrainingLesson {
        require(state == ProfessorTrainingLessonState.PROPOSED) {
            "Only a proposed lesson can be approved"
        }
        require(!ProfessorTrainingSecretPolicy.containsSecret(rule)) {
            "Credential-like content cannot be stored as a lesson"
        }
        return copy(state = ProfessorTrainingLessonState.APPROVED, approvedAtMillis = nowMillis)
    }
}

object ProfessorTrainingSecretPolicy {
    private val patterns = listOf(
        Regex("""(?i)\b(?:password|passcode|otp|one[- ]time code|api key|access token|private key)\b\s*(?:is|:|=)\s*\S+"""),
        Regex("""(?i)\b\d{3,4}\s*(?:cvv|cvc)\b"""),
        Regex("""-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----"""),
    )

    fun containsSecret(text: String): Boolean = patterns.any { it.containsMatchIn(text) }
}
