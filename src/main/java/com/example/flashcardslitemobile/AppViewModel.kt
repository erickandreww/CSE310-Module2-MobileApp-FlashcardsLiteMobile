package com.example.flashcardslitemobile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration

class AppViewModel : ViewModel() {
    private val firebase = FirebaseService()

    private var authListener: FirebaseAuth.AuthStateListener? = null

    var cloudDecks by mutableStateOf(emptyList<CloudDeck>())
        private set
    var cloudCards by mutableStateOf(emptyList<Pair<String, CloudCard>>())
        private set

    var status by mutableStateOf("")
        private set
    var errorMessage by mutableStateOf("")
        private set

    var isLoadingDecks by mutableStateOf(false)
        private set
    var isLoadingCards by mutableStateOf(false)
        private set

    var currentUserEmail by mutableStateOf(firebase.currentEmail())
        private set
    var isLoggedIn by mutableStateOf(firebase.currentUid() != null)
        private set

    private var decksReg: ListenerRegistration? = null
    private var cardsReg: ListenerRegistration? = null

    fun startAuthListening() {
        if (authListener != null) return

        authListener = firebase.listenAuth { logged ->
            isLoggedIn = logged
            currentUserEmail = firebase.currentEmail()

            if (!logged) {
                stopAllListening()
                cloudDecks = emptyList()
                cloudCards = emptyList()
            }
        }
    }

    fun stopAuthListening() {
        authListener?.let { firebase.stopAuthListening(it) }
        authListener = null
    }

    fun startDecksListening() {
        decksReg?.remove()
        isLoadingDecks = true
        errorMessage = ""

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
        cardsReg?.remove()
        cloudCards = emptyList()
        isLoadingCards = true
        errorMessage = ""

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
        cardsReg?.remove()
        cardsReg = null
    }

    fun stopAllListening() {
        decksReg?.remove(); decksReg = null
        cardsReg?.remove(); cardsReg = null
    }


    //
    fun signUp(email: String, password: String) {
        firebase.signUp(email, password) { status = it }
    }

    fun signIn(email: String, password: String) {
        firebase.signIn(email, password) { status = it }
    }

    fun signOut() {
        // Stop firestore listeners immediately
        stopAllListening()

        firebase.signOut { status = it }

        // Clear local state (auth listener will also run, but this makes UI instant)
        cloudDecks = emptyList()
        cloudCards = emptyList()
        errorMessage = ""
        isLoadingDecks = false
        isLoadingCards = false
    }


    // keep status ONLY for user actions
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
        stopAllListening()
        super.onCleared()
    }
}
