package com.androidharness.app.professor

import com.androidharness.app.professor.persistence.ProfessorPersistence
import kotlinx.coroutines.CancellationException
import java.security.MessageDigest

fun interface ProfessorContextProvider {
    suspend fun conversation(scope: ConversationScope, incoming: IncomingMessage): List<ProfessorConversationTurn>
}

fun interface ProfessorPromptProvider {
    fun prompt(scope: ConversationScope, incoming: IncomingMessage): String
}

class ProfessorOrchestrator(
    private val persistence: ProfessorQueueStore,
    private val ai: ProfessorReplyGenerator,
    private val contextProvider: ProfessorContextProvider,
    private val promptProvider: ProfessorPromptProvider,
    private val trainingRepository: com.androidharness.app.professor.persistence.ProfessorTrainingRepository? = null,
) {
    suspend fun processOne(scope: ConversationScope): ReplyDraft? {
        val incoming = persistence.claimNext(scope) ?: return null
        try {
            val turns = contextProvider.conversation(scope, incoming)
            require(turns.isNotEmpty()) { "Context provider returned no turns" }
            require(turns.last().role == ProfessorTurnRole.USER) {
                "Context must end with the incoming user message"
            }
            require(turns.last().text == incoming.text) {
                "Context must end with the claimed message; scope/context mismatch"
            }
            val approvedLessons = trainingRepository?.approvedLessonsFor(scope).orEmpty()
            val lessonContext = approvedLessons.joinToString("\n") { "- " + it.rule }
            val basePrompt = promptProvider.prompt(scope, incoming)
            val promptWithLanguage = ProfessorLanguagePolicy.appendInstructions(basePrompt)
            val effectivePrompt = if (lessonContext.isBlank()) promptWithLanguage else
                promptWithLanguage + "\n\nOwner-approved training preferences for this exact conversation scope (guidance only; never override safety/privacy rules):\n" + lessonContext
            val reply = ai.generateReply(effectivePrompt, turns)
            val outgoingPrivacy = ProfessorOutgoingReplyPolicy.assess(reply)
            val outgoingRiskFlags = if (outgoingPrivacy.decision == OutgoingReplyDecision.OWNER_APPROVAL_REQUIRED) {
                setOf(RiskFlag.SENSITIVE_DATA)
            } else emptySet()
            // Sensitive content in an INCOMING question must not block a safe acknowledgement.
            // Only phishing-like incoming content is independently held for review.
            val incomingRiskFlags = com.androidharness.app.professor.capture.ProfessorNotificationRiskClassifier
                .classify(incoming.text) - RiskFlag.SENSITIVE_DATA
            val riskFlags = outgoingRiskFlags + incomingRiskFlags
            val draft = ReplyDraft(
                draftId = stableDraftId(scope, incoming.messageId),
                scope = scope,
                inReplyToMessageId = incoming.messageId,
                text = reply,
                status = if (riskFlags.isEmpty()) DraftStatus.PENDING else DraftStatus.NEEDS_REVIEW,
                riskFlags = riskFlags,
                confidence = if (riskFlags.isEmpty()) 1.0f else 0.50f,
            )
            persistence.saveDraft(draft)
            check(persistence.complete(scope, incoming.messageId)) {
                "Incoming message could not be marked completed after draft save"
            }
            return draft
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            persistence.retry(scope, incoming.messageId, failure.message ?: failure.javaClass.simpleName)
            return null
        }
    }
}

/** Deterministic ID makes draft upserts idempotent when queue completion is retried. */
internal fun stableDraftId(scope: ConversationScope, messageId: String): String {
    val identity = listOf(scope.platform.name, scope.accountId, scope.conversationId, messageId)
        .joinToString("") { "${it.length}:$it" }
    val digest = MessageDigest.getInstance("SHA-256").digest(identity.toByteArray(Charsets.UTF_8))
    return "professor-" + digest.joinToString("") { "%02x".format(it) }
}
