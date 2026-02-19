# Overview

Flashcards Lite Mobile is a lightweight flashcards app I’m building to practice modern Android development with Jetpack Compose while also learning how to persist app data in the cloud.

In the app, you create “decks” (topics) and add cards (front/back text) to each deck. Cards can be created, edited, deleted, and reviewed. During review, you reveal the back of the card and rate how well you remembered it (Again/Hard/Good/Easy). That rating updates the card’s spaced-repetition fields (such as interval, ease factor, repetitions, and next due date) so the app can prioritize the cards that are due next.

For this module, the app integrates with a cloud database using Firebase Cloud Firestore. Decks and cards are stored in Firestore so the data persists between sessions and can be retrieved again when the app reopens. My goal is to get hands-on experience with a real cloud-backed mobile architecture: creating, reading, updating, and deleting data while keeping the UI in sync with the database.


[Software Demo Video](https://youtu.be/qT4qDSt70lo)

# Cloud Database

**Cloud Database Used**
- Firebase Cloud Firestore (NoSQL document database)

**Database Structure**
Firestore stores data using collections and documents. This app uses a structure that supports decks and cards, with cards referencing their parent deck using `deckId`.

Example structure:

- `decks` (collection)
    - `{deckId}` (document)
        - `id`: number (Int)
        - `name`: string
        - `createdAt`: timestamp 

- `cards` (collection)
    - `{cardId}` (document)
        - `id`: number (Int)
        - `deckId`: number (Int)
        - `front`: string
        - `back`: string
        - `dueDate`: string
        - `intervalDays`: number (Int)
        - `lastReviewed`: String?
        - `createdAt`: timestamp 
        - `updatedAt`: timestamp/number 

This structure allows the app to:
- Insert new decks/cards into Firestore
- Retrieve/query decks and cards (including filtering “cards due”)
- Modify existing documents when editing a card or updating spaced repetition fields
- Delete cards (and optionally decks)

# Development Environment

**Tools**
- Android Studio
- Android Emulator (or a physical Android device)
- Firebase Console (Firestore configuration)
- Git + GitHub (version control and publishing)

**Language & Libraries**
- Kotlin
- Jetpack Compose (UI)
- Navigation Compose (multi-screen navigation)
- Firebase (BOM) + Cloud Firestore (cloud database)
- Kotlin Coroutines (async work)


# Useful Websites

- [Android Developers — Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Android Developers — Navigation for Compose](https://developer.android.com/jetpack/compose/navigation)
- [Firebase — Cloud Firestore](https://firebase.google.com/docs/firestore)
- [Firebase — Add Firebase to Android](https://firebase.google.com/docs/android/setup)
- [Kotlin Documentation](https://kotlinlang.org/docs/home.html)

# Future Work

- Improve the UI for long text (better wrapping/truncation and spacing on small screens).
- Add search and sorting for decks/cards (by name or due date).
- Add deck statistics (cards due today, total cards, review history).
- Improve Firestore queries (ordering, pagination, and faster “due cards” filtering).
- Add optional features like reminders/notifications for daily reviews.
