package com.example.flashcardslitemobile

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.flashcardslitemobile.ui.theme.FlashcardsLiteMobileTheme
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

                    DisposableEffect(Unit) {
                        val listener = firebase.listenAuth { logged ->
                            currentUserEmail = firebase.currentEmail()
                            if (logged) {
                                navController.navigate("home") {
                                    popUpTo("cloudTest") { inclusive = true }
                                }
                            } else {
                                navController.navigate("cloudTest") {
                                    popUpTo("home") { inclusive = true }
                                }
                            }
                        }
                        onDispose { firebase.stopAuthListening(listener) }
                    }

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
                                }
                            },
                                onSignIn = { email, password ->
                                    firebase.signIn(email, password) { msg ->
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

                            LaunchedEffect(firebase.currentUid()) {
                                firebase.listenDecks(
                                    onResult = { cloudDecks = it },
                                    onError = { status = it}
                                )
                            }

                            HomeScreen(
                                decks = cloudDecks,

                                // Add a deck
                                onAddDeck = { name ->
                                    firebase.addDeck(name) { msg ->
                                        status = msg
                                    }
                                },

                                // Open deck screen using the deck ID
                                onOpenDeck = { deck ->
                                    navController.navigate("deck/${deck.id}")
                                },

                                onUpdateDeck = { id, newName ->
                                    firebase.updateDeck(id, newName) { status = it }
                                },

                                // Delete one deck and all cards of the deck
                                onDeleteDeck = { deckId ->
                                    firebase.deleteDeck(deckId) { msg ->
                                        status = msg
                                    }
                                },

                                onSignOut = {
                                    firebase.stopDecksListening()
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

                                DisposableEffect(deckId) {
                                    cloudCards = emptyList()
                                    val reg = firebase.listenCards(
                                        deckId = deckId,
                                        onResult = { cloudCards = it },
                                        onError = { status = it }
                                    )
                                    onDispose { reg?.remove() }
                                }

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
                                        }
                                    },

                                    // Update a card
                                    onUpdateCard = { cardId, updated ->
                                        cloudCards = cloudCards.map { (id, c) ->
                                            if (id == cardId) id to updated else id to c
                                        }
                                        firebase.updateCard(cardId, updated) { msg ->
                                            status = msg
                                        }
                                    },

                                    // Delete a card
                                    onDeleteCard = { cardId ->
                                        cloudCards = cloudCards.filterNot { it.first == cardId }
                                        firebase.deleteCard(cardId) { msg ->
                                            status = msg
                                        }
                                    },

                                    // Navigate to review screen
                                    onReview = {
                                        navController.navigate("review/${deckId}")
                                    },

                                    // Go back
                                    onBack = { navController.popBackStack() },

                                    statusMessage = status
                                )
                            }
                        }

                        // REVIEW SCREEN ROUTE
                        composable("review/{deckId}") { backStackEntry ->
                            val deckId = backStackEntry.arguments?.getString("deckId").orEmpty()
                            val deck = cloudDecks.firstOrNull { it.id == deckId }

                            if (deck == null) {
                                ErrorScreen(onBack = { navController.popBackStack() })
                                return@composable
                            }

                            DisposableEffect(deckId) {
                                cloudCards = emptyList()
                                val reg = firebase.listenCards(
                                    deckId = deckId,
                                    onResult = { cloudCards = it },
                                    onError = { status = it }
                                )
                                onDispose { reg?.remove() }
                            }

                            ReviewScreen(
                                deck = deck,
                                cards = cloudCards,
                                onUpdateCard = { docId, updated ->
                                    firebase.updateCard(docId, updated) { msg ->
                                        status = msg
                                    }
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}