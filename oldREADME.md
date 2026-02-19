# Overview

Flashcards Lite Mobile is a simple flashcards app built to practice modern Android development with Jetpack Compose and to learn how to manage app state, navigation, and persistent storage in a real project.

In the app, you create “decks” (topics) and add cards (front/back text) to each deck. You can edit or delete cards, and you can review cards that are due. During review, you reveal the back of the card and rate how well you remembered it (Again/Hard/Good/Easy). Your rating updates the card’s next due date using a lightweight spaced-repetition approach so that easier cards come back later and harder cards come back sooner.

My purpose in creating this app was to get hands-on experience building a multi-screen mobile UI, handling user input, saving data locally so it persists between sessions, and understanding how Compose recomposition and state updates affect the user experience.

[Software Demo Video](https://youtu.be/04f7zcbUMWM)

# Development Environment

**Tools**
- Android Studio
- Android Emulator (or a physical Android device)
- Git + GitHub (version control and publishing)

**Language & Libraries**
- Kotlin
- Jetpack Compose (UI)
- Navigation Compose (multi-screen navigation)
- AndroidX DataStore (local persistence)
- Kotlinx Serialization (JSON encoding/decoding)

# Useful Websites

* [Android Developers — Jetpack Compose](https://developer.android.com/jetpack/compose)
* [Android Developers — Navigation for Compose](https://developer.android.com/jetpack/compose/navigation)
* [Android Developers — DataStore](https://developer.android.com/topic/libraries/architecture/datastore)
* [kotlinx.serialization — GitHub](https://github.com/Kotlin/kotlinx.serialization)
* [Kotlin Documentation](https://kotlinlang.org/docs/home.html)

# Future Work

* Improve the UI for long text (wrap/truncate card rows and add better spacing on smaller screens).
* Add search and sorting (search decks/cards, sort by due date or name).
* Add deck statistics (cards due today, total cards, review history).
* Add optional features like importing/exporting decks to JSON and adding notifications for daily reviews.