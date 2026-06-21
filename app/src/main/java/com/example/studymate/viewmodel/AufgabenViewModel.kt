package com.example.studymate.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studymate.data.Task
import com.example.studymate.data.TaskDao
import kotlinx.coroutines.launch

class AufgabenViewModel(
    private val taskDao: TaskDao
) : ViewModel() {

    // hier speichern wir alle aufgaben für die UI
    var aufgabenListe = mutableStateOf<List<Task>>(emptyList())
        private set

    // alle aufgaben aus Room laden
    fun aufgabenLaden() {
        viewModelScope.launch {
            aufgabenListe.value = taskDao.getAllTasks()
        }
    }

    // neue aufgabe speichern
    fun aufgabeSpeichern(aufgabenName: String, modulName: String) {
        viewModelScope.launch {
            val neueAufgabe = Task(
                title = aufgabenName,
                moduleName = modulName,
                isDone = false
            )

            taskDao.insertTask(neueAufgabe)
            aufgabenLaden()
        }
    }

    // status ändern: offen oder erledigt
    fun aufgabenStatusAendern(aufgabeId: Int, erledigt: Boolean) {
        viewModelScope.launch {
            taskDao.updateTaskStatus(aufgabeId, erledigt)
            aufgabenLaden()
        }
    }
}