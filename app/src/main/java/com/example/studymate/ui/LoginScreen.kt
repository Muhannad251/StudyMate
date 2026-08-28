package com.example.studymate.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun LoginScreen(
    onLoginErfolgreich: () -> Unit
) {
    val context = LocalContext.current

    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()

    var nutzerName by remember {
        mutableStateOf("")
    }

    var email by remember {
        mutableStateOf("")
    }

    var passwort by remember {
        mutableStateOf("")
    }

    var istRegistrierung by remember {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "StudyMate",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (istRegistrierung) {
                "Neues Konto erstellen"
            } else {
                "Bei deinem Konto anmelden"
            }
        )

        Spacer(modifier = Modifier.height(28.dp))

        if (istRegistrierung) {

            OutlinedTextField(
                value = nutzerName,
                onValueChange = {
                    nutzerName = it
                },
                label = {
                    Text("Nutzername")
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))
        }

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
            },
            label = {
                Text("E-Mail")
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = passwort,
            onValueChange = {
                passwort = it
            },
            label = {
                Text("Passwort")
            },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {

                if (email.isBlank() || passwort.isBlank()) {

                    Toast.makeText(
                        context,
                        "Bitte E-Mail und Passwort eingeben.",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@Button
                }

                if (istRegistrierung) {

                    if (nutzerName.isBlank()) {

                        Toast.makeText(
                            context,
                            "Bitte Nutzername eingeben.",
                            Toast.LENGTH_SHORT
                        ).show()

                        return@Button
                    }

                    auth.createUserWithEmailAndPassword(
                        email.trim(),
                        passwort
                    )
                        .addOnSuccessListener { ergebnis ->

                            val userId =
                                ergebnis.user?.uid ?: return@addOnSuccessListener

                            val nutzer = hashMapOf(
                                "nutzerName" to nutzerName.trim(),
                                "email" to email.trim(),
                                "erstelltAm" to System.currentTimeMillis()
                            )

                            firestore
                                .collection("users")
                                .document(userId)
                                .set(nutzer)
                                .addOnSuccessListener {

                                    Toast.makeText(
                                        context,
                                        "Konto wurde erstellt.",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    onLoginErfolgreich()
                                }
                                .addOnFailureListener { fehler ->

                                    Toast.makeText(
                                        context,
                                        "Nutzer konnte nicht gespeichert werden: ${fehler.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                        }
                        .addOnFailureListener { fehler ->

                            Toast.makeText(
                                context,
                                "Registrierung fehlgeschlagen: ${fehler.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }

                } else {

                    auth.signInWithEmailAndPassword(
                        email.trim(),
                        passwort
                    )
                        .addOnSuccessListener {

                            Toast.makeText(
                                context,
                                "Anmeldung erfolgreich.",
                                Toast.LENGTH_SHORT
                            ).show()

                            onLoginErfolgreich()
                        }
                        .addOnFailureListener { fehler ->

                            Toast.makeText(
                                context,
                                "Anmeldung fehlgeschlagen: ${fehler.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {

            Text(
                text = if (istRegistrierung) {
                    "Registrieren"
                } else {
                    "Anmelden"
                }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                istRegistrierung = !istRegistrierung
            },
            modifier = Modifier.fillMaxWidth()
        ) {

            Text(
                text = if (istRegistrierung) {
                    "Ich habe schon ein Konto"
                } else {
                    "Noch kein Konto? Registrieren"
                }
            )
        }
    }
}