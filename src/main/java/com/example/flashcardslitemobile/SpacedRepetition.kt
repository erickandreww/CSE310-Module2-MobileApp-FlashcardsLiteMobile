package com.example.flashcardslitemobile

import java.time.LocalDate

// Gets today's date as a String, this format it's easy to compare and to save in DataStore
fun todayString(): String = LocalDate.now().toString()

// This function updates a card based on the rating the user picked during review
fun applyRatingCloud(card: CloudCard, rating: Int): CloudCard {
    // Today's date, used to calculate the new due date
    val today = LocalDate.now()

    // If intervalDays is 0 or negative, this force it to be at least 1
    val current = if (card.intervalDays < 1) 1 else card.intervalDays

    // Decide a new interval based on the rating
    val newInterval = when (rating) {
        0 -> 1 // to review again tomorrow
        1 -> maxOf(1, (current * 1.2).toInt()) // Hard = small increase
        2 -> maxOf(1, (current * 2.0).toInt()) // Good = bigger increase
        3 -> maxOf(2, (current * 2.5).toInt()) // Easy = biggest increase
        else -> maxOf(1, (current * 2.0).toInt()) // Fallback if something unexpected happens
    }

    // Calculate the new due date by adding the interval days to today
    val newDue = today.plusDays(newInterval.toLong()).toString()

    // Return a copy of the card with updated fields
    return card.copy(
        intervalDays = newInterval,
        lastReviewed = today.toString(),
        dueDate = newDue
    )
}