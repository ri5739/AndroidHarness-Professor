package com.androidharness.app.professor.capture

import android.app.Notification
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import java.util.concurrent.ConcurrentHashMap

/**
 * Holds the inline-reply PendingIntent exposed by the messaging app's notification.
 * It is deliberately process-local: if Android kills the process or the notification
 * has no free-form RemoteInput action, automatic delivery fails closed and the draft
 * remains available for owner review.
 */
object ProfessorNotificationReplyActions {
    private data class ReplyAction(
        val pendingIntent: android.app.PendingIntent,
        val remoteInputs: Array<RemoteInput>,
    )

    private val actions = ConcurrentHashMap<String, ReplyAction>()

    fun register(messageId: String, notification: Notification) {
        val action = notification.actions.orEmpty().firstOrNull { candidate ->
            candidate.actionIntent != null &&
                candidate.remoteInputs?.any { it.allowFreeFormInput } == true
        } ?: return
        val inputs = action.remoteInputs ?: return
        actions[messageId] = ReplyAction(action.actionIntent, inputs)
        if (actions.size > 500) {
            actions.keys.take(actions.size - 400).forEach(actions::remove)
        }
    }

    /** Reuse the captured action so a held reply can be sent after explicit owner approval. */
    fun send(context: Context, messageId: String, text: String): Boolean {
        if (text.isBlank()) return false
        val action = actions[messageId] ?: return false
        return try {
            val input = action.remoteInputs.firstOrNull { it.allowFreeFormInput } ?: return false
            val results = Bundle().apply { putCharSequence(input.resultKey, text) }
            val fillIn = Intent()
            RemoteInput.addResultsToIntent(action.remoteInputs, fillIn, results)
            action.pendingIntent.send(context, 0, fillIn)
            true
        } catch (failure: Exception) {
            Log.w("ProfessorReply", "Notification inline reply could not be sent", failure)
            false
        }
    }
}
