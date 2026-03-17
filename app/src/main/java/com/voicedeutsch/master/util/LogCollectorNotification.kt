package com.voicedeutsch.master.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

/**
 * 📢 Уведомления для LogCollectorService.
 *
 * Foreground Service на Android 8+ требует уведомление.
 * Это минимальное "тихое" уведомление с низким приоритетом.
 */
object LogCollectorNotification {

    const val NOTIFICATION_ID = 9901
    private const val CHANNEL_ID = "log_collector_channel"
    private const val CHANNEL_NAME = "Log Collector"

    /**
     * Создаёт канал уведомлений (идемпотентно — повторный вызов ничего не ломает).
     */
    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Фоновый сбор логов для диагностики"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    /**
     * Обычное уведомление — сервис работает, всё нормально.
     */
    fun create(context: Context): Notification {
        ensureChannel(context)
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Сбор логов")
            .setContentText("Диагностика активна (60 сек)")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    /**
     * Уведомление после обнаружения краша основного процесса.
     */
    fun createCrashDetected(context: Context): Notification {
        ensureChannel(context)
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("🔥 Краш обнаружен")
            .setContentText("Дособираем логи и сохраняем...")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }
}
