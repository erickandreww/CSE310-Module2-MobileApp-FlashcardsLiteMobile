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
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
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

            // ViewModel owns all app state now (decks, cards, auth, etc.)
            val vm: AppViewModel = viewModel()

            // App theme (colors, typography, etc.)
            FlashcardsLiteMobileTheme {
                // NavController controls moving between screens
                val navController = rememberNavController()

                // Scaffold gives a basic layout structure and padding from system bars
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

                    // Decide which screen starts first
                    val start = if (vm.isLoggedIn) "home" else "cloudTest"

                    // Start listening to auth changes when the app is on screen.
                    // When the composable is destroyed, we stop the listener.
                    DisposableEffect(Unit) {
                        vm.startAuthListening()
                        onDispose { vm.stopAuthListening() }
                    }

                    // This runs whenever login state changes.
                    // Used to navigate and also start loading decks.
                    LaunchedEffect(vm.isLoggedIn) {
                        if (vm.isLoggedIn) {
                            // If logged in, go to home and clear login screen from back stack
                            navController.navigate("home") {
                                popUpTo("cloudTest") { inclusive = true }
                            }
                            // Start listening to decks once the user is logged in
                            vm.startDecksListening()
                        } else {
                            // If logged out, go to login screen and clear home from back stack
                            navController.navigate("cloudTest") {
                                popUpTo("home") { inclusive = true }
                            }
                        }
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
                                    vm.signUp(email, password)
                                },
                                onSignIn = { email, password ->
                                    vm.signIn(email, password)
                                },
                                onBack = { navController.popBackStack() },
                                currentUserEmail = vm.currentUserEmail,
                                statusMessage = vm.status
                            )
                        }

                        // HOME SCREEN ROUTE
                        composable("home") {
                            HomeScreen(
                                decks = vm.cloudDecks,

                                // Add a deck
                                onAddDeck = { name ->
                                    vm.addDeck(name)
                                },

                                // Open deck screen using the deck ID
                                onOpenDeck = { deck ->
                                    navController.navigate("deck/${deck.id}")
                                },

                                onUpdateDeck = { id, newName ->
                                    vm.updateDeck(id, newName)
                                },

                                // Delete one deck and all cards of the deck
                                onDeleteDeck = { deckId ->
                                    vm.deleteDeck(deckId)
                                },

                                onSignOut = {
                                    vm.signOut()
                                    navController.navigate("cloudTest") {
                                        popUpTo("home") { inclusive = true }
                                    }
                                },

                                currentUserEmail = vm.currentUserEmail,
                                statusMessage = vm.status
                            )
                        }

                        // DECK SCREEN ROUTE
                        composable("deck/{deckId}") { backStackEntry ->

                            // Read deckId of the route
                            val deckId = backStackEntry.arguments?.getString("deckId").orEmpty()

                            // Find the deck
                            val deck = vm.cloudDecks.firstOrNull { it.id == deckId }

                            // If the deck doesn't exist, show an error screen
                            if (deck == null) {
                                ErrorScreen(onBack = { navController.popBackStack() })
                            } else {
                                DisposableEffect(deckId) {
                                    vm.startCardsListening(deckId)
                                    onDispose { vm.stopCardsListening() }
                                }

                                DeckScreen(
                                    deck = deck,
                                    cards = vm.cloudCards,

                                    // Add card to this deck
                                    onAddCard = { dId, front, back ->
                                        vm.addCard(dId, front, back)
                                    },

                                    // Update a card
                                    onUpdateCard = { cardId, updated ->
                                        vm.updateCard(cardId, updated)
                                    },

                                    // Delete a card
                                    onDeleteCard = { cardId ->
                                        vm.deleteCard(cardId)
                                    },

                                    // Navigate to review screen
                                    onReview = {
                                        navController.navigate("review/$deckId")
                                    },

                                    // Go back
                                    onBack = { navController.popBackStack() },

                                    statusMessage = vm.status
                                )
                            }
                        }

                        // REVIEW SCREEN ROUTE
                        composable("review/{deckId}") { backStackEntry ->
                            val deckId = backStackEntry.arguments?.getString("deckId").orEmpty()
                            val deck = vm.cloudDecks.firstOrNull { it.id == deckId }

                            if (deck == null) {
                                ErrorScreen(onBack = { navController.popBackStack() })
                                return@composable
                            }

                            ReviewScreen(
                                deck = deck,
                                cards = vm.cloudCards,
                                onUpdateCard = { docId, updated ->
                                    vm.updateCard(docId, updated)
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