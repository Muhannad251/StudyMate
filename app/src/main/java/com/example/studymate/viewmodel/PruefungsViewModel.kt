package com.example.studymate.viewmodel

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studymate.data.Exam
import com.example.studymate.data.ExamDao
import com.example.studymate.scheduleExamReminders
import kotlinx.coroutines.launch

class PruefungsViewModel(
    private val examDao: ExamDao
) : ViewModel() {

    // hier liegen alle gespeicherte prüfungen
    var pruefungsListe = mutableStateOf<List<Exam>>(emptyList())
        private set

    // prüfungen aus Room laden
    fun pruefungenLaden() {
        viewModelScope.launch {
            pruefungsListe.value = examDao.getAllExams()
        }
    }

    // neue prüfung speichern + erinnerung planen
    fun pruefungSpeichern(
        context: Context,
        pruefungsName: String,
        zielOrt: String,
        pruefungsZeit: String,
        pruefungsDatum: String
    ) {
        viewModelScope.launch {
            val neuePruefung = Exam(
                examName = pruefungsName,
                destination = zielOrt,
                examTime = pruefungsZeit,
                examDate = pruefungsDatum
            )

            examDao.insertExam(neuePruefung)

            // erinnerung 7 tage, 1 tag und 1 stunde vorher
            scheduleExamReminders(
                context = context,
                examName = pruefungsName,
                examDate = pruefungsDatum,
                examTime = pruefungsZeit
            )

            pruefungenLaden()
        }
    }
}