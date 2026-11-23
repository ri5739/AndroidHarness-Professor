package com.androidharness.app.professor

/**
 * Offline first-pass privacy policy for the TEXT THAT IS ABOUT TO BE SENT.
 * This classifier is intentionally conservative; it is a guard, not semantic proof.
 */
enum class OutgoingReplyDecision {
    AUTO_SEND_ELIGIBLE,
    OWNER_APPROVAL_REQUIRED
}

data class OutgoingReplyAssessment(
    val decision: OutgoingReplyDecision,
    val reasons: Set<String>,
    val safeAcknowledgement: String? = null
)

object ProfessorOutgoingReplyPolicy {
    private val secretPatterns = listOf(
        Regex("""(?i)\b(?:otp|one[- ]time password|verification code|security code)\b.{0,48}\b\d{4,8}\b"""),
        Regex("""(?i)\b(?:password|passcode|pin|login code|authentication token|access token|api key|private key)\b\s*(?:is|:|=)\s*\S+"""),
        Regex("""(?i)\b(?:cvv|cvc|card security code)\b\s*(?:is|:|=)?\s*\d{3,4}\b""")
    )
    private val privateDataPatterns = listOf(
        Regex("""(?i)\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}\b"""),
        Regex("""(?<!\w)(?:\+?\d[\d .()\-]{7,}\d)(?!\w)"""),
        Regex("""(?i)\b(?:bank account|account number|routing number|iban|card number)\b\s*(?:is|:|=)?\s*[A-Z0-9 -]{6,}"""),
        Regex("""(?i)\b(?:home address|residential address|private address|date of birth|national id|nid number|passport number)\b\s*(?:is|:|=)?\s*[^,;.!?]{4,}"""),
        Regex("""(?i)\b(?:confidential|strictly private|do not share|personal record|private document|internal only)\b""")
    )

    fun assess(outgoingText: String): OutgoingReplyAssessment {
        if (outgoingText.isBlank()) {
            return OutgoingReplyAssessment(
                OutgoingReplyDecision.OWNER_APPROVAL_REQUIRED,
                setOf("Empty reply must not be sent")
            )
        }
        val reasons = linkedSetOf<String>()
        if (secretPatterns.any { it.containsMatchIn(outgoingText) }) {
            reasons += "Possible authentication or payment secret"
        }
        if (privateDataPatterns.any { it.containsMatchIn(outgoingText) }) {
            reasons += "Possible private or confidential information"
        }
        return if (reasons.isEmpty()) {
            OutgoingReplyAssessment(OutgoingReplyDecision.AUTO_SEND_ELIGIBLE, emptySet())
        } else {
            OutgoingReplyAssessment(
                OutgoingReplyDecision.OWNER_APPROVAL_REQUIRED,
                reasons,
                "বিষয়টি ব্যক্তিগত বা গোপনীয় হতে পারে। মালিকের অনুমতি নিয়ে জানাচ্ছি। এর মধ্যে অন্য বিষয়ে কথা চালিয়ে যেতে পারি।"
            )
        }
    }

    /** A mixed reply must be regenerated without the protected details, never partially guessed/redacted. */
    fun safeReplyForApprovalRequired(assessment: OutgoingReplyAssessment): String? =
        if (assessment.decision == OutgoingReplyDecision.OWNER_APPROVAL_REQUIRED) {
            assessment.safeAcknowledgement
        } else null
}
