package com.androidharness.app.professor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfessorLanguagePolicyTest {
    @Test
    fun ordinaryRepliesAreAlwaysBengaliByDefaultRegardlessOfIncomingLanguage() {
        assertEquals("Bengali", ProfessorLanguagePolicy.DEFAULT_REPLY_LANGUAGE)
        assertTrue(ProfessorLanguagePolicy.INSTRUCTIONS.contains("ALWAYS write the initial reply in Bengali"))
        assertTrue(ProfessorLanguagePolicy.INSTRUCTIONS.contains("regardless"))
        assertTrue(ProfessorLanguagePolicy.INSTRUCTIONS.contains("বাংলা"))
        assertFalse(ProfessorLanguagePolicy.INSTRUCTIONS.contains("reply in that same language"))
    }

    @Test
    fun explicitSenderLanguageRequestOverridesAndPersistsWithinConversation() {
        val policy = ProfessorLanguagePolicy.INSTRUCTIONS
        assertTrue(policy.contains("explicitly asks for a reply in a named language"))
        assertTrue(policy.contains("conversation-scoped preference"))
        assertTrue(policy.contains("latest explicit sender instruction"))
        assertTrue(policy.contains("until the sender requests a different"))
    }

    @Test
    fun policySupportsMultilingualTextAndVoiceTranscripts() {
        val policy = ProfessorLanguagePolicy.INSTRUCTIONS
        assertTrue(policy.contains("any language"))
        assertTrue(policy.contains("speech-to-text voice transcripts") || policy.contains("speech-to-text transcripts"))
    }

    @Test
    fun trainingChatUsesItsOwnSeparateLanguagePolicy() {
        assertTrue(ProfessorLanguagePolicy.TRAINING_INSTRUCTIONS.contains("Respond to the owner in Bengali by default"))
        assertFalse(ProfessorLanguagePolicy.TRAINING_INSTRUCTIONS.contains("conversation-scoped preference"))
        val prompt = ProfessorLanguagePolicy.appendTrainingInstructions("Training prompt")
        assertTrue(prompt.contains(ProfessorLanguagePolicy.TRAINING_INSTRUCTIONS))
    }

    @Test
    fun languageInstructionsAreAppendedWithoutDroppingExistingPrompt() {
        val prompt = ProfessorLanguagePolicy.appendInstructions("Existing privacy prompt")
        assertTrue(prompt.startsWith("Existing privacy prompt"))
        assertTrue(prompt.contains(ProfessorLanguagePolicy.INSTRUCTIONS))
    }
}
