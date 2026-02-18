package com.example.flashcardslitemobile

data class CloudDeck(val id: String, val name: String)
data class CloudCard(
    val id: String,
    val deckId: String,
    val front: String,
    val back: String,
    val intervalDays: Int,
    val dueDate: String,
    val lastReviewed: String?
)