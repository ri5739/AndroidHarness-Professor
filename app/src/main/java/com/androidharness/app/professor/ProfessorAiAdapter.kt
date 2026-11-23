package com.androidharness.app.professor

import com.androidharness.app.core.ChatMessage
import com.androidharness.app.core.Role
import com.androidharness.app.llm.LlmProvider
import com.androidharness.app.llm.ProviderConfig
import com.androidharness.app.llm.RequestOptions
import com.androidharness.app.llm.StreamEvent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collect

/**
 * Professor-facing text generation bridge to AndroidHarness' existing provider system.
 * This deliberately does not construct providers, route models, or own API credentials.
 * The caller supplies the provider selected by AndroidHarness (including its fallback/local
 * provider), configuration and key, so Professor cannot create a parallel AI stack.
 */
class ProfessorAiAdapter(
    private val provider: LlmProvider,
    private val providerConfig: ProviderConfig,
    private val apiKey: String,
    private val requestOptions: RequestOptions = RequestOptions(maxOutputTokens = 1024),
) : ProfessorReplyGenerator {
    override suspend fun generateReply(
        systemPrompt: String,
        conversation: List<ProfessorConversationTurn>,
    ): String {
        require(systemPrompt.isNotBlank()) { "Professor system prompt must not be blank" }
        require(conversation.isNotEmpty()) { "Conversation context must not be empty" }

        val messages = conversation.map { turn ->
            ChatMessage(
                role = when (turn.role) {
                    ProfessorTurnRole.USER -> Role.USER
                    ProfessorTurnRole.ASSISTANT -> Role.ASSISTANT
                },
                text = turn.text,
            )
        }
        val output = StringBuilder()
        var failure: StreamEvent.Failure? = null
        provider.streamChat(
            config = providerConfig,
            apiKey = apiKey,
            systemPrompt = systemPrompt,
            messages = messages,
            tools = emptyList(),
            options = requestOptions,
        ).collect { event ->
            when (event) {
                is StreamEvent.TextDelta -> output.append(event.text)
                is StreamEvent.Failure -> failure = event
                else -> Unit // reasoning, usage, tool-related and state events are not reply text
            }
        }
        failure?.let { throw ProfessorAiException(it.message, it.code, it.errorCode) }
        return output.toString().trim().also {
            check(it.isNotEmpty()) { "AndroidHarness AI returned an empty Professor reply" }
        }
    }
}

enum class ProfessorTurnRole { USER, ASSISTANT }

data class ProfessorConversationTurn(
    val role: ProfessorTurnRole,
    val text: String,
) {
    init {
        require(text.isNotBlank()) { "Conversation turn text cannot be blank" }
    }
}

class ProfessorAiException(
    message: String,
    val httpCode: Int? = null,
    val providerErrorCode: String? = null,
) : IllegalStateException(message)
