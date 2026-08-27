package com.scoop.app.ui.screen.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.GetApp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.scoop.app.R
import com.scoop.app.ui.common.SettingHubRow
import com.scoop.app.ui.common.SettingsScreenTitle
import com.scoop.app.util.PermissionUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPermissionsScreen(onBack: () -> Unit) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val context = LocalContext.current

    var notificationsEnabled by remember { mutableStateOf(PermissionUtils.notificationsEnabled(context)) }
    var canInstallUnknownApps by remember { mutableStateOf(PermissionUtils.canInstallUnknownApps(context)) }

    // Android never reports a permission change back to us directly - re-check whenever this
    // screen (re)gains the foreground, since that's the only moment the user could have flipped
    // one via the runtime dialog or the system settings screens we deep-link to below.
    LifecycleResumeEffect(Unit) {
        notificationsEnabled = PermissionUtils.notificationsEnabled(context)
        canInstallUnknownApps = PermissionUtils.canInstallUnknownApps(context)
        onPauseOrDispose { }
    }

    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> notificationsEnabled = granted }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { SettingsScreenTitle(stringResource(R.string.settings_permissions_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back)) }
                },
                scrollBehavior = scrollBehavior,
            )
        }
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.padding(innerPadding)) {
            item {
                SettingHubRow(
                    title = stringResource(R.string.permission_notifications_title),
                    subtitle = stringResource(R.string.permission_notifications_subtitle),
                    leadingIcon = Icons.Filled.Notifications,
                    onClick = {
                        when {
                            notificationsEnabled -> context.startActivity(PermissionUtils.appNotificationSettingsIntent(context))
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            else -> context.startActivity(PermissionUtils.appNotificationSettingsIntent(context))
                        }
                    },
                    trailingContent = { Switch(checked = notificationsEnabled, onCheckedChange = null) },
                )
            }
            item {
                SettingHubRow(
                    title = stringResource(R.string.permission_install_unknown_apps_title),
                    subtitle = stringResource(R.string.permission_install_unknown_apps_subtitle),
                    leadingIcon = Icons.Filled.GetApp,
                    onClick = { context.startActivity(PermissionUtils.installUnknownAppsSettingsIntent(context)) },
                    trailingContent = { Switch(checked = canInstallUnknownApps, onCheckedChange = null) },
                )
            }
            item {
                SettingHubRow(
                    title = stringResource(R.string.permission_network_title),
                    subtitle = stringResource(R.string.permission_network_subtitle),
                    leadingIcon = Icons.Filled.Public,
                    trailingContent = { Switch(checked = true, onCheckedChange = null, enabled = false) },
                )
            }
            item {
                SettingHubRow(
                    title = stringResource(R.string.permission_background_downloads_title),
                    subtitle = stringResource(R.string.permission_background_downloads_subtitle),
                    leadingIcon = Icons.Filled.CloudDownload,
                    trailingContent = { Switch(checked = true, onCheckedChange = null, enabled = false) },
                )
            }
        }
    }
}
