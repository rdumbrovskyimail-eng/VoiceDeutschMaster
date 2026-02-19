package com.voicedeutsch.master.voicecore.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.voicedeutsch.master.R
import com.voicedeutsch.master.app.MainActivity

/**
 * Foreground service that keeps the voice session alive when the app is backgrounded.
 *
 * Architecture lines 625-680 (lifecycle): the service is started by [VoiceCoreEngineImpl]
 * when a session begins and stopped when it ends. It holds a microphone foreground
 * service type so Android doesn't kill the audio pipeline while the user is speaking.
 *
 * Declared in AndroidManifest.xml as:
 * ```xml
 * <service
 *     android:name=".voicecore.service.VoiceSessionService"
 *     android:foregroundServiceType="microphone"
 *     android:exported="false" />
 * ```
 *
 * Usage from VoiceCoreEngineImpl (or a ViewModel):
 * ```kotlin
 * // Start
 * val intent = VoiceSessionService.startIntent(context)
 * ContextCompat.startForegroundService(context, intent)
 *
 * // Stop
 * val intent = VoiceSessionService.stopIntent(context)
 * context.startService(intent)
 * ```
 */
class VoiceSessionService : Service() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                START_NOT_STICKY
            }
            else -> {
                // ACTION_START or null (system restart)
                val notification = buildNotification()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE,
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
                START_STICKY
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        // AudioPipeline cleanup is handled by VoiceCoreEngineImpl.endSession(),
        // not here — the service is just a lifecycle holder.
    }

    // ── Notification ─────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Active voice learning session"
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val tapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val tapPending = PendingIntent.getActivity(
            this, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val stopIntent = stopIntent(this)
        val stopPending = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Голосовой урок активен")
            .setSmallIcon(R.drawable.ic_mic_notification)
            .setOngoing(true)
            .setContentIntent(tapPending)
            .addAction(
                R.drawable.ic_stop_notification,
                "Завершить",
                stopPending,
            )
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "voice_session_channel"
        private const val CHANNEL_NAME = "Voice Session"
        private const val NOTIFICATION_ID = 1001

        private const val ACTION_START = "com.voicedeutsch.master.action.START_SESSION"
        private const val ACTION_STOP = "com.voicedeutsch.master.action.STOP_SESSION"

        /** Intent to start the foreground service. Use with [ContextCompat.startForegroundService]. */
        fun startIntent(context: Context): Intent =
            Intent(context, VoiceSessionService::class.java).apply {
                action = ACTION_START
            }

        /** Intent to stop the foreground service. Use with [Context.startService]. */
        fun stopIntent(context: Context): Intent =
            Intent(context, VoiceSessionService::class.java).apply {
                action = ACTION_STOP
            }
    }
}
