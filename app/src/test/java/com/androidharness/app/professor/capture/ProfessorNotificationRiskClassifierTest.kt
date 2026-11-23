package com.androidharness.app.professor.capture

import com.androidharness.app.professor.RiskFlag
import org.junit.Assert.assertEquals
import org.junit.Test

class ProfessorNotificationRiskClassifierTest {
    @Test fun detectsOtpAndContactInformationAsSensitive() {
        assertEquals(setOf(RiskFlag.SENSITIVE_DATA), ProfessorNotificationRiskClassifier.classify("Your OTP is 482911"))
        assertEquals(setOf(RiskFlag.SENSITIVE_DATA), ProfessorNotificationRiskClassifier.classify("Email me at test@example.com"))
        assertEquals(setOf(RiskFlag.SENSITIVE_DATA), ProfessorNotificationRiskClassifier.classify("Call +8801712345678"))
    }

    @Test fun flagsUrgentCredentialLinkAsPotentialPhishing() {
        assertEquals(
            setOf(RiskFlag.PHISHING),
            ProfessorNotificationRiskClassifier.classify("Urgent: verify your account immediately at https://example.test"),
        )
    }

    @Test fun benignTextAndBlankTextProduceNoFlags() {
        assertEquals(emptySet<RiskFlag>(), ProfessorNotificationRiskClassifier.classify("Hello, are you free today?"))
        assertEquals(emptySet<RiskFlag>(), ProfessorNotificationRiskClassifier.classify("  "))
    }

    @Test fun linkWithoutUrgencyOrCredentialPromptDoesNotTriggerPhishingFlag() {
        assertEquals(emptySet<RiskFlag>(), ProfessorNotificationRiskClassifier.classify("See https://example.test for details"))
    }
}
