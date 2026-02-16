package com.example.flashcardslitemobile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


@Composable
fun FirestoreTestScreen(
    testNotes: List<Pair<String, String>>,
    onAddNote: (String) -> Unit,
    onUpdateNote: (String, String) -> Unit,
    onDeleteNote: (String) -> Unit,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
    currentUserEmail: String?,
    statusMessage: String,
    modifier: Modifier = Modifier
) {
    var note by remember { mutableStateOf("") }

    var message by remember { mutableStateOf("") }

    Column(modifier = modifier
        .padding(16.dp)
        .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Firestore Test Screen")
        Spacer(modifier = Modifier.height(12.dp))

        Text("Current user: ${ currentUserEmail ?: "Not logged in"}")

        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("New Note") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                val newNote = note.trim()
                if (newNote.isBlank()) {
                    message = "Note can't be empty"
                } else {
                    onAddNote(note)
                    message = "Note Created!"
                    note = ""
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add Note")
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "Notes:")

        if (testNotes.isEmpty()) {
            Text("No Notes yet!")
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(testNotes, key = { it.first }) { n ->
                    val noteId = n.first
                    val noteText = n.second

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = noteText, modifier = Modifier.weight(1f))

                        Button(onClick = { onUpdateNote(noteId, "$noteText (edited)") },
                        ) { Text("Edit")}

                        Button(onClick = { onDeleteNote(noteId) },
                        ) { Text("Delete")}
                    }
                }
            }
        }

        if (message.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(message)
        }
        if (statusMessage.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(statusMessage)
        }

        Button(
            onClick = onRefresh,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Refresh Notes")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onBack) { Text("Back") }
    }
}