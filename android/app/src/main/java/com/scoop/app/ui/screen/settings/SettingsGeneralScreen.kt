package com.scoop.app.ui.screen.settings

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.scoop.app.R
import com.scoop.app.core.model.AccentPalette
import com.scoop.app.core.model.ThemeMode
import com.scoop.app.ui.common.AccentPaletteRow
import com.scoop.app.ui.common.SettingHubRow
import com.scoop.app.ui.common.SettingRadioSheet
import com.scoop.app.ui.common.SettingSectionLabel
import com.scoop.app.ui.common.ThemePreviewCard
import com.scoop.app.ui.theme.Spacing
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsGeneralScreen(onBack: () -> Unit, viewModel: SettingsViewModel = koinViewModel()) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val themeMode by viewModel.themeMode.collectAsState()
    val accentPalette by viewModel.accentPalette.collectAsState()
    val dynamicColorEnabled by viewModel.dynamicColorEnabled.collectAsState()
    var showThemeSheet by remember { mutableStateOf(false) }
    var showLanguageSheet by remember { mutableStateOf(false) }

    val systemDark = isSystemInDarkTheme()
    val isDarkResolved =
        when (themeMode) {
            ThemeMode.SYSTEM -> systemDark
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.settings_general_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back)) }
                },
                scrollBehavior = scrollBehavior,
            )
        }
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.padding(innerPadding)) {
            item {
                ThemePreviewCard(
                    sampleTitle = stringResource(R.string.theme_preview_title),
                    sampleSubtitle = stringResource(R.string.theme_preview_subtitle),
                    sampleBadge = stringResource(R.string.theme_preview_badge),
                    progress = 0.68f,
                    modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.md),
                )
            }
            item {
                SettingSectionLabel(stringResource(R.string.settings_accent_palette), modifier = Modifier.padding(horizontal = Spacing.md))
            }
            item {
                AccentPaletteRow(
                    selected = accentPalette,
                    onSelect = viewModel::setAccentPalette,
                    label = { it.label() },
                    modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
                )
            }
            item {
                SettingHubRow(
                    title = stringResource(R.string.settings_dynamic_color),
                    subtitle = stringResource(R.string.settings_dynamic_color_subtitle),
                    leadingIcon = Icons.Filled.Contrast,
                    onClick = { viewModel.setDynamicColorEnabled(!dynamicColorEnabled) },
                    trailingContent = { Switch(checked = dynamicColorEnabled, onCheckedChange = null) },
                )
            }
            item {
                SettingHubRow(
                    title = stringResource(R.string.settings_dark_theme),
                    subtitle = themeMode.label(),
                    leadingIcon = Icons.Filled.DarkMode,
                    onClick = { showThemeSheet = true },
                    trailingContent = {
                        Switch(
                            checked = isDarkResolved,
                            onCheckedChange = { checked -> viewModel.setThemeMode(if (checked) ThemeMode.DARK else ThemeMode.LIGHT) },
                        )
                    },
                )
            }
            item {
                SettingHubRow(
                    title = stringResource(R.string.settings_display_language),
                    subtitle = stringResource(R.string.display_language_english),
                    leadingIcon = Icons.Filled.Language,
                    onClick = { showLanguageSheet = true },
                )
            }
        }
    }

    if (showThemeSheet) {
        SettingRadioSheet(
            title = stringResource(R.string.settings_theme),
            options = ThemeMode.entries,
            selected = themeMode,
            optionLabel = { it.label() },
            onSelect = viewModel::setThemeMode,
            onDismiss = { showThemeSheet = false },
        )
    }

    if (showLanguageSheet) {
        val english = stringResource(R.string.display_language_english)
        SettingRadioSheet(
            title = stringResource(R.string.settings_display_language),
            options = listOf(english),
            selected = english,
            optionLabel = { it },
            onSelect = {},
            onDismiss = { showLanguageSheet = false },
        )
    }
}

@Composable
private fun ThemeMode.label(): String =
    when (this) {
        ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
        ThemeMode.LIGHT -> stringResource(R.string.theme_light)
        ThemeMode.DARK -> stringResource(R.string.theme_dark)
    }

@Composable
private fun AccentPalette.label(): String =
    when (this) {
        AccentPalette.MONOCHROME -> stringResource(R.string.accent_palette_monochrome)
        AccentPalette.OCEAN -> stringResource(R.string.accent_palette_ocean)
        AccentPalette.FOREST -> stringResource(R.string.accent_palette_forest)
        AccentPalette.SUNSET -> stringResource(R.string.accent_palette_sunset)
        AccentPalette.LAVENDER -> stringResource(R.string.accent_palette_lavender)
    }
