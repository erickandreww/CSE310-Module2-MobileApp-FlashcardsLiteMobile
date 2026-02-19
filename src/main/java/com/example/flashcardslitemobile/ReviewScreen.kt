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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalDate

// helper: checks if a card is due (today or earlier)
// if parsing fails, I treat it as due so it still shows up
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
    // Important: this is a "snapshot" for the session, so it doesn't keep resetting mid-review.
    var sessionCards by remember(deck.id) {
        mutableStateOf<List<Pair<String, CloudCard>>>(emptyList())
    }

    // I use this flag so I only build the session once per deck
    var sessionStarted by remember(deck.id) { mutableStateOf(false) }

    // When we enter review (or cards update), build the session list one time.
    // If session already started, we don't overwrite it.
    LaunchedEffect(deck.id, cards) {
        if (!sessionStarted && cards.isNotEmpty()) {
            sessionCards = cards
                .filter { (_, c) -> c.deckId == deck.id && isDue(c.dueDate) }
                .sortedBy { (_, c) -> c.dueDate }
            sessionStarted = true
        }
    }

    // index = which card we're currently on
    // NOTE: because we remove cards from the list, we can always use index 0
    val index = 0

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

        // progress and counts (simple version since we always show card 1)
        Text("Progress: ${if (sessionCards.isEmpty()) 0 else 1} / ${sessionCards.size}")
        Text("Again: $againCount Hard: $hardCount  Good: $goodCount  Easy: $easyCount")

        // If there are no due cards, show message and stop
        if (sessionStarted && sessionCards.isEmpty()) {
            Text("No due cards today!")
            Button(onClick = onBack) { Text("Back") }
            return
        }

        // If session not ready yet, show a basic loading message
        if (!sessionStarted) {
            Text("Loading...")
            Button(onClick = onBack) { Text("Back") }
            return
        }

        // current card that's being reviewed (always the first one)
        val (docId, card) = sessionCards[index]

        Spacer(modifier.height(8.dp))

        // show card front side
        Text("Card 1 of ${sessionCards.size}")
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

        // local function to handle rating click
        fun rate(rating: Int) {
            // calculate next due date + interval
            val updated = applyRatingCloud(card, rating)

            // send update to firestore (through vm callback)
            onUpdateCard(docId, updated)

            // count the rating for this session summary
            when (rating) {
                0 -> againCount++
                1 -> hardCount++
                2 -> goodCount++
                3 -> easyCount++
            }

            // show feedback so user knows it saved
            message = "Next due: ${updated.dueDate}"
            showBack = false

            // remove the reviewed card from the session list so we move forward
            sessionCards = sessionCards.filterNot { it.first == docId }
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
            // restart uses latest cards snapshot (so it can include changes)
            sessionCards = cards
                .filter { (_, c) -> c.deckId == deck.id && isDue(c.dueDate) }
                .sortedBy { (_, c) -> c.dueDate }

            // reset session UI stuff
            showBack = false
            againCount = 0
            hardCount = 0
            goodCount = 0
            easyCount = 0
            message = ""

            // keep sessionStarted true so it doesn't rebuild automatically
            sessionStarted = true
        }) { Text("Restart Session") }

        Spacer(Modifier.height(8.dp))

        // go back to the deck screen
        Button(onClick = onBack) { Text("Back") }
    }
}