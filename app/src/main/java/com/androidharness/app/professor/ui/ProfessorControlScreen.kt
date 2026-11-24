package com.androidharness.app.professor.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
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
            android.service.notification.NotificationListenerService::class.java.let {
                android.provider.Settings.Secure.getString(
                    context.contentResolver,
                    "enabled_notification_listeners",
                )?.split(':')?.any { component ->
                    component.startsWith(context.packageName + "/")
                } == true
            },
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

    val drafts by container.professorPersistence.activeDraftsFlow()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        androidx.compose.material3.TextButton(onClick = onBack) { Text("Back") }
        Text("Agent Professor", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Incoming notifications from supported messaging apps are captured for reply drafting. " +
                "This control screen does not send messages.",
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
        Text("Reply drafts", style = MaterialTheme.typography.titleLarge)
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
            onDelete = { draft ->
                coroutineScope.launch {
                    container.professorPersistence.deleteDraft(draft.draftId)
                }
            },
            modifier = Modifier.weight(1f),
        )
    }
}
