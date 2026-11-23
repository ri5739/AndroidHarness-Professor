package com.androidharness.app.professor

import android.content.Context
import androidx.work.*
import java.security.MessageDigest

/** Schedules one serial processor per account/conversation; database remains the source of truth. */
object ProfessorWorkScheduler {
    fun enqueue(context: Context, scope: ConversationScope) {
        val identity = listOf(scope.platform.name, scope.accountId, scope.conversationId)
            .joinToString("") { "${it.length}:$it" }
        val key = MessageDigest.getInstance("SHA-256").digest(identity.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        val request = OneTimeWorkRequestBuilder<ProfessorWorker>()
            .setInputData(workDataOf(
                ProfessorWorker.KEY_PLATFORM to scope.platform.name,
                ProfessorWorker.KEY_ACCOUNT to scope.accountId,
                ProfessorWorker.KEY_CONVERSATION to scope.conversationId,
            ))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, java.util.concurrent.TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork("professor-$key", ExistingWorkPolicy.KEEP, request)
    }

    fun enqueueAll(context: Context) {
        // Queue scopes are intentionally scheduled by the capture path when messages arrive.
        // Startup recovery is handled by HarnessApp and does not guess identities.
    }
}
