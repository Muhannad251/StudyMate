package com.example.studymate.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.studymate.data.GruppenAufgabe
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class GruppenViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    var statusText = mutableStateOf("")
        private set

    var aktuelleUserId = mutableStateOf("")
        private set

    var aktiveGruppenId = mutableStateOf("")
        private set

    var aktiverGruppenName = mutableStateOf("")
        private set

    var aktiverGruppenCode = mutableStateOf("")
        private set

    var gruppenAufgabenListe = mutableStateOf<List<GruppenAufgabe>>(emptyList())
        private set

    init {
        anonymAnmelden()
    }

    private fun anonymAnmelden() {
        val vorhandenerUser = auth.currentUser

        if (vorhandenerUser != null) {
            aktuelleUserId.value = vorhandenerUser.uid
            statusText.value = "Angemeldet"
        } else {
            auth.signInAnonymously()
                .addOnSuccessListener { ergebnis ->
                    aktuelleUserId.value = ergebnis.user?.uid ?: ""
                    statusText.value = "Anonym angemeldet"
                }
                .addOnFailureListener { fehler ->
                    statusText.value = "Anmeldung fehlgeschlagen: ${fehler.message}"
                }
        }
    }

    fun gruppeErstellen(gruppenName: String) {
        val userId = auth.currentUser?.uid ?: return

        if (gruppenName.isBlank()) {
            statusText.value = "Bitte Gruppennamen eingeben"
            return
        }

        val gruppenCode = codeErstellen()
        val gruppenDokument = firestore.collection("groups").document()

        val gruppe = hashMapOf(
            "name" to gruppenName,
            "code" to gruppenCode,
            "createdBy" to userId,
            "createdAt" to System.currentTimeMillis(),
            "members" to listOf(userId)
        )

        gruppenDokument.set(gruppe)
            .addOnSuccessListener {
                aktiveGruppenId.value = gruppenDokument.id
                aktiverGruppenName.value = gruppenName
                aktiverGruppenCode.value = gruppenCode
                statusText.value = "Gruppe erstellt"

                gruppenAufgabenLaden(gruppenDokument.id)
            }
            .addOnFailureListener { fehler ->
                statusText.value = "Gruppe konnte nicht erstellt werden: ${fehler.message}"
            }
    }

    fun gruppeBeitreten(code: String) {
        val userId = auth.currentUser?.uid ?: return
        val saubererCode = code.trim().uppercase()

        if (saubererCode.isBlank()) {
            statusText.value = "Bitte Gruppencode eingeben"
            return
        }

        firestore.collection("groups")
            .whereEqualTo("code", saubererCode)
            .limit(1)
            .get()
            .addOnSuccessListener { ergebnis ->
                if (ergebnis.isEmpty) {
                    statusText.value = "Keine Gruppe mit diesem Code gefunden"
                    return@addOnSuccessListener
                }

                val dokument = ergebnis.documents.first()
                val gruppenName = dokument.getString("name") ?: "Gruppe"

                dokument.reference.update("members", FieldValue.arrayUnion(userId))
                    .addOnSuccessListener {
                        aktiveGruppenId.value = dokument.id
                        aktiverGruppenName.value = gruppenName
                        aktiverGruppenCode.value = saubererCode
                        statusText.value = "Gruppe beigetreten"

                        gruppenAufgabenLaden(dokument.id)
                    }
            }
            .addOnFailureListener { fehler ->
                statusText.value = "Beitreten fehlgeschlagen: ${fehler.message}"
            }
    }

    fun gruppenAufgabeErstellen(titel: String, modulName: String) {
        val userId = auth.currentUser?.uid ?: return
        val gruppenId = aktiveGruppenId.value

        if (gruppenId.isBlank()) {
            statusText.value = "Bitte zuerst Gruppe erstellen oder beitreten"
            return
        }

        if (titel.isBlank()) {
            statusText.value = "Bitte Aufgabentitel eingeben"
            return
        }

        val aufgabenDokument = firestore
            .collection("groups")
            .document(gruppenId)
            .collection("tasks")
            .document()

        val aufgabe = hashMapOf(
            "titel" to titel,
            "modulName" to modulName,
            "erledigt" to false,
            "erstelltVon" to userId,
            "erledigtVon" to "",
            "proofVorhanden" to false,
            "erstelltAm" to System.currentTimeMillis(),
            "erledigtAm" to 0L
        )

        aufgabenDokument.set(aufgabe)
            .addOnSuccessListener {
                statusText.value = "Gruppenaufgabe gespeichert"
            }
            .addOnFailureListener { fehler ->
                statusText.value = "Aufgabe konnte nicht gespeichert werden: ${fehler.message}"
            }
    }

    fun aufgabeMitBeweisErledigen(aufgabenId: String) {
        val userId = auth.currentUser?.uid ?: return
        val gruppenId = aktiveGruppenId.value

        if (gruppenId.isBlank()) {
            statusText.value = "Keine aktive Gruppe"
            return
        }

        firestore
            .collection("groups")
            .document(gruppenId)
            .collection("tasks")
            .document(aufgabenId)
            .update(
                mapOf(
                    "erledigt" to true,
                    "erledigtVon" to userId,
                    "proofVorhanden" to true,
                    "erledigtAm" to System.currentTimeMillis()
                )
            )
            .addOnSuccessListener {
                statusText.value = "Aufgabe mit Kamera-Beweis erledigt"
            }
            .addOnFailureListener { fehler ->
                statusText.value = "Status konnte nicht geändert werden: ${fehler.message}"
            }
    }

    private fun gruppenAufgabenLaden(gruppenId: String) {
        firestore
            .collection("groups")
            .document(gruppenId)
            .collection("tasks")
            .addSnapshotListener { ergebnis, fehler ->
                if (fehler != null) {
                    statusText.value = "Aufgaben konnten nicht geladen werden: ${fehler.message}"
                    return@addSnapshotListener
                }

                val neueListe = ergebnis?.documents?.map { dokument ->
                    GruppenAufgabe(
                        id = dokument.id,
                        titel = dokument.getString("titel")
                            ?: dokument.getString("title")
                            ?: "",
                        modulName = dokument.getString("modulName")
                            ?: dokument.getString("moduleName")
                            ?: "",
                        erledigt = dokument.getBoolean("erledigt")
                            ?: dokument.getBoolean("isDone")
                            ?: false,
                        erstelltVon = dokument.getString("erstelltVon")
                            ?: dokument.getString("createdBy")
                            ?: "",
                        erledigtVon = dokument.getString("erledigtVon")
                            ?: dokument.getString("doneBy")
                            ?: "",
                        proofVorhanden = dokument.getBoolean("proofVorhanden") ?: false,
                        erstelltAm = dokument.getLong("erstelltAm") ?: 0L,
                        erledigtAm = dokument.getLong("erledigtAm") ?: 0L
                    )
                } ?: emptyList()

                gruppenAufgabenListe.value = neueListe
            }
    }

    private fun codeErstellen(): String {
        val zeichen = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..6)
            .map { zeichen.random() }
            .joinToString("")
    }
}