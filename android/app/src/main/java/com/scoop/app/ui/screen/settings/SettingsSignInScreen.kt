package com.scoop.app.ui.screen.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.scoop.app.R
import com.scoop.app.extractor.InstagramSession
import com.scoop.app.ui.common.SettingHubRow
import com.scoop.app.ui.common.SettingsScreenTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSignInScreen(
    onBack: () -> Unit,
    onOpenInstagram: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    var instagramConnected by remember { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        instagramConnected = InstagramSession.hasSession()
        onPauseOrDispose { }
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { SettingsScreenTitle(stringResource(R.string.settings_sign_in_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.padding(innerPadding)) {
            item {
                SettingHubRow(
                    title = stringResource(R.string.instagram_title),
                    subtitle = stringResource(if (instagramConnected) R.string.instagram_session_saved else R.string.instagram_settings_subtitle),
                    leadingIcon = Icons.Filled.AccountCircle,
                    onClick = onOpenInstagram,
                )
            }
        }
    }
}
