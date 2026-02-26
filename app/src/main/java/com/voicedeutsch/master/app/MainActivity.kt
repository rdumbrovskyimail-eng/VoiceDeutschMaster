package com.voicedeutsch.master.app

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.voicedeutsch.master.presentation.navigation.AppNavigation
import com.voicedeutsch.master.presentation.theme.VoiceDeutschMasterTheme
import com.voicedeutsch.master.util.CrashLogger
import org.koin.android.ext.android.inject
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val dataStore: UserPreferencesDataStore by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkForRecentCrashes()
        enableEdgeToEdge()

        setContent {
            // Исправлено: VoiceDeutschMasterTheme не принимает darkTheme —
            // тема всегда светлая, isDarkTheme и getThemeFlow() не используются.
            VoiceDeutschMasterTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation()
                }
            }
        }
    }

    private fun checkForRecentCrashes() {
        try {
            val crashLogger = CrashLogger.getInstance() ?: return
            val latestCrash = crashLogger.getLatestCrashLog() ?: return

            val fiveMinutesAgo = System.currentTimeMillis() - (5 * 60 * 1000)
            if (latestCrash.lastModified() > fiveMinutesAgo) {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

                Log.w(TAG, "━".repeat(80))
                Log.w(TAG, "🔥 RECENT CRASH DETECTED!")
                Log.w(TAG, "━".repeat(80))
                Log.w(TAG, "📁 Location: ${latestCrash.absolutePath}")
                Log.w(TAG, "📊 Size: ${latestCrash.length() / 1024} KB")
                Log.w(TAG, "🕐 Time: ${dateFormat.format(latestCrash.lastModified())}")
                Log.w(TAG, "━".repeat(80))

                Log.i(TAG, "📋 First 50 lines of crash log:")
                latestCrash.readLines().take(50).forEach { line ->
                    Log.i(TAG, line)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for crashes", e)
        }
    }

    private companion object {
        const val TAG = "MainActivity"
    }
}