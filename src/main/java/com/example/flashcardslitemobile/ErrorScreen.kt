package com.example.flashcardslitemobile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ErrorScreen(onBack: () -> Unit) {
    // Simple screen for when something goes wrong

    // Column formating
    Column(modifier = Modifier.padding(16.dp)) {
        // Message to the user
        Text("Deck not found")
        Spacer(modifier = Modifier.height(12.dp))

        // Button to go back
        Button(onClick = onBack) { Text("back") }
    }
}