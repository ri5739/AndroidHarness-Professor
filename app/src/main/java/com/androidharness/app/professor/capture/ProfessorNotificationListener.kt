package com.androidharness.app.professor.capture

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.app.Notification
import android.os.Bundle
import com.androidharness.app.HarnessApp
import com.androidharness.app.professor.ConversationScope
import com.androidharness.app.professor.IncomingMessage
import com.androidharness.app.professor.ProfessorWorkScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Opt-in notification capture. Ordinary replies can be delivered only through a
 * free-form RemoteInput action explicitly supplied by the messaging app. No UI
 * automation, accessibility scraping, or guessed send endpoint is used.
 */
class ProfessorNotificationListener : NotificationListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        if (sbn.packageName == packageName) return
        val platform = ProfessorNotificationClassifier.platformForPackage(sbn.packageName) ?: return
        if (!com.androidharness.app.professor.ProfessorPlatformPreferences.isCaptureEnabled(this, platform)) return
        val notification = sbn.notification ?: return
        val extras: Bundle = notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
        val captured = ProfessorNotificationClassifier.normalize(
            sbn.packageName, title, text, sbn.postTime
        ) ?: return
        val conversationName = captured.conversationHint?.takeIf { it.isNotBlank() } ?: "unknown"
        val conversationId = ProfessorNotificationIdentity.conversationId(sbn.key, conversationName)
        // Account identity is not exposed reliably in notification metadata; keep a
        // conservative local default until the user explicitly maps an account.
        val conversationScope = ConversationScope(platform, ProfessorNotificationIdentity.accountScope(sbn.packageName), conversationId)
        val messageId = ProfessorNotificationIdentity.messageId(sbn.packageName, conversationId, captured.body, sbn.postTime)
        // Save only the app-provided inline reply action; absence of one means no auto-send.
        ProfessorNotificationReplyActions.register(messageId, notification)
        val incoming = IncomingMessage(messageId, conversationScope, conversationName.take(256), captured.body, sbn.postTime)
        scope.launch {
            val app = applicationContext as? HarnessApp ?: return@launch
            if (app.container.professorPersistence.enqueue(incoming)) {
                ProfessorWorkScheduler.enqueue(applicationContext, conversationScope)
            }
        }
    }
}
