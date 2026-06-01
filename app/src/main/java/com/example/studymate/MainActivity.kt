package com.example.studymate


import android.app.AlarmManager
import android.app.PendingIntent
import java.text.SimpleDateFormat
import java.util.Locale
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
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
        val taskDao = db.taskDao()

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


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                "exam_reminder_channel",
                "Exam Reminder",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Benachrichtigungen für Prüfungen"
            }

            val notificationManager =
                getSystemService(NotificationManager::class.java)

            notificationManager.createNotificationChannel(channel)
        }
    }
}
/* ---------------- DASHBOARD ---------------- */


@Composable
fun DashboardScreen(
    examDao: ExamDao,
    taskDao: TaskDao,
    modifier: Modifier = Modifier
) {
    var currentScreen by remember { mutableStateOf("dashboard") }

    when (currentScreen) {

        "dashboard" -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "StudyMate",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "Dein smarter Lernplaner",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    DashboardCard(
                        title = "📍 Mobility Reminder",
                        description = "Prüfungen speichern und Route öffnen",
                        onClick = { currentScreen = "mobility" }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    DashboardCard(
                        title = "✅ Task System",
                        description = "Aufgaben erstellen und abhaken",
                        onClick = { currentScreen = "tasks" }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    DashboardCard(
                        title = "📊 Progress Tracker",
                        description = "Fortschritt deiner Aufgaben ansehen",
                        onClick = { currentScreen = "progress" }
                    )
                }
            }
        }

        "mobility" -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Button(onClick = { currentScreen = "dashboard" }) {
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
                Button(onClick = { currentScreen = "dashboard" }) {
                    Text("← Zurück")
                }

                Spacer(modifier = Modifier.height(12.dp))

                TaskScreen(
                    taskDao = taskDao,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        "progress" -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Button(onClick = { currentScreen = "dashboard" }) {
                    Text("← Zurück")
                }

                Spacer(modifier = Modifier.height(12.dp))

                ProgressScreen(
                    taskDao = taskDao,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun ProgressScreen(
    taskDao: TaskDao,
    modifier: Modifier = Modifier
) {
    var taskList by remember { mutableStateOf(listOf<Task>()) }

    LaunchedEffect(Unit) {
        taskList = taskDao.getAllTasks()
    }

    val totalTasks = taskList.size
    val doneTasks = taskList.count { it.isDone }
    val openTasks = totalTasks - doneTasks

    val progress = if (totalTasks > 0) {
        doneTasks.toFloat() / totalTasks.toFloat()
    } else {
        0f
    }

    val progressPercent = (progress * 100).toInt()

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Column {
            Text(
                text = "Progress Tracker",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Gesamtfortschritt",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Alle Aufgaben: $totalTasks")
                    Text("Erledigt: $doneTasks")
                    Text("Offen: $openTasks")

                    Spacer(modifier = Modifier.height(20.dp))

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "$progressPercent% erledigt",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}



@Composable
fun DashboardCard(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium
            )
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

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = task.isDone,
                                onCheckedChange = { checked ->
                                    scope.launch {
                                        taskDao.updateTaskStatus(task.id, checked)
                                        taskList = taskDao.getAllTasks()
                                    }
                                }
                            )

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
}


/* ---------------- MOBILITY SCREEN ---------------- */


fun scheduleExamReminders(
    context: android.content.Context,
    examName: String,
    examDate: String,
    examTime: String
) {
    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMANY)

    val examDateTime = dateFormat.parse("$examDate $examTime") ?: return

    val reminderTimes = listOf(
        examDateTime.time - 7L * 24 * 60 * 60 * 1000,
        examDateTime.time - 1L * 24 * 60 * 60 * 1000,
        examDateTime.time - 1L * 60 * 60 * 1000
    )

    val alarmManager = context.getSystemService(AlarmManager::class.java)

    reminderTimes.forEachIndexed { index, reminderTime ->

        if (reminderTime > System.currentTimeMillis()) {
            val intent = Intent(context, ExamReminderReceiver::class.java).apply {
                putExtra("examName", examName)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                System.currentTimeMillis().toInt() + index,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                reminderTime,
                pendingIntent
            )
        }
    }
}



@Composable
fun MobilityReminderScreen(
    examDao: ExamDao,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var examName by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var examTime by remember { mutableStateOf("") }
    var examDate by remember { mutableStateOf("") }
    var examList by remember { mutableStateOf(listOf<Exam>()) }

    LaunchedEffect(Unit) {
        examList = examDao.getAllExams()
    }

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

                        scheduleExamReminders(
                            context = context,
                            examName = examName,
                            examDate = examDate,
                            examTime = examTime
                        )

                        examList = examDao.getAllExams()
                    }

                    examName = ""
                    destination = ""
                    examTime = ""
                    examDate = ""

                    println("Prüfung gespeichert")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Prüfung speichern")
            }
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    Text(
        text = "Gespeicherte Prüfungen",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
    )

    Spacer(modifier = Modifier.height(12.dp))

    examList.forEach { exam ->

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = exam.examName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(text = "Datum: ${exam.examDate}")
                Text(text = "Uhrzeit: ${exam.examTime}")
                Text(text = "Ort: ${exam.destination}")
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