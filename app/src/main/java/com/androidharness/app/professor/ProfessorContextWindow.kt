package com.androidharness.app.professor

/**
 * Conservative context window from captured incoming messages only. It never crosses
 * platform/account/conversation boundaries and always places the claimed message last.
 * Outgoing messages are not available from notification capture and are not fabricated.
 */
object ProfessorContextWindow {
    const val DEFAULT_MAX_TURNS = 12

    fun build(
        history: List<IncomingMessage>,
        current: IncomingMessage,
        maxTurns: Int = DEFAULT_MAX_TURNS,
    ): List<ProfessorConversationTurn> {
        require(maxTurns >= 1)
        val scoped = history.asSequence()
            .filter { it.scope == current.scope && it.messageId != current.messageId }
            .distinctBy { it.messageId }
            .sortedWith(compareBy<IncomingMessage> { it.receivedAtEpochMillis }.thenBy { it.messageId })
            .take((maxTurns - 1).coerceAtLeast(0))
            .map { ProfessorConversationTurn(ProfessorTurnRole.USER, it.text) }
            .toList()
        return (scoped + ProfessorConversationTurn(ProfessorTurnRole.USER, current.text)).takeLast(maxTurns)
    }
}
