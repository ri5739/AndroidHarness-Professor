package com.androidharness.app.professor.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.androidharness.app.AppContainer
import com.androidharness.app.professor.DraftStatus
import com.androidharness.app.professor.MessagingPlatform
import com.androidharness.app.professor.ProfessorPlatformPreferences
import com.androidharness.app.professor.capture.ProfessorNotificationReplyActions
import kotlinx.coroutines.launch

@Composable
fun ProfessorControlScreen(
    container: AppContainer,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    var notificationAccess by remember {
        mutableStateOf(
            Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners",
            )?.split(':')?.any { component ->
                component.startsWith(context.packageName + "/")
            } == true,
        )
    }
    fun refreshPermission() {
        notificationAccess = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners",
        )?.split(':')?.any { component ->
            component.startsWith(context.packageName + "/")
        } == true
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshPermission()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var platformStates by remember {
        mutableStateOf(MessagingPlatform.entries.associateWith { ProfessorPlatformPreferences.isCaptureEnabled(context, it) })
    }
    val drafts by container.professorPersistence.activeDraftsFlow()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        androidx.compose.material3.TextButton(onClick = onBack) { Text("Back") }
        Text("Agent Professor", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Ordinary messages are replied to automatically when the messaging app exposes a supported notification reply action. Personal or confidential replies wait for your approval.",
            style = MaterialTheme.typography.bodyMedium,
        )
        ProfessorNotificationPermissionPanel(
            granted = notificationAccess,
            onRequestPermission = {
                runCatching {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                }
            },
        )
        Text("Platform connections", style = MaterialTheme.typography.titleLarge)
        Text(
            "Enable notification capture separately for each messaging app. Automatic sending requires that app to expose an inline reply action in its notification.",
            style = MaterialTheme.typography.bodySmall,
        )
        MessagingPlatform.entries.forEach { platform ->
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Text(platform.displayName(), modifier = Modifier.weight(1f))
                Switch(
                    checked = platformStates[platform] == true,
                    onCheckedChange = { enabled ->
                        ProfessorPlatformPreferences.setCaptureEnabled(context, platform, enabled)
                        platformStates = platformStates + (platform to enabled)
                    },
                )
            }
        }
        Text("Replies awaiting action", style = MaterialTheme.typography.titleLarge)
        ProfessorDraftManagement(
            drafts = drafts,
            onSaveEdit = { draft, text ->
                coroutineScope.launch {
                    container.professorPersistence.saveDraft(draft.copy(text = text))
                }
            },
            onMarkReviewed = { draft ->
                coroutineScope.launch {
                    container.professorPersistence.setDraftStatus(draft.draftId, DraftStatus.READY)
                }
            },
            onApproveAndSend = { draft ->
                coroutineScope.launch {
                    val sent = ProfessorNotificationReplyActions.send(
                        context, draft.inReplyToMessageId, draft.text,
                    )
                    if (sent) {
                        container.professorPersistence.setDraftStatus(draft.draftId, DraftStatus.SENT)
                    }
                }
            },
            onDelete = { draft ->
                coroutineScope.launch {
                    container.professorPersistence.deleteDraft(draft.draftId)
                }
            },
            modifier = Modifier.weight(1f),
        )
    }
}

private fun MessagingPlatform.displayName(): String = when (this) {
    MessagingPlatform.WHATSAPP_PERSONAL -> "WhatsApp"
    MessagingPlatform.WHATSAPP_BUSINESS -> "WhatsApp Business"
    MessagingPlatform.MESSENGER_PERSONAL -> "Messenger"
    MessagingPlatform.META_BUSINESS_MESSENGER -> "Meta Business Suite"
    MessagingPlatform.INSTAGRAM_DM -> "Instagram"
}
