package com.example.studymate

import com.example.studymate.data.Task
import com.example.studymate.data.TaskDao
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.room.Room
import com.example.studymate.data.AppDatabase
import com.example.studymate.data.Exam
import com.example.studymate.data.ExamDao
import com.example.studymate.ui.theme.StudyMateTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "studymate_database"
        )
            .fallbackToDestructiveMigration()
            .build()

        val examDao = db.examDao()
        val taskDao =db.taskDao()

        setContent {
            StudyMateTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->

                    DashboardScreen(
                        examDao = examDao,
                        taskDao = taskDao,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

/* ---------------- DASHBOARD ---------------- */

@Composable
fun DashboardScreen(
    examDao: ExamDao,
    taskDao:TaskDao,
    modifier: Modifier = Modifier
) {
    var currentScreen by remember { mutableStateOf("dashboard") }

    when (currentScreen) {

        "dashboard" -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.Center
            ) {

                Text(
                    text = "StudyMate Dashboard",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        currentScreen = "mobility"
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Mobility Reminder")
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        currentScreen = "tasks"
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Task System")
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Progress Tracker")
                }
            }
        }

        "mobility" -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {

                Button(
                    onClick = {
                        currentScreen = "dashboard"
                    }
                ) {
                    Text("← Zurück")
                }

                Spacer(modifier = Modifier.height(12.dp))

                MobilityReminderScreen(
                    examDao = examDao,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        "tasks" -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {

                Button(
                    onClick = {
                        currentScreen = "dashboard"
                    }
                ) {
                    Text("← Zurück")
                }

                Spacer(modifier = Modifier.height(12.dp))

                TaskScreen(
                    taskDao = taskDao,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/* ---------------- TASK SCREEN ---------------- */

@Composable
fun TaskScreen(
    taskDao: TaskDao,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    var taskTitle by remember { mutableStateOf("") }
    var moduleName by remember { mutableStateOf("") }
    var taskList by remember { mutableStateOf(listOf<Task>()) }

    LaunchedEffect(Unit) {
        taskList = taskDao.getAllTasks()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Column {

            Text(
                text = "Task Manager",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = taskTitle,
                onValueChange = { taskTitle = it },
                label = { Text("Task Name") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = moduleName,
                onValueChange = { moduleName = it },
                label = { Text("Modul Name") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    val newTask = Task(
                        title = taskTitle,
                        moduleName = moduleName,
                        isDone = false
                    )

                    scope.launch {
                        taskDao.insertTask(newTask)
                        taskList = taskDao.getAllTasks()
                    }

                    taskTitle = ""
                    moduleName = ""
                    println("Task gespeichert")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Task speichern")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Gespeicherte Tasks",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            taskList.forEach { task ->

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Text(text = "Modul: ${task.moduleName}")

                        Text(
                            text = if (task.isDone) {
                                "Status: Erledigt"
                            } else {
                                "Status: Offen"
                            }
                        )
                    }
                }
            }
        }
    }
}


/* ---------------- MOBILITY SCREEN ---------------- */

@Composable
fun MobilityReminderScreen(
    examDao: ExamDao,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()


    var examName by remember { mutableStateOf("Mobile Computing Prüfung") }
    var destination by remember { mutableStateOf("TH Köln Campus Gummersbach") }
    var examTime by remember { mutableStateOf("10:00 Uhr") }
    var examDate by remember { mutableStateOf("20.06.2026") }

    val travelTime = "Wird später berechnet"
    val leaveTime = "Wird später berechnet"

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {

            Text(
                text = "Mobility Reminder",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = examName,
                onValueChange = { examName = it },
                label = { Text("Prüfungsname") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = examDate,
                onValueChange = { examDate = it },
                label = { Text("Prüfungsdatum") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = destination,
                onValueChange = { destination = it },
                label = { Text("Zielort") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = examTime,
                onValueChange = { examTime = it },
                label = { Text("Prüfungszeit") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    val uri = Uri.parse("geo:0,0?q=${Uri.encode(destination)}")
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Route in Google Maps öffnen")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    val newExam = Exam(
                        examName = examName,
                        destination = destination,
                        examTime = examTime,
                        examDate = examDate
                    )

                    scope.launch {
                        examDao.insertExam(newExam)
                    }

                    println("Prüfung gespeichert")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Prüfung speichern")
            }
        }
    }
}

/* ---------------- INFO ROW ---------------- */

@Composable
fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column {
            Text(title)
            Text(value)
        }
    }
}

/* ---------------- PREVIEW ---------------- */

@Preview(showBackground = true)
@Composable
fun PreviewScreen() {
    StudyMateTheme {
        Text("Preview")
    }
}