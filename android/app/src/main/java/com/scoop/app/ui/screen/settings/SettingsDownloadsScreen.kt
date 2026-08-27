package com.scoop.app.ui.screen.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.scoop.app.R
import com.scoop.app.core.model.AutoRetryPolicy
import com.scoop.app.core.model.BatteryPauseThreshold
import com.scoop.app.core.model.DownloadSpeedLimit
import com.scoop.app.core.model.HistoryRetention
import com.scoop.app.ui.common.SettingHubRow
import com.scoop.app.ui.common.SettingRadioSheet
import com.scoop.app.ui.common.SettingsScreenTitle
import org.koin.androidx.compose.koinViewModel

private enum class DownloadsActiveSheet { NONE, AUTO_RETRY, SPEED_LIMIT, BATTERY, RETENTION }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDownloadsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = koinViewModel()) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    var activeSheet by remember { mutableStateOf(DownloadsActiveSheet.NONE) }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { SettingsScreenTitle(stringResource(R.string.settings_downloads_title)) },
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
                    title = stringResource(R.string.settings_wifi_only_downloads),
                    subtitle = stringResource(R.string.settings_wifi_only_downloads_subtitle),
                    leadingIcon = Icons.Filled.Wifi,
                    onClick = { viewModel.updateWifiOnlyDownloads(!viewModel.wifiOnlyDownloads) },
                    trailingContent = { Switch(checked = viewModel.wifiOnlyDownloads, onCheckedChange = null) },
                )
            }
            item {
                SettingHubRow(
                    title = stringResource(R.string.settings_auto_retry),
                    subtitle = viewModel.autoRetryPolicy.label,
                    leadingIcon = Icons.Filled.Replay,
                    onClick = { activeSheet = DownloadsActiveSheet.AUTO_RETRY },
                )
            }
            item {
                SettingHubRow(
                    title = stringResource(R.string.settings_speed_limit),
                    subtitle = viewModel.downloadSpeedLimit.label,
                    leadingIcon = Icons.Filled.Speed,
                    onClick = { activeSheet = DownloadsActiveSheet.SPEED_LIMIT },
                )
            }
            item {
                SettingHubRow(
                    title = stringResource(R.string.settings_battery_pause),
                    subtitle = viewModel.batteryPauseThreshold.label,
                    leadingIcon = Icons.Filled.BatteryAlert,
                    onClick = { activeSheet = DownloadsActiveSheet.BATTERY },
                )
            }
            item {
                SettingHubRow(
                    title = stringResource(R.string.settings_history_retention),
                    subtitle = viewModel.historyRetention.label,
                    leadingIcon = Icons.Filled.DeleteSweep,
                    onClick = { activeSheet = DownloadsActiveSheet.RETENTION },
                )
            }
        }
    }

    when (activeSheet) {
        DownloadsActiveSheet.AUTO_RETRY ->
            SettingRadioSheet(
                title = stringResource(R.string.settings_auto_retry),
                options = AutoRetryPolicy.entries,
                selected = viewModel.autoRetryPolicy,
                optionLabel = { it.label },
                onSelect = viewModel::updateAutoRetryPolicy,
                onDismiss = { activeSheet = DownloadsActiveSheet.NONE },
            )
        DownloadsActiveSheet.SPEED_LIMIT ->
            SettingRadioSheet(
                title = stringResource(R.string.settings_speed_limit),
                options = DownloadSpeedLimit.entries,
                selected = viewModel.downloadSpeedLimit,
                optionLabel = { it.label },
                onSelect = viewModel::updateDownloadSpeedLimit,
                onDismiss = { activeSheet = DownloadsActiveSheet.NONE },
            )
        DownloadsActiveSheet.BATTERY ->
            SettingRadioSheet(
                title = stringResource(R.string.settings_battery_pause),
                options = BatteryPauseThreshold.entries,
                selected = viewModel.batteryPauseThreshold,
                optionLabel = { it.label },
                onSelect = viewModel::updateBatteryPauseThreshold,
                onDismiss = { activeSheet = DownloadsActiveSheet.NONE },
            )
        DownloadsActiveSheet.RETENTION ->
            SettingRadioSheet(
                title = stringResource(R.string.settings_history_retention),
                options = HistoryRetention.entries,
                selected = viewModel.historyRetention,
                optionLabel = { it.label },
                onSelect = viewModel::updateHistoryRetention,
                onDismiss = { activeSheet = DownloadsActiveSheet.NONE },
            )
        DownloadsActiveSheet.NONE -> Unit
    }
}
