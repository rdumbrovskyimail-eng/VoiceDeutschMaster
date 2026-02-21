package com.voicedeutsch.master.app.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.voicedeutsch.master.R
import com.voicedeutsch.master.app.MainActivity

/**
 * Sends a daily study reminder notification.
 * Architecture lines 584-592 (WorkManager — Напоминания о занятиях).
 */
class ReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        ensureNotificationChannel()
        showReminderNotification()
        return Result.success()
    }

    private fun ensureNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Напоминания об обучении",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Ежедневные напоминания о занятиях немецким"
        }
        val nm = applicationContext.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }

    private fun showReminderNotification() {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_mic_notification)
            .setContentTitle("Zeit zum Üben! 🇩🇪")
            .setContentText("Пора заниматься немецким! У тебя есть слова для повторения.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        if (ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        const val WORK_NAME = "study_reminder"
        const val CHANNEL_ID = "study_reminders"
        const val NOTIFICATION_ID = 1001
    }
}