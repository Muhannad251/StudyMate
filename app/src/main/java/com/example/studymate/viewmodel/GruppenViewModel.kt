package com.example.studymate.viewmodel

import com.example.studymate.data.GruppenNachricht
import com.google.firebase.firestore.Query
import com.example.studymate.data.GruppeInfo
import com.google.firebase.firestore.ListenerRegistration
import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.studymate.data.GruppenAufgabe
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class GruppenViewModel(
    private val context: Context
) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val gruppenSpeicher =
        context.getSharedPreferences(
            "gruppen_speicher",
            Context.MODE_PRIVATE
        )

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

    var meineGruppenListe = mutableStateOf<List<GruppeInfo>>(emptyList())
        private set

    var gruppenNachrichtenListe = mutableStateOf<List<GruppenNachricht>>(emptyList())
        private set

    var aktuellerNutzerName = mutableStateOf("")
        private set

    private var nachrichtenListener: ListenerRegistration? = null

    private var aufgabenListener: ListenerRegistration? = null

    init {
        angemeldetenUserLaden()
    }

    private fun gespeicherteGruppeLaden() {

        val gruppenId =
            gruppenSpeicher.getString(
                "aktiveGruppenId",
                ""
            ) ?: ""

        val gruppenName =
            gruppenSpeicher.getString(
                "aktiverGruppenName",
                ""
            ) ?: ""

        val gruppenCode =
            gruppenSpeicher.getString(
                "aktiverGruppenCode",
                ""
            ) ?: ""

        if (gruppenId.isNotBlank()) {

            aktiveGruppenId.value = gruppenId
            aktiverGruppenName.value = gruppenName
            aktiverGruppenCode.value = gruppenCode

            gruppenAufgabenLaden(gruppenId)
            gruppenNachrichtenLaden(gruppenId)

            statusText.value = "Gespeicherte Gruppe geladen"
        }
    }

    fun gruppeOeffnen(gruppe: GruppeInfo) {

        aktiveGruppenId.value = gruppe.id
        aktiverGruppenName.value = gruppe.name
        aktiverGruppenCode.value = gruppe.code

        gruppeLokalSpeichern(
            gruppenId = gruppe.id,
            gruppenName = gruppe.name,
            gruppenCode = gruppe.code
        )

        gruppenAufgabenLaden(gruppe.id)

        gruppenNachrichtenLaden(gruppe.id)

        statusText.value = "Gruppe geöffnet"
    }


    private fun meineGruppenLaden() {

        val userId = auth.currentUser?.uid ?: return

        firestore
            .collection("groups")
            .whereArrayContains("members", userId)
            .addSnapshotListener { ergebnis, fehler ->

                if (fehler != null) {
                    statusText.value =
                        "Gruppen konnten nicht geladen werden: ${fehler.message}"
                    return@addSnapshotListener
                }

                val gruppenListe =
                    ergebnis?.documents?.map { dokument ->

                        GruppeInfo(
                            id = dokument.id,
                            name = dokument.getString("name") ?: "",
                            code = dokument.getString("code") ?: "",
                            createdBy = dokument.getString("createdBy") ?: ""
                        )

                    } ?: emptyList()

                meineGruppenListe.value = gruppenListe
            }
    }



    private fun angemeldetenUserLaden() {

        val vorhandenerUser = auth.currentUser

        if (
            vorhandenerUser != null &&
            !vorhandenerUser.isAnonymous
        ) {

            aktuelleUserId.value = vorhandenerUser.uid
            statusText.value = "Angemeldet"

            nutzerNameLaden(vorhandenerUser.uid)

            // alle gruppen laden wo der nutzer mitglied ist
            meineGruppenLaden()

// zuletzt geöffnete gruppe wieder laden
            gespeicherteGruppeLaden()

        } else {

            aktuelleUserId.value = ""
            statusText.value = "Kein Nutzer angemeldet"
        }
    }

    private fun nutzerNameLaden(userId: String) {

        firestore
            .collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { dokument ->

                aktuellerNutzerName.value =
                    dokument.getString("nutzerName") ?: "Nutzer"
            }
            .addOnFailureListener {

                aktuellerNutzerName.value = "Nutzer"
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

                gruppeLokalSpeichern(
                    gruppenId = gruppenDokument.id,
                    gruppenName = gruppenName,
                    gruppenCode = gruppenCode
                )

                gruppenAufgabenLaden(gruppenDokument.id)
                gruppenNachrichtenLaden(gruppenDokument.id)
            }
            .addOnFailureListener { fehler ->
                statusText.value = "Gruppe konnte nicht erstellt werden: ${fehler.message}"
            }
    }

    private fun gruppeLokalSpeichern(
        gruppenId: String,
        gruppenName: String,
        gruppenCode: String
    ) {
        gruppenSpeicher
            .edit()
            .putString(
                "aktiveGruppenId",
                gruppenId
            )
            .putString(
                "aktiverGruppenName",
                gruppenName
            )
            .putString(
                "aktiverGruppenCode",
                gruppenCode
            )
            .apply()
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

                        gruppeLokalSpeichern(
                            gruppenId = dokument.id,
                            gruppenName = gruppenName,
                            gruppenCode = saubererCode
                        )

                        statusText.value = "Gruppe beigetreten"

                        gruppenAufgabenLaden(dokument.id)
                        gruppenNachrichtenLaden(dokument.id)
                    }
            }
            .addOnFailureListener { fehler ->
                statusText.value = "Beitreten fehlgeschlagen: ${fehler.message}"
            }
    }

    fun gruppeVerlassen(gruppenId: String) {

        val userId = auth.currentUser?.uid ?: return

        if (gruppenId.isBlank()) {
            statusText.value = "Keine Gruppe ausgewählt"
            return
        }

        firestore
            .collection("groups")
            .document(gruppenId)
            .update(
                "members",
                FieldValue.arrayRemove(userId)
            )
            .addOnSuccessListener {

                // wenn gerade diese gruppe geöffnet war
                if (aktiveGruppenId.value == gruppenId) {

                    aktiveGruppenId.value = ""
                    aktiverGruppenName.value = ""
                    aktiverGruppenCode.value = ""

                    gruppenAufgabenListe.value = emptyList()

                    // aufgaben-listener stoppen
                    aufgabenListener?.remove()
                    aufgabenListener = null

                    // chat-listener auch stoppen
                    nachrichtenListener?.remove()
                    nachrichtenListener = null

// alte nachrichten aus der oberfläche entfernen
                    gruppenNachrichtenListe.value = emptyList()

                    // gespeicherte aktive gruppe löschen
                    gruppenSpeicher
                        .edit()
                        .remove("aktiveGruppenId")
                        .remove("aktiverGruppenName")
                        .remove("aktiverGruppenCode")
                        .apply()
                }

                statusText.value = "Gruppe verlassen"
            }
            .addOnFailureListener { fehler ->

                statusText.value =
                    "Gruppe konnte nicht verlassen werden: ${fehler.message}"
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

        // alten listener stoppen
        aufgabenListener?.remove()

        // listener für die aktuell geöffnete gruppe starten
        aufgabenListener = firestore
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

    private fun gruppenNachrichtenLaden(gruppenId: String) {

        nachrichtenListener?.remove()

        nachrichtenListener = firestore
            .collection("groups")
            .document(gruppenId)
            .collection("messages")
            .orderBy("erstelltAm", Query.Direction.ASCENDING)
            .addSnapshotListener { ergebnis, fehler ->

                if (fehler != null) {
                    statusText.value =
                        "Nachrichten konnten nicht geladen werden: ${fehler.message}"
                    return@addSnapshotListener
                }

                val nachrichten =
                    ergebnis?.documents?.map { dokument ->

                        GruppenNachricht(
                            id = dokument.id,
                            text = dokument.getString("text") ?: "",
                            absenderId = dokument.getString("absenderId") ?: "",
                            absenderName = dokument.getString("absenderName") ?: "Nutzer",
                            erstelltAm = dokument.getLong("erstelltAm") ?: 0L
                        )

                    } ?: emptyList()

                gruppenNachrichtenListe.value = nachrichten
            }
    }

    fun nachrichtSenden(text: String) {

        val gruppenId = aktiveGruppenId.value
        val userId = auth.currentUser?.uid ?: return
        val nutzerName = aktuellerNutzerName.value

        if (gruppenId.isBlank()) {
            statusText.value = "Keine Gruppe geöffnet"
            return
        }

        if (text.isBlank()) {
            statusText.value = "Bitte Nachricht eingeben"
            return
        }

        val nachricht = hashMapOf(
            "text" to text.trim(),
            "absenderId" to userId,
            "absenderName" to nutzerName,
            "erstelltAm" to System.currentTimeMillis()
        )

        firestore
            .collection("groups")
            .document(gruppenId)
            .collection("messages")
            .add(nachricht)
            .addOnSuccessListener {

                statusText.value = "Nachricht gesendet"
            }
            .addOnFailureListener { fehler ->

                statusText.value =
                    "Nachricht konnte nicht gesendet werden: ${fehler.message}"
            }
    }

    private fun codeErstellen(): String {
        val zeichen = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..6)
            .map { zeichen.random() }
            .joinToString("")
    }
}