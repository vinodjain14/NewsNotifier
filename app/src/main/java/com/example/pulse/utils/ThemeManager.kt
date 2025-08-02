package com.example.pulse.utils

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ThemeManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)

    private val _themeModeFlow = MutableStateFlow(getThemeMode())
    val themeModeFlow: StateFlow<ThemeMode> = _themeModeFlow.asStateFlow()

    private val _fontSizeFlow = MutableStateFlow(getFontSize())
    val fontSizeFlow: StateFlow<FontSize> = _fontSizeFlow.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString("theme_mode", mode.name).apply()
        _themeModeFlow.value = mode
    }

    fun setFontSize(size: FontSize) {
        prefs.edit().putString("font_size", size.name).apply()
        _fontSizeFlow.value = size
    }

    private fun getThemeMode(): ThemeMode {
        val modeString = prefs.getString("theme_mode", ThemeMode.SYSTEM.name)
        return try {
            ThemeMode.valueOf(modeString ?: ThemeMode.SYSTEM.name)
        } catch (e: Exception) {
            ThemeMode.SYSTEM
        }
    }

    private fun getFontSize(): FontSize {
        val sizeString = prefs.getString("font_size", FontSize.MEDIUM.name)
        return try {
            FontSize.valueOf(sizeString ?: FontSize.MEDIUM.name)
        } catch (e: Exception) {
            FontSize.MEDIUM
        }
    }

    init {
        _themeModeFlow.value = getThemeMode()
        _fontSizeFlow.value = getFontSize()
    }
}

enum class ThemeMode(val displayName: String) {
    LIGHT("Light"),
    DARK("Dark"),
    SYSTEM("Follow System")
}

enum class FontSize(val displayName: String, val scale: Float) {
    SMALL("Small", 0.85f),
    MEDIUM("Medium", 1.0f),
    LARGE("Large", 1.15f),
    EXTRA_LARGE("Extra Large", 1.3f),
    HUGE("Huge", 1.5f)
}