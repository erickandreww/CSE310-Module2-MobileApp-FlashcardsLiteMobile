package com.example.flashcardslitemobile

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

// This creates a DataStore instance attached to the Context.
private val Context.dataStore by preferencesDataStore(name = "flashcards_store")

// "keys" used to store JSON strings in DataStore.
private val DECKS_KEY = stringPreferencesKey("decks_json")
private val CARDS_KEY = stringPreferencesKey("cards_json")

// to save/load clean objects using JSON.
@Serializable
data class DeckDto(
    val id: Int,
    val name: String
)

@Serializable
data class CardDto(
    val id: Int,
    val deckId: Int,
    val front: String,
    val back: String,
    val intervalDays: Int = 1,
    val dueDate: String = todayString(),
    val lastReviewed: String? = null
)

class DataStoreManager(private val context: Context) {

    // Json serializer/deserializer.
    private val json = Json { ignoreUnknownKeys = true }

    // Save decks and cards into DataStore.
    suspend fun save(decks: List<Deck>, cards: List<Card>) {

        // Convert decks list into a JSON string
        val decksJson = json.encodeToString(decks.map { DeckDto(it.id, it.name) })

        // Convert cards list into a JSON string
        val cardsJson = json.encodeToString(
            cards.map { CardDto(it.id, it.deckId, it.front,
                it.back, it.intervalDays, it.dueDate, it.lastReviewed)
            }
        )

        // Write the JSON strings into DataStore
        context.dataStore.edit { prefs ->
            prefs[DECKS_KEY] = decksJson
            prefs[CARDS_KEY] = cardsJson
        }
    }

    // Load decks and cards from DataStore.
    suspend fun load(): Pair<List<Deck>, List<Card>> {
        // Reads the saved preferences from DataStore
        val prefs = context.dataStore.data.first()

        // Read the JSON strings
        val decksJson = prefs[DECKS_KEY].orEmpty()
        val cardsJson = prefs[CARDS_KEY].orEmpty()

        // If the JSON string is empty, return empty list
        val decks = if (decksJson.isBlank()) emptyList()
        // decode JSON into DTOs and convert to real models.
        else json.decodeFromString<List<DeckDto>>(decksJson).map { Deck(it.id, it.name)}

        val cards = if (cardsJson.isBlank()) emptyList()
        else json.decodeFromString<List<CardDto>>(cardsJson).map {
            Card(it.id, it.deckId, it.front, it.back, it.intervalDays, it.dueDate, it.lastReviewed)
        }

        return decks to cards
    }

    // Clears the saved JSON from DataStore - Resets storage
    suspend fun clear() {
        context.dataStore.edit { prefs ->
            prefs.remove(DECKS_KEY)
            prefs.remove(CARDS_KEY)
        }
    }
}