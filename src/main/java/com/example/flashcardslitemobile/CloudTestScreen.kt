package com.example.flashcardslitemobile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun CloudTestScreen(
    onSignUp: (String, String) -> Unit,
    onSignIn: (String, String) -> Unit,
    onBack: () -> Unit,
    currentUserEmail: String?,
    statusMessage: String,
    modifier: Modifier = Modifier
) {
    // input fields
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // local status is for quick validation errors (before calling firebase)
    var localStatus by remember { mutableStateOf("") }

    // small helper to validate and return cleaned credentials
    fun validate(): Pair<String, String>? {
        val e = email.trim()
        val p = password.trim()

        // basic validation so we don't call firebase with empty inputs
        if (e.isBlank() || p.isBlank()) {
            localStatus = "Email and password can't be empty"
            return null
        }

        // clear local error if valid
        localStatus = ""
        return e to p
    }

    Column(
        modifier = modifier.padding(16.dp).fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Cloud Test")
        Spacer(modifier = Modifier.height(12.dp))

        // show current user email (if logged in)
        Text("Current user: ${currentUserEmail ?: "Not logged in"}")

        // email input
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )

        // password input (hidden)
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = modifier.fillMaxWidth()
        )

        // two buttons side by side
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = {
                    // validate first, then call sign up callback
                    val creds = validate() ?: return@Button
                    onSignUp(creds.first, creds.second)
                },
                modifier = Modifier.weight(1f)
            ) { Text("Sign Up") }

            Button(
                onClick = {
                    // validate first, then call sign in callback
                    val creds = validate() ?: return@Button
                    onSignIn(creds.first, creds.second)
                },
                modifier = Modifier.weight(1f)
            ) { Text("Sign In") }
        }

        // show validation error first, otherwise show VM status message
        val msg = localStatus.ifBlank { statusMessage }
        if (msg.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(msg)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // go back to previous screen
        Button(onClick = onBack) { Text("Back") }
    }
}