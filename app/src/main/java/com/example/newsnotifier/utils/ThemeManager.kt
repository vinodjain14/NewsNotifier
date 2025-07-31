// Create: app/src/main/java/com/example/newsnotifier/utils/ThemeManager.kt
package com.example.newsnotifier.utils

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages theme preferences and settings
 */
class ThemeManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)

    // Theme mode flow
    private val _themeModeFlow = MutableStateFlow(getThemeMode())
    val themeModeFlow: StateFlow<ThemeMode> = _themeModeFlow.asStateFlow()

    // Font size flow
    private val _fontSizeFlow = MutableStateFlow(getFontSize())
    val fontSizeFlow: StateFlow<FontSize> = _fontSizeFlow.asStateFlow()

    // High contrast mode flow
    private val _highContrastFlow = MutableStateFlow(getHighContrast())
    val highContrastFlow: StateFlow<Boolean> = _highContrastFlow.asStateFlow()

    // Dynamic colors flow (Android 12+)
    private val _dynamicColorsFlow = MutableStateFlow(getDynamicColors())
    val dynamicColorsFlow: StateFlow<Boolean> = _dynamicColorsFlow.asStateFlow()

    companion object {
        private const val THEME_MODE_KEY = "theme_mode"
        private const val FONT_SIZE_KEY = "font_size"
        private const val HIGH_CONTRAST_KEY = "high_contrast"
        private const val DYNAMIC_COLORS_KEY = "dynamic_colors"
    }

    private fun getThemeMode(): ThemeMode {
        val modeString = prefs.getString(THEME_MODE_KEY, ThemeMode.SYSTEM.name)
        return try {
            ThemeMode.valueOf(modeString ?: ThemeMode.SYSTEM.name)
        } catch (e: Exception) {
            ThemeMode.SYSTEM
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(THEME_MODE_KEY, mode.name).apply()
        _themeModeFlow.value = mode
    }

    private fun getFontSize(): FontSize {
        val sizeString = prefs.getString(FONT_SIZE_KEY, FontSize.MEDIUM.name)
        return try {
            FontSize.valueOf(sizeString ?: FontSize.MEDIUM.name)
        } catch (e: Exception) {
            FontSize.MEDIUM
        }
    }

    fun setFontSize(size: FontSize) {
        prefs.edit().putString(FONT_SIZE_KEY, size.name).apply()
        _fontSizeFlow.value = size
    }

    private fun getHighContrast(): Boolean {
        return prefs.getBoolean(HIGH_CONTRAST_KEY, false)
    }

    fun setHighContrast(enabled: Boolean) {
        prefs.edit().putBoolean(HIGH_CONTRAST_KEY, enabled).apply()
        _highContrastFlow.value = enabled
    }

    private fun getDynamicColors(): Boolean {
        return prefs.getBoolean(DYNAMIC_COLORS_KEY, true)
    }

    fun setDynamicColors(enabled: Boolean) {
        prefs.edit().putBoolean(DYNAMIC_COLORS_KEY, enabled).apply()
        _dynamicColorsFlow.value = enabled
    }
}

/**
 * Theme mode options
 */
enum class ThemeMode(val displayName: String) {
    LIGHT("Light"),
    DARK("Dark"),
    SYSTEM("Follow System")
}

/**
 * Font size options for accessibility
 */
enum class FontSize(val displayName: String, val scale: Float) {
    SMALL("Small", 0.85f),
    MEDIUM("Medium", 1.0f),
    LARGE("Large", 1.15f),
    EXTRA_LARGE("Extra Large", 1.3f),
    HUGE("Huge", 1.5f)
}