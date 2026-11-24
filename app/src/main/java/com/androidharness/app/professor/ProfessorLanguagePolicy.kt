package com.androidharness.app.professor

/**
 * Language behavior for real messaging and the separate owner Training Chat.
 * Language understanding is delegated to AndroidHarness AI Core.
 */
object ProfessorLanguagePolicy {
    const val DEFAULT_REPLY_LANGUAGE = "Bengali"

    /** Policy for ordinary incoming messages across messaging platforms. */
    const val INSTRUCTIONS = """
        LANGUAGE AND VOICE MESSAGE POLICY FOR REAL CONVERSATIONS:
        - Understand incoming text in any language and mixed-language messages; do not assume English.
        - Incoming voice messages may arrive as speech-to-text transcripts. Understand the transcript
          semantically even when punctuation or minor transcription errors are present.
        - Detect what the sender means, but do not use the sender's detected language as the reply language.
        - ALWAYS write the initial reply in Bengali (বাংলা), in natural Bengali script, regardless of
          whether the sender wrote or spoke English, Arabic, Hindi, or any other supported language.
        - Exception: if the sender explicitly asks for a reply in a named language (for example,
          "reply in English", "বাংলায় নয়, আরবিতে বলুন"), follow that request for the reply.
        - Treat the sender's latest explicit language request as a conversation-scoped preference:
          continue using that requested language for later replies in this same conversation, even
          when later messages arrive in another language, until the sender requests a different
          language or asks to return to Bengali.
        - Determine the active preference from the latest explicit sender instruction in the
          available conversation history; a new explicit request overrides an older one.
        - A request to translate quoted text is not automatically a request to change the language
          of the whole conversation; apply the requested language to the requested content.
        - If no explicit language preference exists, use Bengali. If meaning is unclear, ask briefly
          in Bengali. Never invent a translation or claim certainty about unclear speech.
        - These language rules never override safety, privacy, consent, or owner-only instructions.
    """.trimIndent()

    /** Training Chat is an owner-facing workflow and intentionally has its own language behavior. */
    const val TRAINING_INSTRUCTIONS = """
        TRAINING CHAT LANGUAGE POLICY:
        - Understand owner instructions in any language, including speech-to-text voice transcripts.
        - Respond to the owner in Bengali by default.
        - If the owner explicitly requests another output language, use it for the requested content.
        - Do not apply the real-conversation sender language preference rules to Training Chat.
    """.trimIndent()

    fun appendInstructions(basePrompt: String): String {
        require(basePrompt.isNotBlank()) { "Base prompt cannot be blank" }
        return basePrompt.trim() + "\n\n" + INSTRUCTIONS
    }

    fun appendTrainingInstructions(basePrompt: String): String {
        require(basePrompt.isNotBlank()) { "Base prompt cannot be blank" }
        return basePrompt.trim() + "\n\n" + TRAINING_INSTRUCTIONS
    }
}
