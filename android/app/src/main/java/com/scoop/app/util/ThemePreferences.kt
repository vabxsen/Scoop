package com.scoop.app.util

import com.scoop.app.core.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Reactive front for the theme-related MMKV settings. [PreferenceUtil] itself is a dumb
 * synchronous wrapper, so without this StateFlow layer toggling a Settings switch would not
 * recompose [com.scoop.app.ui.theme.ScoopTheme] until the app restarted.
 */
class ThemePreferences {
    private val _themeMode =
        MutableStateFlow(
            ThemeMode.entries.firstOrNull { it.name == PreferenceUtil.getString(PrefKeys.THEME_MODE, ThemeMode.SYSTEM.name) }
                ?: ThemeMode.SYSTEM
        )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _dynamicColorEnabled = MutableStateFlow(PreferenceUtil.getBoolean(PrefKeys.DYNAMIC_COLOR, default = false))
    val dynamicColorEnabled: StateFlow<Boolean> = _dynamicColorEnabled.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        PreferenceUtil.putString(PrefKeys.THEME_MODE, mode.name)
        _themeMode.value = mode
    }

    fun setDynamicColorEnabled(enabled: Boolean) {
        PreferenceUtil.putBoolean(PrefKeys.DYNAMIC_COLOR, enabled)
        _dynamicColorEnabled.value = enabled
    }
}
