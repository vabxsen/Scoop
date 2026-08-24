package com.scoop.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import com.scoop.app.ui.navigation.ScoopNavHost
import com.scoop.app.ui.theme.ScoopTheme
import com.scoop.app.util.ThemePreferences
import org.koin.compose.koinInject

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val startUrl = intent.extractSharedUrl()

        setContent {
            val themePreferences = koinInject<ThemePreferences>()
            val themeMode by themePreferences.themeMode.collectAsState()
            val accentPalette by themePreferences.accentPalette.collectAsState()
            val dynamicColorEnabled by themePreferences.dynamicColorEnabled.collectAsState()

            ScoopTheme(themeMode = themeMode, accentPalette = accentPalette, useDynamicColor = dynamicColorEnabled) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    ScoopNavHost(startUrl = startUrl)
                }
            }
        }
    }

    private fun Intent.extractSharedUrl(): String? =
        when (action) {
            Intent.ACTION_SEND -> getStringExtra(Intent.EXTRA_TEXT)
            Intent.ACTION_VIEW -> data?.toString()
            else -> null
        }
}
