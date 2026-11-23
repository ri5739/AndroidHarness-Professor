package com.androidharness.app.professor.capture

import java.security.MessageDigest

/**
 * Stable, privacy-preserving identifiers derived from notification metadata.
 * Package scope separates messaging app variants; notification key is preferred
 * over display name so two conversations with identical names do not collide.
 * Android does not reliably expose a logged-in account ID in notifications.
 */
internal object ProfessorNotificationIdentity {
    fun accountScope(packageName: String): String = "notification:$packageName"

    fun conversationId(notificationKey: String?, conversationHint: String?): String {
        val identity = notificationKey?.takeIf { it.isNotBlank() }
            ?: conversationHint?.takeIf { it.isNotBlank() }
            ?: "unknown"
        return digest(identity.lowercase())
    }

    fun messageId(packageName: String, conversationId: String, body: String, postedAt: Long): String =
        digest(listOf(packageName, conversationId, body, postedAt.toString()).joinToString("|"))

    private fun digest(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
}
