package com.example.flashcardslitemobile

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class FirebaseService {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    fun currentEmail(): String? = auth.currentUser?.email

    fun signUp(email: String, password: String, onResult: (String) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { onResult("Signed up!") }
            .addOnFailureListener { e -> onResult("Sign Up Failed: ${e.message}")}
    }

    fun signIn(email: String, password: String, onResult: (String) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { onResult("Signed in!")}
            .addOnFailureListener { e -> onResult("Sign In failed: ${e.message}")}
    }

    fun signOut(onResult: (String) -> Unit) {
        auth.signOut()
        onResult("Signed Out!")
    }

    fun addTestNote(text: String, onResult: (String) -> Unit) {
        val email = currentEmail() ?: run {
            onResult("You need to be logged in.")
            return
        }

        val data = hashMapOf(
            "text" to text,
            "owner" to email,
            "updatedAt" to System.currentTimeMillis()
        )

        db.collection("test_notes")
            .add(data)
            .addOnSuccessListener { onResult("Note added!") }
            .addOnFailureListener { e -> onResult("Add failed: ${e.message}") }
    }

    fun loadMyTestNotes(
        onResult: (List<Pair<String, String>>) -> Unit,
        onError: (String) -> Unit
    ) {
        val email = currentEmail() ?: run {
            onError("You need to be logged in.")
            return
        }

        db.collection("test_notes")
            .whereEqualTo("owner", email)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snap ->
                val items = snap.documents.map { doc ->
                    val text = doc.getString("text").orEmpty()
                    doc.id to text
                }
                onResult(items)
            }
            .addOnFailureListener { e -> onError("Load failed: ${e.message}")}
    }

    fun updateTestNotes(noteId: String, newText: String, onResult: (String) -> Unit) {
        db.collection("test_notes")
            .document(noteId)
            .update(
                mapOf(
                    "text" to newText,
                    "updatedAt" to System.currentTimeMillis()
                )
            )
            .addOnSuccessListener { onResult("Note updated!") }
            .addOnFailureListener { e -> onResult("Update failed: ${e.message}") }
    }

    fun deleteTestNote(noteId: String, onResult: (String) -> Unit) {
        db.collection("test_notes")
            .document(noteId)
            .delete()
            .addOnSuccessListener { onResult("Note Deleted!") }
            .addOnFailureListener { e -> onResult("Delete failed: ${e.message}") }
    }

}