package com.voicedeutsch.master.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.voicedeutsch.master.presentation.navigation.AppNavigation
import com.voicedeutsch.master.presentation.theme.VoiceDeutschMasterTheme

/**
 * Single Activity — the only Android Activity in the application.
 *
 * Sets up:
 *  - Edge-to-edge rendering
 *  - Material 3 theme via [VoiceDeutschMasterTheme]
 *  - Full [AppNavigation] NavHost as the root Composable
 *
 * All navigation, back-stack management and screen transitions are handled
 * inside [AppNavigation]. This class deliberately has no business logic.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Extend content behind system bars (status bar + navigation bar).
        enableEdgeToEdge()

        setContent {
            VoiceDeutschMasterTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    AppNavigation()
                }
            }
        }
    }
}