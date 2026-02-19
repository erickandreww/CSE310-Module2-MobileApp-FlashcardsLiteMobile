package com.example.flashcardslitemobile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    decks: List<CloudDeck>,
    onAddDeck: (String) -> Unit,
    onOpenDeck: (CloudDeck) -> Unit,
    onUpdateDeck: (deckId: String, newName: String) -> Unit,
    onDeleteDeck: (String) -> Unit,
    onSignOut: () -> Unit,
    currentUserEmail: String?,
    statusMessage: String,
    modifier: Modifier = Modifier
) {
    // Deck Name the user typed
    var deckName by remember { mutableStateOf("") }

    // confirm deck deletion when not null
    var deckToDelete by remember { mutableStateOf<CloudDeck?>(null) }

    // this is for renaming (when not null it means the dialog is open)
    var deckToRename by remember { mutableStateOf<CloudDeck?>(null) }

    // text inside the rename dialog input
    var renameText by remember { mutableStateOf("") }

    // feedback massage (local validation messages)
    var message by remember { mutableStateOf("") }

    // Column and it formating
    Column(modifier = modifier.padding(16.dp).fillMaxSize()
    ) {
        // simple title
        Text(text = "Flashcards Lite Mobile")
        Spacer(modifier = Modifier.height(12.dp))

        // show who is logged in (if nobody, show default text)
        Text("User: ${currentUserEmail ?: "Not logged in"}")

        // this is the "status" from ViewModel (mostly for buttons/actions)
        if (statusMessage.isNotBlank()) Text(statusMessage)

        Spacer(modifier = Modifier.height(12.dp))

        // input where the user types the deck name
        OutlinedTextField(
            value = deckName,
            onValueChange = {
                // limit text so user can't type a huge deck name
                deckName = limitText(it, maxChars = 30, maxLines = 1)
                // clear local message when user starts typing again
                message = ""
            },
            label = { Text("New deck name") },
            modifier = modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Create a deck after checking the validation
        Button(
            onClick = {
                val name = deckName.trim()

                // basic validation
                when {
                    name.isBlank() -> message = "Name can't be empty"

                    // check if the name already exists
                    decks.any { it.name.equals(name, true) } ->
                        message = "Deck name already exits"

                    // add deck to the list (it will appear when firestore listener updates)
                    else -> {
                        onAddDeck(name)
                        deckName = "" // clear input
                        message = ""  // clear message
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create Deck")
        }

        // show the result message (local message only)
        if (message.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(message)
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "Decks:")
        Spacer(modifier = Modifier.height(8.dp))

        // LazyColumn is better for lists that can grow
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // list of decks
            items(decks, key = { it.id }) { d ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // open Deck Screen
                    Button(
                        onClick = { onOpenDeck(d) },
                        modifier = Modifier.weight(1f)
                    ) { Text(d.name) }

                    // rename deck (opens dialog)
                    Button(
                        onClick = {
                            deckToRename = d
                            renameText = d.name
                        },
                        modifier = Modifier.width(80.dp)
                    ) { Text("Edit") }

                    // delete deck button (opens confirmation dialog)
                    Button(
                        onClick = { deckToDelete = d },
                        modifier = Modifier.width(90.dp)
                    ) { Text("Delete") }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // sign out button (just calls callback)
        Button(onClick = onSignOut, modifier = modifier.fillMaxWidth()) {
            Text("Sign Out")
        }
    }

    // Rename dialog appears only when deckToRename != null
    deckToRename?.let { deck ->
        AlertDialog(
            onDismissRequest = { deckToRename = null },
            title = { Text("Rename deck") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = {
                        // again limit text so it doesn't break the UI
                        renameText = limitText(it, maxChars = 40, maxLines = 1)
                    },
                    label = { Text("New name") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    val newName = renameText.trim()

                    // validate new name before calling update
                    when {
                        newName.isBlank() -> message = "Name can't be empty"

                        // avoid duplicates, but allow renaming the same deck to itself
                        decks.any { it.name.equals(newName, true) && it.id != deck.id } ->
                            message = "Deck name already exists"

                        else -> {
                            onUpdateDeck(deck.id, newName)
                            deckToRename = null // close dialog
                            message = ""        // clear message
                        }
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                Button(onClick = { deckToRename = null }) { Text("Cancel") }
            }
        )
    }

    // confirm deck deletion
    deckToDelete?.let { deck ->
        // alert notification
        AlertDialog(
            onDismissRequest = { deckToDelete = null },
            title = { Text("Delete deck?") },
            text = { Text("Delete '${deck.name}'? This will also delete its cards.") },

            // confirms deck deletion
            confirmButton = {
                Button(onClick = {
                    onDeleteDeck(deck.id)
                    deckToDelete = null // close dialog
                }) { Text("Yes") }
            },

            // dismiss deck deletion
            dismissButton = {
                Button(onClick = { deckToDelete = null }) { Text("No") }
            }
        )
    }
}