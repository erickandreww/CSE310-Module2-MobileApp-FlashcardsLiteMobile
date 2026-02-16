package com.example.flashcardslitemobile

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.flashcardslitemobile.ui.theme.FlashcardsLiteMobileTheme
import kotlinx.coroutines.launch
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {

    // This is the first thing Android runs when the app opens
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Just a simple log so I can see in Logcat that the app started
        Log.d("MyTag", "App started")

        val app = FirebaseApp.initializeApp(this)
        Log.d("MyTag", "FirebaseApp init = ${app != null}")

        enableEdgeToEdge()
        setContent {
            // These are my main "app state" variables:
            // Using remember + mutableState so Compose updates the UI when values change
            var decks by remember { mutableStateOf( listOf<Deck>())}
            var cards by remember { mutableStateOf( listOf<Card>())}

            // These counters help me generate IDs when I create new decks/cards
            var nextDeckId by remember { mutableIntStateOf(1) }
            var nextCardId by remember { mutableIntStateOf(1) }

            val firebase = remember { FirebaseService() }
            var status by remember { mutableStateOf("") }
            var currentUserEmail by remember { mutableStateOf(firebase.currentEmail())}
            var notes by remember { mutableStateOf(listOf<Pair<String, String>>()) }

            // Context needed for DataStore (local storage)
            val context = LocalContext.current
            // DataStoreManager handles saving/loading my decks/cards from the phone storage
            val store = remember { DataStoreManager(context) }
            // Coroutine scope so I can call save/load from button clicks
            val scope = rememberCoroutineScope()

            // This runs one time when the UI starts, it load saved data
            LaunchedEffect(Unit) {
                // store.load() returns decks and cards
                val (loadedDecks, loadedCards) = store.load()
                // Put loaded data into variables
                decks = loadedDecks
                cards = loadedCards

                // Update the next IDs so I don't reuse IDs after restarting the app
                nextDeckId = (decks.maxOfOrNull { it.id } ?: 0) + 1
                nextCardId = (cards.maxOfOrNull { it.id } ?: 0) + 1
            }

            // App theme (colors, typography, etc.)
            FlashcardsLiteMobileTheme {
                // NavController controls moving between screens
                val navController = rememberNavController()

                // Scaffold gives a basic layout structure and padding from system bars
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

                    // Where we define all the screens routes in the app
                    NavHost(
                        navController = navController,
                        startDestination = "home",
                        modifier = Modifier.padding(innerPadding)
                    ) {

                        // HOME SCREEN ROUTE
                        composable("home") {
                            HomeScreen(
                                decks = decks,

                                // Add a deck
                                onAddDeck = { name ->
                                    val newDeck = Deck(id = nextDeckId, name = name)
                                    nextDeckId++
                                    decks = decks + newDeck

                                    scope.launch { store.save(decks, cards) }
                                },

                                // Clear decks and cards
                                onClearDecks = {
                                    decks = emptyList()
                                    cards = emptyList()
                                    nextDeckId = 1
                                    nextCardId = 1

                                    scope.launch {
                                        store.clear()
                                        store.save(decks, cards)
                                    }
                                },

                                // Open deck screen using the deck ID
                                onOpenDeck = { deck ->
                                    navController.navigate("deck/${deck.id}")
                                },

                                onCloudTest = {
                                    navController.navigate("cloudTest")
                                },

                                onfirestoreTest = {
                                    navController.navigate("firestoreTest")
                                },

                                // Delete one deck and all cards of the deck
                                onDeleteDeck = { deckId ->
                                    decks = decks.filterNot { it.id == deckId }
                                    cards = cards.filterNot { it.deckId == deckId}
                                    scope.launch { store.save(decks, cards) }
                                }
                            )
                        }

                        // DECK SCREEN ROUTE
                        composable("deck/{deckId}") { backStackEntry ->

                            // Read deckId of the route
                            val deckId = backStackEntry
                                .arguments?.getString("deckId")?.toIntOrNull()

                            // Find the deck
                            val deck = decks.firstOrNull { it.id == deckId }

                            // If the deck doesn't exist, show an error screen
                            if (deck == null) {
                                ErrorScreen(onBack = { navController.popBackStack() })
                            } else {
                                DeckScreen(
                                    deck = deck,
                                    cards = cards,

                                    // Add card to this deck
                                    onAddCard = { dId, front, back ->
                                        val newCard = Card(
                                            id = nextCardId,
                                            deckId = dId,
                                            front = front,
                                            back = back
                                        )
                                        nextCardId++
                                        cards = cards + newCard

                                        scope.launch { store.save(decks, cards) }
                                    },

                                    // Navigate to review screen
                                    onReview = {
                                        navController.navigate("review/$deckId")
                                    },

                                    // Update a card
                                    onUpdateCard = { updated ->
                                        cards = cards.map {
                                            if (it.id == updated.id) updated else it
                                        }
                                        scope.launch { store.save(decks, cards) }
                                    },

                                    // Delete a card
                                    onDeleteCard = { cardId ->
                                        cards = cards.filterNot { it.id == cardId}
                                        scope.launch { store.save(decks, cards) }
                                    },

                                    // Go back
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }

                        // REVIEW SCREEN ROUTE
                        composable("review/{deckId}") { backStackEntry ->
                            // Read deckId from the route
                            val deckId = backStackEntry
                                .arguments?.getString("deckId")?.toIntOrNull()

                            // Find the deck
                            val deck = decks.firstOrNull { it.id == deckId }

                            // If missing, show error screen
                            if (deck == null) {
                                ErrorScreen(onBack = { navController.popBackStack() } )
                            } else {
                                ReviewScreen(
                                    deck = deck,
                                    cards = cards,

                                    // Update card after rating
                                    onUpdateCard = { updated ->
                                        cards = cards.map {
                                            if (it.id == updated.id) updated else it
                                        }
                                        scope.launch { store.save(decks, cards) }
                                    },

                                    // Go back
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }

                        composable("cloudTest") {
                            CloudTestScreen(
                                onSignUp = { email, password ->
                                    firebase.signUp(email, password) { msg ->
                                        status = msg
                                        currentUserEmail = firebase.currentEmail()
                                    }

                                },
                                onSignIn = { email, password ->
                                    firebase.signIn(email, password) { msg ->
                                        status = msg
                                        currentUserEmail = firebase.currentEmail()
                                    }
                                },
                                onSignOut = {
                                    firebase.signOut { msg ->
                                        status = msg
                                        currentUserEmail = firebase.currentEmail()
                                    }
                                },
                                onBack = { navController.popBackStack() },
                                currentUserEmail = currentUserEmail,
                                statusMessage = status
                            )
                        }

                        composable("firestoreTest") {
                            fun refreshNotes() {
                                firebase.loadMyTestNotes(
                                    onResult = { list ->
                                        notes = list
                                        status = "Loaded ${list.size} notes"
                                    },
                                    onError = { err ->
                                        status = err
                                    }
                                )
                            }

                            LaunchedEffect(Unit) {
                                refreshNotes()
                            }

                            FirestoreTestScreen(
                                testNotes = notes,
                                onAddNote = { note ->
                                    firebase.addTestNote(note) { msg ->
                                        status = msg
                                        refreshNotes()
                                    }
                                },
                                onUpdateNote = { noteId, newText ->
                                    firebase.updateTestNotes(
                                        noteId, newText)
                                    { msg ->
                                        status = msg
                                        refreshNotes()
                                    }
                                },
                                onDeleteNote = {noteId ->
                                    firebase.deleteTestNote(noteId) { msg ->
                                        status = msg
                                        refreshNotes()
                                    }
                                },
                                onRefresh = { refreshNotes() },
                                onBack = { navController.popBackStack() },
                                currentUserEmail = currentUserEmail,
                                statusMessage = status
                            )
                        }
                    }
                }
            }
        }
    }
}