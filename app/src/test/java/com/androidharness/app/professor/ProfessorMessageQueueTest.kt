package com.androidharness.app.professor

import org.junit.Assert.*
import org.junit.Test

class ProfessorMessageQueueTest {
    private val scopeA = ConversationScope(MessagingPlatform.WHATSAPP_PERSONAL, "acct", "contact-a")
    private val scopeB = ConversationScope(MessagingPlatform.WHATSAPP_PERSONAL, "acct", "contact-b")
    private fun msg(id: String, scope: ConversationScope = scopeA) =
        IncomingMessage(id, scope, scope.conversationId, id, 1L)

    @Test fun fifoAndDedupAreScopedPerConversation() {
        val q = ProfessorMessageQueue()
        assertEquals(ProfessorMessageQueue.EnqueueResult.ADDED, q.enqueue(msg("m1")))
        assertEquals(ProfessorMessageQueue.EnqueueResult.DUPLICATE, q.enqueue(msg("m1")))
        q.enqueue(msg("m2"))
        q.enqueue(msg("m1", scopeB))
        assertEquals("m1", q.poll(scopeA)?.messageId)
        assertEquals("m2", q.poll(scopeA)?.messageId)
        assertEquals("m1", q.poll(scopeB)?.messageId)
    }

    @Test fun concurrentDuplicateEnqueueOnlyAddsOneMessage() {
        val q = ProfessorMessageQueue()
        val workers = (1..32).map {
            Thread { q.enqueue(msg("same-id")) }
        }
        workers.forEach(Thread::start)
        workers.forEach(Thread::join)
        assertEquals(1, q.pendingCount(scopeA))
        assertEquals("same-id", q.poll(scopeA)?.messageId)
        assertNull(q.poll(scopeA))
    }

    @Test fun pauseBlocksEnqueueAndPoll() {
        val q = ProfessorMessageQueue()
        q.setPaused(true)
        assertEquals(ProfessorMessageQueue.EnqueueResult.PAUSED, q.enqueue(msg("x")))
        assertNull(q.poll(scopeA))
        q.setPaused(false)
        assertEquals(ProfessorMessageQueue.EnqueueResult.ADDED, q.enqueue(msg("x")))
        assertEquals("x", q.poll(scopeA)?.messageId)
    }

    @Test fun completedMessagesCannotBeEnqueuedAgain() {
        val q = ProfessorMessageQueue()
        val message = msg("done")
        q.enqueue(message)
        assertEquals(message, q.poll(scopeA))
        q.markCompleted(message)
        assertEquals(ProfessorMessageQueue.EnqueueResult.DUPLICATE, q.enqueue(message))
    }

    @Test fun clearingOneConversationDoesNotAffectAnother() {
        val q = ProfessorMessageQueue()
        q.enqueue(msg("a"))
        q.enqueue(msg("b", scopeB))
        q.clear(scopeA)
        assertEquals(0, q.pendingCount(scopeA))
        assertEquals(1, q.pendingCount(scopeB))
    }
}
