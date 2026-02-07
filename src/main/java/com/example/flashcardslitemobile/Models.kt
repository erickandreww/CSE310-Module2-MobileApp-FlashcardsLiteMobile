package com.example.flashcardslitemobile

// Deck data model
data class Deck(
    val id: Int,
    val name: String
)

// Card data model
data class Card(
    val id: Int,
    val deckId: Int,
    val front: String,
    val back: String,
    val intervalDays: Int = 1,
    val dueDate: String = todayString(),
    val lastReviewed: String? = null)