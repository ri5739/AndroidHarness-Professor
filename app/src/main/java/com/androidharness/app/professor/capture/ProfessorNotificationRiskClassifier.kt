package com.androidharness.app.professor.capture

import com.androidharness.app.professor.RiskFlag

/**
 * Conservative, offline-only signal detection for notification text.
 * This is not a replacement for semantic privacy/scam review; matches are used
 * to raise review flags and never to authorize sending.
 */
object ProfessorNotificationRiskClassifier {
    private val otpPattern = Regex("""(?i)\\b(?:otp|one[- ]time (?:password|code)|verification code|security code)\\b.{0,48}\\b\\d{4,8}\\b""")
    private val emailPattern = Regex("""(?i)\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b""")
    private val phonePattern = Regex("""(?<!\\w)(?:\\+?\\d[\\d .()\\-]{7,}\\d)(?!\\w)""")
    private val linkPattern = Regex("""(?i)\\bhttps?://|\\bwww\\.""")
    private val urgencyPattern = Regex("""(?i)\\b(?:urgent|immediately|verify|verification|password|login|account|bank|wallet|payment|click|suspended|blocked)\\b""")

    fun classify(text: String): Set<RiskFlag> {
        if (text.isBlank()) return emptySet()
        val flags = linkedSetOf<RiskFlag>()
        if (otpPattern.containsMatchIn(text) || emailPattern.containsMatchIn(text) || phonePattern.containsMatchIn(text)) {
            flags += RiskFlag.SENSITIVE_DATA
        }
        if (linkPattern.containsMatchIn(text) && urgencyPattern.containsMatchIn(text)) {
            flags += RiskFlag.PHISHING
        }
        return flags
    }
}
