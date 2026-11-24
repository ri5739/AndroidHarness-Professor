package com.androidharness.app.professor

/**
 * Language behavior shared by Training Chat and the real reply orchestrator.
 * Language detection itself is delegated to AndroidHarness AI Core; this class
 * supplies deterministic output rules without adding another model/runtime.
 */
object ProfessorLanguagePolicy {
    const val DEFAULT_REPLY_LANGUAGE = "Bengali"

    const val INSTRUCTIONS = """
        LANGUAGE AND VOICE MESSAGE POLICY:
        - Understand incoming text in any language and mixed-language messages; do not assume English.
        - Incoming voice messages may arrive as speech-to-text transcripts. Interpret the transcript
          semantically even if it contains punctuation or minor transcription errors.
        - Detect the language of the latest incoming sender message from its content, not from the
          device locale, account locale, or previous conversation.
        - Default reply language is Bengali (বাংলা), using natural Bengali script.
        - For an incoming message written or spoken in a clear non-Bengali language, reply in that
          same language unless the owner has explicitly configured a different reply language or
          the sender explicitly requests another language.
        - For mixed-language messages, use the dominant language; preserve names, technical terms,
          and phrases that are naturally code-switched.
        - Follow explicit owner-approved language preferences for the exact conversation scope.
        - If the message language or meaning is genuinely unclear, ask a short clarifying question
          in Bengali. Never invent a translation or claim certainty about unclear speech.
        - These language rules never override safety, privacy, consent, or owner-only instructions.
    """.trimIndent()

    fun appendInstructions(basePrompt: String): String {
        require(basePrompt.isNotBlank()) { "Base prompt cannot be blank" }
        return basePrompt.trim() + "\n\n" + INSTRUCTIONS
    }
}
