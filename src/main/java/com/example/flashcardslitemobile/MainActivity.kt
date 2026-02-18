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
            var cloudDecks by remember { mutableStateOf(emptyList<CloudDeck>()) }
            var cloudCards by remember { mutableStateOf(emptyList<Pair<String, CloudCard>>()) }

            val firebase = remember { FirebaseService() }
            var status by remember { mutableStateOf("") }
            var currentUserEmail by remember { mutableStateOf(firebase.currentEmail())}

            // App theme (colors, typography, etc.)
            FlashcardsLiteMobileTheme {
                // NavController controls moving between screens
                val navController = rememberNavController()

                // Scaffold gives a basic layout structure and padding from system bars
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

                    val start = if (firebase.currentUid() != null) "home" else "cloudTest"

                    // Where we define all the screens routes in the app
                    NavHost(
                        navController = navController,
                        startDestination = start,
                        modifier = Modifier.padding(innerPadding)
                    ) {

                        composable("cloudTest") {
                            CloudTestScreen(
                                onSignUp = { email, password ->
                                    firebase.signUp(email, password) { msg ->
                                        status = msg
                                        currentUserEmail = firebase.currentEmail()
                                        navController.navigate("home") {
                                            popUpTo("cloudTest") { inclusive = true }
                                        }
                                    }
                                },
                                onSignIn = { email, password ->
                                    firebase.signIn(email, password) { msg ->
                                        status = msg
                                        currentUserEmail = firebase.currentEmail()
                                        navController.navigate("home") {
                                            popUpTo("cloudTest") { inclusive = true }
                                        }
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

                        // HOME SCREEN ROUTE
                        composable("home") {

                            fun refreshDecks() {
                                firebase.loadDecks(
                                    onResult = { list ->
                                        cloudDecks = list
                                        status = "Loaded ${list.size} decks"
                                    },
                                    onError = { err -> status = err }
                                )
                            }

                            LaunchedEffect(Unit) { refreshDecks() }

                            HomeScreen(
                                decks = cloudDecks,

                                // Add a deck
                                onAddDeck = { name ->
                                    firebase.addDeck(name) { msg ->
                                        status = msg
                                        refreshDecks()
                                    }
                                },

                                // Open deck screen using the deck ID
                                onOpenDeck = { deck ->
                                    navController.navigate("deck/${deck.id}")
                                },

                                // Delete one deck and all cards of the deck
                                onDeleteDeck = { deckId ->
                                    firebase.deleteDeck(deckId) { msg ->
                                        status = msg
                                        refreshDecks()
                                    }
                                },

                                onRefresh = { refreshDecks() },

                                onSignOut = {
                                    firebase.signOut { msg ->
                                        status = msg
                                        currentUserEmail = null
                                        cloudDecks = emptyList()
                                        cloudCards = emptyList()
                                        navController.navigate("cloudTest") {
                                            popUpTo("home") { inclusive = true }
                                        }
                                    }
                                },

                                currentUserEmail = currentUserEmail,
                                statusMessage = status
                            )
                        }

                        // DECK SCREEN ROUTE
                        composable("deck/{deckId}") { backStackEntry ->

                            // Read deckId of the route
                            val deckId = backStackEntry.arguments?.getString("deckId").orEmpty()

                            // Find the deck
                            val deck = cloudDecks.firstOrNull { it.id == deckId }

                            // If the deck doesn't exist, show an error screen
                            if (deck == null) {
                                ErrorScreen(onBack = { navController.popBackStack() })
                            } else {

                                fun refreshCards() {
                                    firebase.loadCards(
                                        deckId = deck.id,
                                        onResult = { list ->
                                            cloudCards = list.map { it.id to it}
                                            status = "Loaded ${list.size} cards"
                                        },
                                        onError = { err -> status = err }
                                    )
                                }

                                LaunchedEffect(deck.id) { refreshCards() }

                                DeckScreen(
                                    deck = deck,
                                    cards = cloudCards,

                                    // Add card to this deck
                                    onAddCard = { dId, front, back ->
                                        firebase.addCard(
                                            deckId = dId,
                                            front = front,
                                            back = back,
                                            intervalDays = 1,
                                            dueDate = todayString(),
                                            lastReviewed = null
                                        ) { msg ->
                                            status = msg
                                            refreshCards()
                                        }
                                    },

                                    // Update a card
                                    onUpdateCard = { cardId, updated ->
                                        firebase.updateCard(cardId, updated) { msg ->
                                            status = msg
                                            refreshCards()
                                        }
                                    },

                                    // Delete a card
                                    onDeleteCard = { cardId ->
                                        firebase.deleteCard(cardId) { msg ->
                                            status = msg
                                            refreshCards()
                                        }
                                    },

                                    // Navigate to review screen
                                    onReview = {
                                        status = "Review (Cloud) coming next"
                                    },

                                    // refresh all cards
                                    onRefreshCards = { refreshCards() },

                                    // Go back
                                    onBack = { navController.popBackStack() },

                                    statusMessage = status
                                )
                            }
                        }

                        // REVIEW SCREEN ROUTE
//                        composable("review/{deckId}") { backStackEntry ->
//                            // Read deckId from the route
//                            val deckId = backStackEntry
//                                .arguments?.getString("deckId")?.toIntOrNull()
//
//                            // Find the deck
//                            val deck = decks.firstOrNull { it.id == deckId }
//
//                            // If missing, show error screen
//                            if (deck == null) {
//                                ErrorScreen(onBack = { navController.popBackStack() } )
//                            } else {
//                                ReviewScreen(
//                                    deck = deck,
//                                    cards = cards,
//
//                                    // Update card after rating
//                                    onUpdateCard = { updated ->
//                                        cards = cards.map {
//                                            if (it.id == updated.id) updated else it
//                                        }
//                                        scope.launch { store.save(decks, cards) }
//                                    },
//
//                                    // Go back
//                                    onBack = { navController.popBackStack() }
//                                )
//                            }
//                        }
                    }
                }
            }
        }
    }
}