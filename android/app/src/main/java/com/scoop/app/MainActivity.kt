package com.scoop.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.scoop.app.ui.navigation.ScoopNavHost
import com.scoop.app.ui.theme.ScoopTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val startUrl = intent.extractSharedUrl()

        setContent {
            ScoopTheme {
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
