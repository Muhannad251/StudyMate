package com.example.studymate

import com.example.studymate.ui.LoginScreen
import com.google.firebase.auth.FirebaseAuth
import androidx.activity.compose.BackHandler
import com.example.studymate.viewmodel.GruppenViewModel
import com.example.studymate.ui.GruppenScreen
import com.example.studymate.viewmodel.PruefungsViewModel
import com.example.studymate.viewmodel.AufgabenViewModel
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

        // kanal für benachrichtigung
        benachrichtigungsKanalErstellen()

// erlaubnis für benachrichtigung
        benachrichtigungsErlaubnisAnfragen()


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

                val auth = FirebaseAuth.getInstance()

                var istEingeloggt by remember {
                    mutableStateOf(
                        auth.currentUser != null &&
                                auth.currentUser?.isAnonymous == false
                    )
                }

                if (istEingeloggt) {

                    Scaffold(
                        modifier = Modifier.fillMaxSize()
                    ) { innerPadding ->

                        DashboardScreen(
                            examDao = examDao,
                            taskDao = taskDao,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }

                } else {

                    LoginScreen(
                        onLoginErfolgreich = {
                            istEingeloggt = true
                        }
                    )
                }
            }
        }
        }


    private fun benachrichtigungsKanalErstellen() {
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

    private fun benachrichtigungsErlaubnisAnfragen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            val erlaubnisStatus = ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            )

            if (erlaubnisStatus != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
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

    BackHandler(enabled = currentScreen != "dashboard") {
        currentScreen = "dashboard"
    }

    // ViewModel für aufgaben erstellen
    val aufgabenViewModel = remember {
        AufgabenViewModel(taskDao)
    }

    // ViewModel für prüfungen
    val pruefungsViewModel = remember {
        PruefungsViewModel(examDao)
    }

    val context = LocalContext.current

    val gruppenViewModel = remember {
        GruppenViewModel(context.applicationContext)
    }

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
                        description = "Fortschritt deiner Aufgaben ansehen",
                        title = "📊 Progress Tracker",
                        onClick = { currentScreen = "progress" }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    DashboardCard(
                        title = "👥 Task Gruppen",
                        description = "Gemeinsame Aufgaben online bearbeiten",
                        onClick = { currentScreen = "gruppen" }
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
                    pruefungsViewModel = pruefungsViewModel,
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
                    aufgabenViewModel = aufgabenViewModel,
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
                    aufgabenViewModel = aufgabenViewModel,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        "gruppen" -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Button(onClick = { currentScreen = "dashboard" }) {
                    Text("← Zurück")
                }

                Spacer(modifier = Modifier.height(12.dp))

                GruppenScreen(
                    gruppenViewModel = gruppenViewModel,
                    modifier = Modifier.weight(1f)
                )
            }
        }

    }
}

@Composable
fun ProgressScreen(
    aufgabenViewModel: AufgabenViewModel,
    modifier: Modifier = Modifier
) {


    // aufgaben aus dem ViewModel benutzen
    val aufgabenListe = aufgabenViewModel.aufgabenListe.value

    LaunchedEffect(Unit) {
        aufgabenViewModel.aufgabenLaden()
    }

    val totalTasks = aufgabenListe.size
    val doneTasks = aufgabenListe.count { it.isDone }
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
                text = description,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(6.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

    }
}
/* ---------------- TASK SCREEN ---------------- */

@Composable
fun TaskScreen(
    aufgabenViewModel: AufgabenViewModel,
    modifier: Modifier = Modifier
)
{
    var aufgabenName by remember { mutableStateOf("") }
    var modulName by remember { mutableStateOf("") }

    val aufgabenListe = aufgabenViewModel.aufgabenListe.value

    LaunchedEffect(Unit) {
        aufgabenViewModel.aufgabenLaden()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {


            Text(
                text = "Task Manager",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = aufgabenName,
                onValueChange = { aufgabenName = it },
                label = { Text("Task Name") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = modulName,
                onValueChange = { modulName = it },
                label = { Text("Modul Name") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    aufgabenViewModel.aufgabeSpeichern(
                        aufgabenName = aufgabenName,
                        modulName = modulName
                    )

                    aufgabenName = ""
                    modulName = ""
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

            aufgabenListe.forEach { task ->

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
                                    aufgabenViewModel.aufgabenStatusAendern(
                                        aufgabeId = task.id,
                                        erledigt = checked
                                    )
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
    try {
        // datum und uhrzeit lesen
        val dateFormat = SimpleDateFormat(
            "dd.MM.yyyy HH:mm",
            Locale.GERMANY
        )

        // falsche datum eingaben nicht erlauben
        dateFormat.isLenient = false

        val examDateTime = dateFormat.parse(
            "$examDate $examTime"
        ) ?: return

        // erinnerungen planen
        val reminderTimes = listOf(
            System.currentTimeMillis() + 30_000L, // test nach 30 sekunden
            examDateTime.time - 7L * 24 * 60 * 60 * 1000,
            examDateTime.time - 1L * 24 * 60 * 60 * 1000,
            examDateTime.time - 1L * 60 * 60 * 1000
        )

        val alarmManager =
            context.getSystemService(AlarmManager::class.java)

        reminderTimes.forEachIndexed { index, erinnerungsZeit ->

            if (erinnerungsZeit > System.currentTimeMillis()) {

                val intent = Intent(
                    context,
                    ExamReminderReceiver::class.java
                ).apply {
                    putExtra("examName", examName)
                }

                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    examName.hashCode() + index,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or
                            PendingIntent.FLAG_IMMUTABLE
                )

                if (
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                    alarmManager.canScheduleExactAlarms()
                ) {
                    alarmManager.setAlarmClock(
                        AlarmManager.AlarmClockInfo(
                            erinnerungsZeit,
                            pendingIntent
                        ),
                        pendingIntent
                    )
                } else {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        erinnerungsZeit,
                        pendingIntent
                    )
                }
            }
        }

    } catch (fehler: Exception) {
        println(
            "Erinnerung konnte nicht geplant werden: ${fehler.message}"
        )
    }
}



@Composable
fun MobilityReminderScreen(
    pruefungsViewModel: PruefungsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var examName by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var examTime by remember { mutableStateOf("") }
    var examDate by remember { mutableStateOf("") }

// prüfungen kommen jetzt aus ViewModel
    val pruefungsListe = pruefungsViewModel.pruefungsListe.value

    LaunchedEffect(Unit) {
        pruefungsViewModel.pruefungenLaden()
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
                    // mobiles feature: navigation zum zielort
                    val uri = Uri.parse("google.navigation:q=${Uri.encode(destination)}")
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Navigation zum Prüfungsort starten")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    pruefungsViewModel.pruefungSpeichern(
                        context = context,
                        pruefungsName = examName,
                        zielOrt = destination,
                        pruefungsZeit = examTime,
                        pruefungsDatum = examDate
                    )

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

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Gespeicherte Prüfungen",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            pruefungsListe.forEach { exam ->

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

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                val uri = Uri.parse(
                                    "google.navigation:q=${Uri.encode(exam.destination)}"
                                )

                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    uri
                                )

                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Navigation starten")
                        }
                    }
                }
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