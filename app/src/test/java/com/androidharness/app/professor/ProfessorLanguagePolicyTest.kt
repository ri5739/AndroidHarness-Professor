package com.androidharness.app.professor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfessorLanguagePolicyTest {
    @Test
    fun BengaliIsTheDefaultReplyLanguage() {
        assertEquals("Bengali", ProfessorLanguagePolicy.DEFAULT_REPLY_LANGUAGE)
        assertTrue(ProfessorLanguagePolicy.INSTRUCTIONS.contains("Default reply language is Bengali"))
        assertTrue(ProfessorLanguagePolicy.INSTRUCTIONS.contains("বাংলা"))
    }

    @Test
    fun policyRequiresUnderstandingMultilingualAndVoiceTranscribedMessages() {
        val policy = ProfessorLanguagePolicy.INSTRUCTIONS
        assertTrue(policy.contains("any language"))
        assertTrue(policy.contains("speech-to-text transcripts"))
        assertTrue(policy.contains("Detect the language of the latest incoming sender message"))
        assertTrue(policy.contains("same language"))
    }

    @Test
    fun languageInstructionsAreAppendedWithoutDroppingExistingPrompt() {
        val prompt = ProfessorLanguagePolicy.appendInstructions("Existing privacy prompt")
        assertTrue(prompt.startsWith("Existing privacy prompt"))
        assertTrue(prompt.contains(ProfessorLanguagePolicy.INSTRUCTIONS))
    }
}
