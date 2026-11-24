package com.androidharness.app.professor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.androidharness.app.professor.persistence.ProfessorTrainingLessonEntity
import com.androidharness.app.professor.persistence.ProfessorTrainingAuditEntity
import com.androidharness.app.professor.persistence.ProfessorTrainingTurnEntity
import com.androidharness.app.professor.ProfessorTrainingConflictDetector
import com.androidharness.app.professor.ConversationScope
import com.androidharness.app.professor.MessagingPlatform

/**
 * Local owner-facing training workspace. It is deliberately not exposed through notification
 * intents or external conversation routes. Host supplies the AndroidHarness AI Core callbacks.
 */
@Composable
fun ProfessorTrainingChatScreen(
    history: List<ProfessorTrainingTurnEntity>,
    lessons: List<ProfessorTrainingLessonEntity>,
    auditEvents: List<ProfessorTrainingAuditEntity>,
    onTeach: (String, ConversationScope?) -> Unit,
    onApprove: (String) -> Unit,
    onEdit: (String, String) -> Unit,
    onRestore: (String, String) -> Unit,
    onPause: (String) -> Unit,
    onResume: (String) -> Unit,
    onDelete: (String) -> Unit,
    onReset: () -> Unit,
    onClearHistory: () -> Unit,
    onSimulate: (String, String?, ConversationScope?) -> Unit,
    simulationPreview: String?,
    aiCoreReady: Boolean,
    modifier: Modifier = Modifier,
) {
    var message by remember { mutableStateOf("") }
    var sample by remember { mutableStateOf("") }
    var proposedRule by remember { mutableStateOf("") }
    var scopePlatform by remember { mutableStateOf("") }
    var scopeAccountId by remember { mutableStateOf("") }
    var scopeConversationId by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<ProfessorTrainingLessonEntity?>(null) }
    var editText by remember { mutableStateOf("") }
    var showReset by remember { mutableStateOf(false) }
    var showClearHistory by remember { mutableStateOf(false) }
    val hasAnyScopeField = scopePlatform.isNotBlank() || scopeAccountId.isNotBlank() || scopeConversationId.isNotBlank()
    val selectedScope = if (!hasAnyScopeField) null else runCatching {
        ConversationScope(
            MessagingPlatform.valueOf(scopePlatform.trim().uppercase()),
            scopeAccountId.trim(),
            scopeConversationId.trim(),
        )
    }.getOrNull()
    val scopeIsValid = !hasAnyScopeField || selectedScope != null
    val activeLessons = lessons.filter { it.state == "APPROVED" || it.state == "PROPOSED" }
    val globalLessons = activeLessons.filter { it.scopePlatform == null && it.accountId == null && it.conversationId == null }
    val effectiveRuleGroups = buildList {
        if (globalLessons.isNotEmpty()) add(globalLessons)
        activeLessons.filter { it.scopePlatform != null }
            .groupBy { Triple(it.scopePlatform, it.accountId, it.conversationId) }
            .values.forEach { scoped -> add(globalLessons + scoped) }
    }
    val scopedConflicts = effectiveRuleGroups.flatMap { group ->
        ProfessorTrainingConflictDetector.findConflicts(group.map { it.rule }).mapNotNull { conflict ->
            val first = group.firstOrNull { it.rule == conflict.firstRule }
            val second = group.firstOrNull { it.rule == conflict.secondRule }
            if (first != null && second != null) first to second else null
        }
    }.distinctBy { it.first.lessonId to it.second.lessonId }

    LazyColumn(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Training Chat", style = MaterialTheme.typography.headlineSmall)
            Text(
                "শুধু মালিকের শেখানো নিয়ম এখানে প্রস্তাব হবে। অনুমোদন না দেওয়া পর্যন্ত কোনো lesson কার্যকর হবে না।",
                style = MaterialTheme.typography.bodyMedium,
            )
            if (!aiCoreReady) {
                Text(
                    "AndroidHarness AI Core সংযোগ প্রস্তুত নয়। AI শেখানো সাময়িকভাবে বন্ধ; সংরক্ষিত lesson ব্যবস্থাপনা চালু আছে।",
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("মালিকের নির্দেশনা", style = MaterialTheme.typography.titleMedium)
                    Text("Lesson scope (সব ফাঁকা রাখলে Global)", style = MaterialTheme.typography.labelMedium)
                    OutlinedTextField(
                        value = scopePlatform,
                        onValueChange = { scopePlatform = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Platform (যেমন WHATSAPP_PERSONAL)") },
                        supportingText = { Text(MessagingPlatform.values().joinToString(", ") { it.name }) },
                        singleLine = true,
                    )
                    OutlinedTextField(value = scopeAccountId, onValueChange = { scopeAccountId = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Account ID (Global হলে ফাঁকা রাখুন)") }, singleLine = true)
                    OutlinedTextField(value = scopeConversationId, onValueChange = { scopeConversationId = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Conversation / Contact ID (Global হলে ফাঁকা রাখুন)") }, singleLine = true)
                    if (!scopeIsValid) Text("Scoped lesson-এর জন্য সঠিক platform, account ID ও conversation/contact ID—তিনটিই দিতে হবে।", color = MaterialTheme.colorScheme.error)
                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("কথোপকথনে কী শেখাতে চান?") },
                        minLines = 2,
                    )
                    Button(
                        onClick = { onTeach(message.trim(), selectedScope); message = "" },
                        enabled = aiCoreReady && message.isNotBlank() && scopeIsValid,
                    ) { Text("প্রস্তাব তৈরি") }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Training history", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = { showClearHistory = true }, enabled = history.isNotEmpty()) { Text("Clear history") }
            }
        }
        items(history, key = { it.turnId }) { turn ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(10.dp)) {
                    Text(if (turn.role == "OWNER") "আপনি" else "Training Assistant", style = MaterialTheme.typography.labelMedium)
                    Text(if (turn.scopePlatform == null) "Global training" else "${turn.scopePlatform} / ${turn.accountId} / ${turn.conversationId}", style = MaterialTheme.typography.labelSmall)
                    Text(turn.text)
                }
            }
        }
        item {
            if (scopedConflicts.isNotEmpty()) {
                Text("Conflicting rules — choose which one to pause", style = MaterialTheme.typography.titleMedium)
            }
        }
        items(scopedConflicts, key = { it.first.lessonId + "|" + it.second.lessonId }) { pair ->
            val first = pair.first
            val second = pair.second
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("দুটি rule পরস্পর বিরোধী হতে পারে", color = MaterialTheme.colorScheme.error)
                    Text("১. " + first.rule)
                    Text("২. " + second.rule)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        first?.let { OutlinedButton(onClick = { second?.let { loser -> onPause(loser.lessonId) } }) { Text("Keep 1") } }
                        second?.let { OutlinedButton(onClick = { first?.let { loser -> onPause(loser.lessonId) } }) { Text("Keep 2") } }
                    }
                }
            }
        }
        item { Text("Lesson review & management", style = MaterialTheme.typography.titleMedium) }
        items(lessons, key = { it.lessonId }) { lesson ->
            val lessonEvents = auditEvents.filter { it.lessonId == lesson.lessonId }.take(4)
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(lesson.rule, style = MaterialTheme.typography.bodyLarge)
                    Text("Status: ${lesson.state} · Revision ${lesson.revision}", style = MaterialTheme.typography.labelMedium)
                    if (lessonEvents.isNotEmpty()) {
                        Text("Change history", style = MaterialTheme.typography.labelMedium)
                        lessonEvents.forEach { event ->
                            val detail = when (event.action) {
                                "EDITED" -> "v${event.revision}: ${event.ruleBefore} → ${event.ruleAfter}"
                                "DELETED", "RESET" -> "v${event.revision}: ${event.action.lowercase()} — ${event.ruleBefore}"
                                else -> "v${event.revision}: ${event.action.lowercase()} — ${event.ruleBefore}"
                            }
                            Text(detail, style = MaterialTheme.typography.bodySmall)
                            if (event.action == "EDITED") {
                                TextButton(onClick = { onRestore(lesson.lessonId, event.auditId) }) {
                                    Text("Restore revision " + event.revision)
                                }
                            }
                        }
                    }
                    Text(
                        if (lesson.scopePlatform == null) "Scope: Global owner rule"
                        else "Scope: ${lesson.scopePlatform} / ${lesson.accountId} / ${lesson.conversationId}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (lesson.state == "PROPOSED") {
                            Button(onClick = { onApprove(lesson.lessonId) }) { Text("Approve") }
                        }
                        OutlinedButton(onClick = { editing = lesson; editText = lesson.rule }) { Text("Edit") }
                        if (lesson.state == "APPROVED") OutlinedButton(onClick = { onPause(lesson.lessonId) }) { Text("Pause") }
                        if (lesson.state == "PAUSED") OutlinedButton(onClick = { onResume(lesson.lessonId) }) { Text("Resume") }
                        TextButton(onClick = { onDelete(lesson.lessonId) }) { Text("Delete") }
                    }
                }
            }
        }
        item {
            if (auditEvents.isNotEmpty()) {
                Text("Recent training audit", style = MaterialTheme.typography.titleMedium)
                auditEvents.take(30).forEach { event ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("${event.action} · revision ${event.revision}", style = MaterialTheme.typography.labelMedium)
                            Text("Lesson: ${event.lessonId}", style = MaterialTheme.typography.labelSmall)
                            if (event.ruleBefore.isNotBlank()) Text(event.ruleBefore, style = MaterialTheme.typography.bodySmall)
                            if (event.ruleAfter.isNotBlank() && event.ruleAfter != event.ruleBefore) {
                                Text("→ ${event.ruleAfter}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("No-send simulation", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = sample,
                        onValueChange = { sample = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Sample incoming message") },
                        minLines = 2,
                    )
                    OutlinedTextField(
                        value = proposedRule,
                        onValueChange = { proposedRule = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Optional proposed lesson") },
                    )
                    OutlinedButton(
                        onClick = { onSimulate(sample.trim(), proposedRule.takeIf(String::isNotBlank), selectedScope) },
                        enabled = aiCoreReady && sample.isNotBlank() && scopeIsValid,
                    ) { Text("Preview only — never send") }
                    simulationPreview?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
                    Text("এই simulation কোনো WhatsApp, Messenger বা Instagram message পাঠায় না।", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            OutlinedButton(onClick = { showReset = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Reset all training lessons")
            }
        }
    }

    editing?.let { lesson ->
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("Edit lesson") },
            text = {
                OutlinedTextField(value = editText, onValueChange = { editText = it }, minLines = 3)
            },
            confirmButton = {
                Button(onClick = { onEdit(lesson.lessonId, editText.trim()); editing = null }, enabled = editText.isNotBlank()) {
                    Text("Save as proposal")
                }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Cancel") } },
        )
    }
    if (showClearHistory) {
        AlertDialog(
            onDismissRequest = { showClearHistory = false },
            title = { Text("Clear training history?") },
            text = { Text("Training Chat-এর কথোপকথনের ইতিহাস মুছে যাবে। Approved lesson অক্ষত থাকবে।") },
            confirmButton = { Button(onClick = { onClearHistory(); showClearHistory = false }) { Text("Clear history") } },
            dismissButton = { TextButton(onClick = { showClearHistory = false }) { Text("Cancel") } },
        )
    }
    if (showReset) {
        AlertDialog(
            onDismissRequest = { showReset = false },
            title = { Text("Reset training lessons?") },
            text = { Text("সব saved lesson মুছে যাবে। Training history আলাদাভাবে থাকবে।") },
            confirmButton = { Button(onClick = { onReset(); showReset = false }) { Text("Reset lessons") } },
            dismissButton = { TextButton(onClick = { showReset = false }) { Text("Cancel") } },
        )
    }
}
