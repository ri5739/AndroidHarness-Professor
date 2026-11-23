package com.androidharness.app.professor

import org.junit.Assert.assertEquals
import org.junit.Test

class ProfessorContextWindowTest {
    private val scope = ConversationScope(MessagingPlatform.WHATSAPP_PERSONAL, "account", "contact")
    private fun message(id: String, text: String, time: Long, s: ConversationScope = scope) =
        IncomingMessage(id, s, "sender", text, time)

    @Test fun chronologicalContextAlwaysEndsWithClaimedMessage() {
        val turns = ProfessorContextWindow.build(
            listOf(message("2", "second", 20), message("1", "first", 10)),
            message("3", "current", 30),
        )
        assertEquals(listOf("first", "second", "current"), turns.map { it.text })
        assertEquals(ProfessorTurnRole.USER, turns.last().role)
    }

    @Test fun contextDoesNotCrossConversationScopeAndRespectsBound() {
        val other = ConversationScope(MessagingPlatform.INSTAGRAM_DM, "account", "different")
        val turns = ProfessorContextWindow.build(
            listOf(message("1", "older", 1), message("2", "newer", 2), message("x", "leak", 3, other)),
            message("3", "current", 4),
            maxTurns = 2,
        )
        assertEquals(listOf("newer", "current"), turns.map { it.text })
    }

    @Test fun duplicateCurrentMessageIsNotRepeated() {
        val current = message("3", "current", 3)
        val turns = ProfessorContextWindow.build(listOf(current, current), current)
        assertEquals(listOf("current"), turns.map { it.text })
    }
}
