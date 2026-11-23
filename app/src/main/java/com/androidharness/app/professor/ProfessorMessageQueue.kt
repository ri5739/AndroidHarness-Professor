package com.androidharness.app.professor

/**
 * Thread-safe in-memory queue primitive. Persistence and WorkManager wiring are intentionally
 * separate; this class only defines ordering/dedup semantics and can be tested on the JVM.
 */
class ProfessorMessageQueue {
    enum class EnqueueResult { ADDED, DUPLICATE, PAUSED }

    private val lock = Any()
    private val pending = LinkedHashMap<ConversationScope, ArrayDeque<IncomingMessage>>()
    private val knownIds = HashMap<ConversationScope, MutableSet<String>>()
    private val completedIds = HashMap<ConversationScope, MutableSet<String>>()
    private var paused = false

    fun setPaused(value: Boolean) = synchronized(lock) { paused = value }
    fun isPaused(): Boolean = synchronized(lock) { paused }

    fun enqueue(message: IncomingMessage): EnqueueResult = synchronized(lock) {
        if (paused) return@synchronized EnqueueResult.PAUSED
        val ids = knownIds.getOrPut(message.scope) { mutableSetOf() }
        if (message.messageId in ids || message.messageId in completedIds[message.scope].orEmpty()) {
            return@synchronized EnqueueResult.DUPLICATE
        }
        ids += message.messageId
        pending.getOrPut(message.scope) { ArrayDeque() }.addLast(message)
        EnqueueResult.ADDED
    }

    /** FIFO within each conversation; other conversations never share an ordered queue. */
    fun poll(scope: ConversationScope): IncomingMessage? = synchronized(lock) {
        if (paused) return@synchronized null
        val queue = pending[scope] ?: return@synchronized null
        val next = queue.removeFirstOrNull()
        if (queue.isEmpty()) pending.remove(scope)
        next
    }

    fun markCompleted(message: IncomingMessage) = synchronized(lock) {
        knownIds[message.scope]?.remove(message.messageId)
        completedIds.getOrPut(message.scope) { mutableSetOf() }.add(message.messageId)
    }

    fun pendingCount(scope: ConversationScope? = null): Int = synchronized(lock) {
        if (scope != null) pending[scope]?.size ?: 0 else pending.values.sumOf { it.size }
    }

    fun clear(scope: ConversationScope? = null) = synchronized(lock) {
        if (scope == null) {
            pending.clear()
            knownIds.clear()
            completedIds.clear()
        } else {
            pending.remove(scope)
            knownIds.remove(scope)
            completedIds.remove(scope)
        }
    }
}
