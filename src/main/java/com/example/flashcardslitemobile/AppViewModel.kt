package com.example.flashcardslitemobile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration

class AppViewModel : ViewModel() {

    // This class is kind of the "brain" of the app now.
    // It holds the state, and it calls FirebaseService for database/auth stuff.
    private val firebase = FirebaseService()

    // Used to listen to auth changes (login / logout)
    private var authListener: FirebaseAuth.AuthStateListener? = null

    // Decks list that HomeScreen uses
    var cloudDecks by mutableStateOf(emptyList<CloudDeck>())
        private set

    // Cards list that DeckScreen/ReviewScreen uses
    var cloudCards by mutableStateOf(emptyList<Pair<String, CloudCard>>())
        private set

    // status is for messages after user actions (create, delete, etc.)
    var status by mutableStateOf("")
        private set

    // error message is for listener failures (like Firestore errors)
    var errorMessage by mutableStateOf("")
        private set

    // basic loading flags (useful if you want a spinner later)
    var isLoadingDecks by mutableStateOf(false)
        private set
    var isLoadingCards by mutableStateOf(false)
        private set

    // simple info about the current user
    var currentUserEmail by mutableStateOf(firebase.currentEmail())
        private set
    var isLoggedIn by mutableStateOf(firebase.currentUid() != null)
        private set

    // Firestore listener registrations (so we can remove them later)
    private var decksReg: ListenerRegistration? = null
    private var cardsReg: ListenerRegistration? = null

    fun startAuthListening() {
        // don't add the listener twice
        if (authListener != null) return

        // Listen to Firebase auth changes (login / logout)
        authListener = firebase.listenAuth { logged ->
            isLoggedIn = logged
            currentUserEmail = firebase.currentEmail()

            // if user logged out, clear local state and stop listeners
            if (!logged) {
                stopAllListening()
                cloudDecks = emptyList()
                cloudCards = emptyList()
            }
        }
    }

    fun stopAuthListening() {
        // Remove the auth listener safely
        authListener?.let { firebase.stopAuthListening(it) }
        authListener = null
    }

    fun startDecksListening() {
        // Remove old listener if it exists (prevents duplicates)
        decksReg?.remove()

        isLoadingDecks = true
        errorMessage = ""

        // Start a new listener and save the registration so we can stop it later
        decksReg = firebase.listenDecks(
            onResult = {
                cloudDecks = it
                isLoadingDecks = false
            },
            onError = {
                errorMessage = it
                isLoadingDecks = false
            }
        )
    }

    fun startCardsListening(deckId: String) {
        // Stop previous cards listener (like when switching decks)
        cardsReg?.remove()

        // Clear the list so the UI doesn't show old deck cards
        cloudCards = emptyList()

        isLoadingCards = true
        errorMessage = ""

        // Start listening to cards for this deck
        cardsReg = firebase.listenCards(
            deckId = deckId,
            onResult = {
                cloudCards = it
                isLoadingCards = false
            },
            onError = {
                errorMessage = it
                isLoadingCards = false
            }
        )
    }

    fun stopCardsListening() {
        // Stop only the cards listener (used when leaving deck screen)
        cardsReg?.remove()
        cardsReg = null
    }

    fun stopAllListening() {
        // Stop both decks + cards listeners
        decksReg?.remove(); decksReg = null
        cardsReg?.remove(); cardsReg = null
    }

    // These are auth actions called from the login screen
    fun signUp(email: String, password: String) {
        firebase.signUp(email, password) { status = it }
    }

    fun signIn(email: String, password: String) {
        firebase.signIn(email, password) { status = it }
    }

    fun signOut() {
        // Stop firestore listeners immediately (so we don't keep syncing after logout)
        stopAllListening()

        // Firebase sign out
        firebase.signOut { status = it }

        // Clear local state (auth listener will also run, but this makes UI instant)
        cloudDecks = emptyList()
        cloudCards = emptyList()
        errorMessage = ""
        isLoadingDecks = false
        isLoadingCards = false
    }

    // keep status ONLY for user actions (not for syncing messages)
    fun addDeck(name: String) = firebase.addDeck(name) { status = it }
    fun updateDeck(id: String, name: String) = firebase.updateDeck(id, name) { status = it }
    fun deleteDeck(id: String) = firebase.deleteDeck(id) { status = it }

    fun addCard(deckId: String, front: String, back: String) =
        firebase.addCard(deckId, front, back, 1, todayString(), null) { status = it }

    fun updateCard(id: String, updated: CloudCard) =
        firebase.updateCard(id, updated) { status = it }

    fun deleteCard(id: String) =
        firebase.deleteCard(id) { status = it }

    override fun onCleared() {
        // If Android destroys the VM, stop listeners so nothing leaks
        stopAllListening()
        super.onCleared()
    }
}