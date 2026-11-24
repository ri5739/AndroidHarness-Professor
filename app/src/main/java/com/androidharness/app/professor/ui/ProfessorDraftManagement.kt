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
import com.androidharness.app.professor.DraftStatus
import com.androidharness.app.professor.ReplyDraft

/** Draft inbox with explicit owner approval before a held reply can be sent. */
@Composable
fun ProfessorDraftManagement(
    drafts: List<ReplyDraft>,
    onSaveEdit: (ReplyDraft, String) -> Unit,
    onMarkReviewed: (ReplyDraft) -> Unit,
    onDelete: (ReplyDraft) -> Unit,
    modifier: Modifier = Modifier,
) {
    var editingDraft by remember { mutableStateOf<ReplyDraft?>(null) }
    var editedText by remember { mutableStateOf("") }

    Column(modifier = modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Agent Professor replies", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Replies are saved as drafts. Sending is currently disabled until a verified platform adapter and all safety checks are available.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (drafts.isEmpty()) {
            Text("No pending replies.", style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(drafts, key = { it.draftId }) { draft ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                "${draft.scope.platform.name} · ${draft.status.name}",
                                style = MaterialTheme.typography.labelLarge,
                            )
                            Text(
                                "Account: ${draft.scope.accountId} · Conversation: ${draft.scope.conversationId}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(draft.text, style = MaterialTheme.typography.bodyLarge)
                            if (draft.riskFlags.isNotEmpty()) {
                                Text(
                                    "Owner approval required: ${draft.riskFlags.joinToString()}",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            Text(
                                "Confidence: ${(draft.confidence * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = {
                                    editingDraft = draft
                                    editedText = draft.text
                                }) { Text("Edit") }
                                if (draft.status == DraftStatus.PENDING || draft.status == DraftStatus.NEEDS_REVIEW) {
                                    Button(onClick = { onMarkReviewed(draft) }) {
                                        Text("Mark reviewed")
                                    }
                                }
                                TextButton(onClick = { onDelete(draft) }) { Text("Delete") }
                            }
                        }
                    }
                }
            }
        }
    }

    val current = editingDraft
    if (current != null) {
        AlertDialog(
            onDismissRequest = { editingDraft = null },
            title = { Text("Edit reply") },
            text = {
                OutlinedTextField(
                    value = editedText,
                    onValueChange = { editedText = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 8,
                    label = { Text("Reply text") },
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveEdit(current, editedText)
                        editingDraft = null
                    },
                    enabled = editedText.isNotBlank() && editedText != current.text,
                ) { Text("Save edit") }
            },
            dismissButton = {
                TextButton(onClick = { editingDraft = null }) { Text("Cancel") }
            },
        )
    }

}
