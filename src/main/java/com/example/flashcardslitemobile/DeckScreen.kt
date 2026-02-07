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
fun DeckScreen(
    deck: Deck,
    cards: List<Card>,
    onAddCard: (deckId: Int, front: String, back: String) -> Unit,
    onBack: () -> Unit,
    onReview: (Int) -> Unit,
    onUpdateCard: (Card) -> Unit,
    onDeleteCard: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // what the user types in the Front and Back input
    var front by remember { mutableStateOf("") }
    var back by remember { mutableStateOf("") }

    // if this is null we are adding a new card and
    // if this has an id then we are editing that specific card
    var editingCardId by remember { mutableStateOf<Int?>(null) }

    // feedback message for the user
    var message by remember { mutableStateOf("") }

    // get only the cards that belong to this deck
    val deckCards = cards.filter { it.deckId == deck.id }

    // Deck screen layout
    Column(modifier = modifier
        .padding(16.dp)
        .fillMaxSize()
    ) {
        // header info
        Text(text = "Deck: ${deck.name}")
        Text("Cards in this deck: ${deckCards.size}")
        Spacer(modifier = Modifier.height(12.dp))

        // input for the card front
        OutlinedTextField(
            value = front,
            onValueChange = { front = it; message = "" },
            label = { Text("Front") }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // input for the card back
        OutlinedTextField(
            value = back,
            onValueChange = { back = it; message = "" },
            label = { Text("Back") }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // this button works for both: Add Card and Save Changes (edit mode)
        Button(onClick = {
            val f = front.trim()
            val b = back.trim()

            if (f.isBlank() || b.isBlank()) {
                message = "Front and Back can't be empty"
                return@Button
            }

            // check duplicates, but ignore the card we are editing (so it doesn't block itself)
            val duplicate = deckCards
                .any { it.front.equals(f, true)
                        && it.back.equals(b, true)
                        && it.id != editingCardId
                }
            if (duplicate) {
                message = "This card already exists."
                return@Button
            }

            // if no card is selected, add new card
            if (editingCardId == null) {
                // add new card
                onAddCard(deck.id, f, b)
                message = "Card added!"
            }
            else {
                // if a card is selected, update that card
                val updated = deckCards.first { it.id == editingCardId }.copy(front = f, back = b)
                onUpdateCard(updated)
                message = "Card updated!"
            }

            // clear inputs and exit edit mode
            front = ""
            back = ""
            editingCardId = null
        }) {
            // change the button text depending on add/edit mode
            Text(if (editingCardId == null) "Add Card" else "Save Changes")
        }

        // this button is only showed when the user is editing a card
        if(editingCardId != null) {
            Spacer(modifier = Modifier.height(8.dp))
            // cancel edit and reset inputs
            Button(onClick = {
                front = ""
                back = ""
                editingCardId = null
                message = "Edit cancelled"
            }) {
                Text("Cancel Edit")
            }
        }

        // show feedback message if there is one
        if (message.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(message)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Cards:")
        Spacer(modifier = Modifier.height(8.dp))

        // list of cards in this deck
        if (deckCards.isEmpty()) {
            Text("No Cards yet!")
        } else {
            // LazyColumn = better list for many cards
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(deckCards, key = { it.id }) { c ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // show the card text
                        Text(
                            text = "${c.front} -> ${c.back}",
                            modifier = Modifier.weight(1f)
                        )

                        // put the card values in the inputs so the user can edit
                        Button(onClick = {
                            front = c.front
                            back = c.back
                            editingCardId = c.id
                            message = "Editing card ${c.id}"
                        }) {
                            Text("Edit")
                        }

                        // delete the card
                        Button(onClick = {
                            if (editingCardId == c.id) {
                                front = ""
                                back = ""
                                editingCardId = null
                                message = ""
                            }
                            onDeleteCard(c.id)
                        }) {
                            Text("Delete")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // go to review screen for deck
        Button(onClick = { onReview(deck.id) }) { Text("Review Due Cards") }

        Spacer(modifier = Modifier.height(16.dp))

        // go back to Home Screen
        Button(onClick = onBack) { Text("Back") }
    }
}
