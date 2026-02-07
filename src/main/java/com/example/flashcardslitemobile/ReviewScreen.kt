package com.example.flashcardslitemobile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ReviewScreen(
    deck: Deck,
    cards: List<Card>,
    onUpdateCard: (Card) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // today's date as a String
    val today = todayString()

    // The list of cards to review in the session.
    // It only includes cards that are due today (or earlier).
    var sessionCards by remember(deck.id) {
        mutableStateOf(
            cards.filter { it.deckId == deck.id && it.dueDate <= today }
                .sortedBy { it.dueDate }
        )
    }

    // index = which card we're currently on
    var index by remember { mutableIntStateOf(0) }
    // if true, show the "back" of the card
    var showBack by remember { mutableStateOf(false) }
    // message shown after rating
    var message by remember { mutableStateOf("") }

    // counters for the session summary
    var againCount by remember { mutableIntStateOf(0) }
    var hardCount by remember { mutableIntStateOf(0) }
    var goodCount by remember { mutableIntStateOf(0) }
    var easyCount by remember { mutableIntStateOf(0) }

    // Review Screen layout
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // header info
        Text("Review: ${deck.name}")
        Spacer(modifier.height(8.dp))

        // progress and counts
        Text("Progress: ${minOf(index + 1, sessionCards.size)} / ${sessionCards.size}")
        Text("Again: $againCount Hard: $hardCount  Good: $goodCount  Easy: $easyCount")

        // If there are no due cards, show message and stop
        if (sessionCards.isEmpty()) {
            Text("No due cards today!")
            Button(onClick = onBack) { Text("Back") }
            return
        }

        // If we everything was reviewed, show a summary and stop the session
        if (index >= sessionCards.size) {
            Text("Done!")
            Text("Summary:")
            Text("Again: $againCount")
            Text("Hard: $hardCount")
            Text("Good: $goodCount")
            Text("Easy: $easyCount")
            Button(onClick = onBack) { Text("Back") }
            return
        }

        // current card that's being reviewed
        val card = sessionCards[index]

        Spacer(modifier.height(8.dp))

        // show card front side
        Text("Card ${index + 1} of ${sessionCards.size}")
        Spacer(modifier.height(4.dp))
        Text("Front: ${card.front}")

        Spacer(modifier.height(6.dp))

        // toggle showing the back side
        Button(onClick = { showBack = !showBack}) {
            Text(if (showBack) "Hide Back" else "Reveal Back")
        }

        // It only show if the user reveals it
        if (showBack) { Text("Back: ${card.back}") }

        Spacer(modifier.height(8.dp))

        // rating buttons
        Text("Rate:")
        // rating buttons
        // Row 1: Again - Hard
        Row (
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ){
            // rating button - Again
            Button(onClick = {
                // apply rating and generate updated card
                val updated = applyRatingLite(card, 0)

                // update the main cards list
                onUpdateCard(updated)

                // update the session copy too so the UI stays consistent in this session
                sessionCards = sessionCards.map { if (it.id == updated.id) updated else it }

                // update UI stuff
                againCount++
                message = "Again → due ${updated.dueDate}"
                showBack = false
                index++ // go to teh next card
            },
                modifier = Modifier.weight(1f)
            ) { Text("Again") }

            // rating button - Hard
            Button(onClick = {
                val updated = applyRatingLite(card, 1)
                onUpdateCard(updated)
                sessionCards = sessionCards.map { if (it.id == updated.id) updated else it }
                hardCount++
                message = "Hard → due ${updated.dueDate}"
                showBack = false
                index++
            },
                modifier = Modifier
                    .weight(1f)
            ) { Text("Hard") }
        }

        // Row 2: Good - Easy
        Row (
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ){
            // rating button - Good
            Button(onClick = {
                val updated = applyRatingLite(card, 2)
                onUpdateCard(updated)
                sessionCards = sessionCards.map { if (it.id == updated.id) updated else it }
                goodCount++
                message = "Good → due ${updated.dueDate}"
                showBack = false
                index++
            },
                modifier = Modifier
                    .weight(1f)
            ) { Text("Good") }

            // rating button - Easy
            Button(onClick = {
                val updated = applyRatingLite(card, 3)
                onUpdateCard(updated)
                sessionCards = sessionCards.map { if (it.id == updated.id) updated else it }
                easyCount++
                message = "Easy → due ${updated.dueDate}"
                showBack = false
                index++
            }, modifier = Modifier
                .weight(1f)
            ) { Text("Easy") }
        }

        // show rating message (if any)
        if (message.isNotBlank()) { Text(message) }

        Spacer(modifier.height(8.dp))

        // restart button reloads due cards again and resets all counters
        Button(onClick = {
            val newToday = todayString()

            // rebuild the session list based on what is due right now
            sessionCards = cards
                .filter { it.deckId == deck.id && it.dueDate <= newToday }
                .sortedBy { it.dueDate }

            // reset the session UI
            index = 0
            showBack = false
            againCount = 0
            hardCount = 0
            goodCount = 0
            easyCount = 0
        }) { Text("Restart Session") }

        Spacer(Modifier.height(8.dp))

        // go back to the deck screen
        Button(onClick = onBack) { Text("Back") }
    }
}