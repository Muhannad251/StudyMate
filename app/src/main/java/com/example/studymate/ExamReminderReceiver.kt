package com.example.studymate

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class ExamReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pruefungsName = intent.getStringExtra("examName") ?: "Prüfung"

        // benachrichtigung bauen
        val benachrichtigung = NotificationCompat.Builder(context, "exam_reminder_channel")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Prüfungserinnerung")
            .setContentText("Deine Prüfung $pruefungsName steht bald an.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        // ab Android 13 muss erlaubnis geprüft werden
        val darfBenachrichtigungSenden =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED

        if (darfBenachrichtigungSenden) {
            NotificationManagerCompat.from(context).notify(
                System.currentTimeMillis().toInt(),
                benachrichtigung
            )
        }
    }
}