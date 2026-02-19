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
import java.time.LocalDate

fun isDue(dueDate: String): Boolean {
    val due = runCatching { LocalDate.parse(dueDate) }.getOrNull()
    return due == null || !due.isAfter(LocalDate.now())
}

@Composable
fun ReviewScreen(
    deck: CloudDeck,
    cards: List<Pair<String, CloudCard>>,
    onUpdateCard: (String, CloudCard) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {

    // The list of cards to review in the session.
    // It only includes cards that are due today (or earlier).
    var sessionCards by remember(deck.id, cards) {
        mutableStateOf(
            cards
                .filter { (_, c) -> c.deckId == deck.id && isDue(c.dueDate) }
                .sortedBy { (_, c) -> c.dueDate }
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
        val (docId, card) = sessionCards[index]

        Spacer(modifier.height(8.dp))

        // show card front side
        Text("Card ${index + 1} of ${sessionCards.size}")
        Spacer(modifier.height(4.dp))
        Text("Front: ${card.front}")

        Spacer(modifier.height(6.dp))

        // toggle showing the back side
        Button(onClick = { showBack = !showBack }) {
            Text(if (showBack) "Hide Back" else "Reveal Back")
        }

        // It only show if the user reveals it
        if (showBack) Text("Back: ${card.back}")

        Spacer(modifier.height(8.dp))

        // rating buttons
        Text("Rate:")

        fun rate(rating: Int) {
            val updated = applyRatingCloud(card, rating)
            onUpdateCard(docId, updated)

            sessionCards = sessionCards.map { (id, c) ->
                if (id == docId) id to updated else id to c
            }

            when (rating) {
                0 -> againCount++
                1 -> hardCount++
                2 -> goodCount++
                3 -> easyCount++
            }

            message = "Next due: ${updated.dueDate}"
            showBack = false
            index++
        }

        // Row 1: Again - Hard
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { rate(0) }, modifier = Modifier.weight(1f)) { Text("Again") }
            Button(onClick = { rate(1) }, modifier = Modifier.weight(1f)) { Text("Hard") }
        }

        // Row 2: Good - Easy
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { rate(2) }, modifier = Modifier.weight(1f)) { Text("Good") }
            Button(onClick = { rate(3) }, modifier = Modifier.weight(1f)) { Text("Easy") }
        }

        // show rating message (if any)
        if (message.isNotBlank()) Text(message)

        Spacer(modifier.height(8.dp))

        // restart button reloads due cards again and resets all counters
        Button(onClick = {
            sessionCards = cards
                .filter { (_, c) -> c.deckId == deck.id && isDue(c.dueDate) }
                .sortedBy { (_, c) -> c.dueDate }

            // reset the session UI
            index = 0
            showBack = false
            againCount = 0
            hardCount = 0
            goodCount = 0
            easyCount = 0
            message = ""
        }) { Text("Restart Session") }

        Spacer(Modifier.height(8.dp))

        // go back to the deck screen
        Button(onClick = onBack) { Text("Back") }
    }
}