package com.example.flashcardslitemobile

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ListenerRegistration

class FirebaseService {

    // Firebase Auth = login system
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // Firestore = database
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    // Helpers to get current logged user info
    fun currentEmail(): String? = auth.currentUser?.email
    fun currentUid(): String? = auth.currentUser?.uid

    // This just checks if user is logged in before doing DB stuff
    private fun requiredUid(onError: (String) -> Unit): String? {
        val uid = currentUid()
        if (uid == null) onError("You need to be logged in")
        return uid
    }

    // User structure in Firestore:
    // users/{uid}/decks
    // users/{uid}/cards
    private fun userDoc(uid: String) = db.collection("users").document(uid)
    private fun decksCol(uid: String) = userDoc(uid).collection("decks")
    private fun cardsCol(uid: String) = userDoc(uid).collection("cards")

    // -----------------------
    // AUTH
    // -----------------------

    fun signUp(email: String, password: String, onResult: (String) -> Unit) {
        // Create a new account in Firebase Auth
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { onResult("Signed up!") }
            .addOnFailureListener { e -> onResult("Sign Up Failed: ${e.message}") }
    }

    fun signIn(email: String, password: String, onResult: (String) -> Unit) {
        // Login with email + password
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { onResult("Signed in!") }
            .addOnFailureListener { e -> onResult("Sign In failed: ${e.message}") }
    }

    fun signOut(onResult: (String) -> Unit) {
        // Logout
        auth.signOut()
        onResult("Signed Out!")
    }

    fun listenAuth(onChange: (Boolean) -> Unit): FirebaseAuth.AuthStateListener {
        // This listener runs every time auth changes (login/logout)
        val listener = FirebaseAuth.AuthStateListener { a ->
            onChange(a.currentUser != null)
        }
        auth.addAuthStateListener(listener)
        return listener
    }

    fun stopAuthListening(listener: FirebaseAuth.AuthStateListener) {
        // Remove the auth listener
        auth.removeAuthStateListener(listener)
    }

    // -----------------------
    // DECKS
    // -----------------------

    fun addDeck(name: String, onResult: (String) -> Unit) {
        val uid = requiredUid(onResult) ?: return

        // Basic deck fields
        val data = hashMapOf(
            "name" to name,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        // Add deck to Firestore
        decksCol(uid)
            .add(data)
            .addOnSuccessListener { onResult("Deck added!") }
            .addOnFailureListener { e -> onResult("Add failed: ${e.message}") }
    }

    fun updateDeck(deckId: String, newName: String, onResult: (String) -> Unit) {
        val uid = requiredUid(onResult) ?: return

        // Update deck name + timestamp
        val data = mapOf(
            "name" to newName,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        decksCol(uid)
            .document(deckId)
            .update(data)
            .addOnSuccessListener { onResult("Deck updated!") }
            .addOnFailureListener { e -> onResult("Update failed: ${e.message}") }
    }

    fun deleteDeck(deckId: String, onResult: (String) -> Unit) {
        val uid = requiredUid(onResult) ?: return

        // First we load cards from this deck...
        cardsCol(uid)
            .whereEqualTo("deckId", deckId)
            .get()
            .addOnSuccessListener { snap ->

                // ...then we delete them in a batch
                val batch = db.batch()
                snap.documents.forEach { doc -> batch.delete(doc.reference) }

                batch.commit()
                    .addOnSuccessListener {

                        // ...then finally delete the deck document
                        decksCol(uid).document(deckId).delete()
                            .addOnSuccessListener { onResult("Deck and cards Deleted!") }
                            .addOnFailureListener { e -> onResult("Deck Delete failed: ${e.message}") }
                    }
                    .addOnFailureListener { e -> onResult("Card delete failed: ${e.message}") }
            }
            .addOnFailureListener { e -> onResult("Load cards failed: ${e.message}") }
    }

    fun listenDecks(
        onResult: (List<CloudDeck>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration {

        // Need UID to listen to this user's decks
        val uid = requiredUid(onError)
            ?: return db.collection("_noop").addSnapshotListener { _, _ -> }
        // ^ small trick so we can still return a ListenerRegistration

        // Real-time listener: anytime Firestore changes, this runs again
        return decksCol(uid)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, e ->
                if (e != null) { onError("Listen failed: ${e.message}"); return@addSnapshotListener }
                if (snap == null) { onResult(emptyList()); return@addSnapshotListener }

                val items = snap.documents.map { doc ->
                    CloudDeck(id = doc.id, name = doc.getString("name").orEmpty())
                }
                onResult(items)
            }
    }

    // -----------------------
    // CARDS
    // -----------------------

    fun addCard(
        deckId: String,
        front: String,
        back: String,
        intervalDays: Int,
        dueDate: String,
        lastReviewed: String?,
        onResult: (String) -> Unit
    ) {
        val uid = requiredUid(onResult) ?: return

        // Card fields (deckId is how we know which deck it belongs to)
        val data = hashMapOf(
            "deckId" to deckId,
            "front" to front,
            "back" to back,
            "intervalDays" to intervalDays,
            "dueDate" to dueDate,
            "lastReviewed" to lastReviewed,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        cardsCol(uid)
            .add(data)
            .addOnSuccessListener { onResult("Card added!") }
            .addOnFailureListener { e -> onResult("Add failed: ${e.message}") }
    }

    fun updateCard(docId: String, updated: CloudCard, onResult: (String) -> Unit) {
        val uid = requiredUid(onResult) ?: return

        // Update the main fields + updatedAt
        val data = mapOf(
            "front" to updated.front,
            "back" to updated.back,
            "intervalDays" to updated.intervalDays,
            "dueDate" to updated.dueDate,
            "lastReviewed" to updated.lastReviewed,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        cardsCol(uid)
            .document(docId)
            .update(data)
            .addOnSuccessListener { onResult("Card updated!") }
            .addOnFailureListener { e -> onResult("Update Failed: ${e.message}") }
    }

    fun deleteCard(docId: String, onResult: (String) -> Unit) {
        val uid = requiredUid(onResult) ?: return

        cardsCol(uid)
            .document(docId)
            .delete()
            .addOnSuccessListener { onResult("Card Deleted!") }
            .addOnFailureListener { e -> onResult("Delete failed: ${e.message}") }
    }

    fun listenCards(
        deckId: String,
        onResult: (List<Pair<String, CloudCard>>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration {

        // Need UID to listen to this user's cards
        val uid = requiredUid(onError)
            ?: return db.collection("_noop").addSnapshotListener { _, _ -> }

        // Listen to cards from ONE deck only
        return cardsCol(uid)
            .whereEqualTo("deckId", deckId)
            .addSnapshotListener { snap, e ->
                if (e != null) { onError("Listen failed: ${e.message}"); return@addSnapshotListener }
                if (snap == null) { onResult(emptyList()); return@addSnapshotListener }

                // Convert Firestore docs into CloudCard objects
                val items = snap.documents.map { doc ->
                    val card = CloudCard(
                        id = doc.id,
                        deckId = doc.getString("deckId").orEmpty(),
                        front = doc.getString("front").orEmpty(),
                        back = doc.getString("back").orEmpty(),
                        intervalDays = (doc.getLong("intervalDays") ?: 1L).toInt(),
                        dueDate = doc.getString("dueDate").orEmpty(),
                        lastReviewed = doc.getString("lastReviewed")
                    )
                    doc.id to card
                }

                onResult(items)
            }
    }
}