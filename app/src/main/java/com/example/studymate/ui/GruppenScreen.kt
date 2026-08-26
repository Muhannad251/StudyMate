package com.example.studymate.ui

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.studymate.viewmodel.GruppenViewModel

@Composable
fun GruppenScreen(
    gruppenViewModel: GruppenViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var gruppenName by remember { mutableStateOf("") }
    var gruppenCode by remember { mutableStateOf("") }
    var aufgabenTitel by remember { mutableStateOf("") }
    var modulName by remember { mutableStateOf("") }
    var ausgewaehlteAufgabeId by remember { mutableStateOf("") }

    val statusText = gruppenViewModel.statusText.value
    val aktiveGruppenId = gruppenViewModel.aktiveGruppenId.value
    val aktiverGruppenName = gruppenViewModel.aktiverGruppenName.value
    val aktiverGruppenCode = gruppenViewModel.aktiverGruppenCode.value
    val gruppenAufgaben = gruppenViewModel.gruppenAufgabenListe.value

    val kameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bild ->
        if (bild != null && ausgewaehlteAufgabeId.isNotBlank()) {
            gruppenViewModel.aufgabeMitBeweisErledigen(ausgewaehlteAufgabeId)
            Toast.makeText(
                context,
                "Foto aufgenommen. Aufgabe erledigt.",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(
                context,
                "Kein Foto aufgenommen.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val kameraErlaubnisLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { erlaubt ->
        if (erlaubt) {
            kameraLauncher.launch(null)
        } else {
            Toast.makeText(
                context,
                "Kamera-Erlaubnis wurde nicht gegeben.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun kameraStarten(aufgabeId: String) {
        ausgewaehlteAufgabeId = aufgabeId

        val erlaubnis = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        )

        if (erlaubnis == PackageManager.PERMISSION_GRANTED) {
            kameraLauncher.launch(null)
        } else {
            kameraErlaubnisLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text(
            text = "Task Gruppen",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Gemeinsame Aufgaben online mit anderen Nutzer*innen bearbeiten.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (statusText.isNotBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Text(
                    text = statusText,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Gruppe erstellen",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = gruppenName,
                    onValueChange = { gruppenName = it },
                    label = { Text("Gruppenname") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        gruppenViewModel.gruppeErstellen(gruppenName)
                        gruppenName = ""
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Gruppe erstellen")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Gruppe beitreten",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = gruppenCode,
                    onValueChange = { gruppenCode = it },
                    label = { Text("Gruppencode") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        gruppenViewModel.gruppeBeitreten(gruppenCode)
                        gruppenCode = ""
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Gruppe beitreten")
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (aktiveGruppenId.isNotBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Aktive Gruppe",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Name: $aktiverGruppenName")
                    Text("Code: $aktiverGruppenCode")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Neue Gruppenaufgabe",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = aufgabenTitel,
                        onValueChange = { aufgabenTitel = it },
                        label = { Text("Aufgabe") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = modulName,
                        onValueChange = { modulName = it },
                        label = { Text("Modul") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            gruppenViewModel.gruppenAufgabeErstellen(
                                titel = aufgabenTitel,
                                modulName = modulName
                            )

                            aufgabenTitel = ""
                            modulName = ""
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Gruppenaufgabe speichern")
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Gruppenaufgaben",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (gruppenAufgaben.isEmpty()) {
                Text("Noch keine Gruppenaufgaben vorhanden.")
            }

            gruppenAufgaben.forEach { aufgabe ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = aufgabe.titel,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Text("Modul: ${aufgabe.modulName}")

                        Spacer(modifier = Modifier.height(8.dp))

                        if (aufgabe.erledigt) {
                            Text("Status: Erledigt")
                            Text("Kamera-Beweis: vorhanden")
                        } else {
                            Text("Status: Offen")

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    kameraStarten(aufgabe.id)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Mit Kamera-Beweis erledigen")
                            }
                        }
                    }
                }
            }
        } else {
            Text(
                text = "Erstelle eine Gruppe oder tritt einer Gruppe bei, um Aufgaben zu teilen."
            )
        }
    }
}