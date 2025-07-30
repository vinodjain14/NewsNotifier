package com.example.newsnotifier.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Design system constants for consistent spacing, sizing, and other design tokens
 */
object DesignConstants {

    // Spacing Scale
    object Spacing {
        val xs = 4.dp
        val sm = 8.dp
        val md = 12.dp
        val lg = 16.dp
        val xl = 20.dp
        val xxl = 24.dp
        val xxxl = 32.dp
        val huge = 40.dp
        val massive = 48.dp
    }

    // Corner Radius Scale
    object CornerRadius {
        val xs = 4.dp
        val sm = 8.dp
        val md = 12.dp
        val lg = 16.dp
        val xl = 20.dp
        val xxl = 24.dp
        val round = 50.dp // For fully rounded elements
    }

    // Elevation Scale
    object Elevation {
        val none = 0.dp
        val sm = 2.dp
        val md = 4.dp
        val lg = 8.dp
        val xl = 12.dp
        val xxl = 16.dp
    }

    // Icon Sizes
    object IconSize {
        val xs = 16.dp
        val sm = 20.dp
        val md = 24.dp
        val lg = 32.dp
        val xl = 40.dp
        val xxl = 48.dp
        val huge = 64.dp
    }

    // Button Heights
    object ButtonHeight {
        val sm = 32.dp
        val md = 40.dp
        val lg = 48.dp
        val xl = 56.dp
        val xxl = 64.dp
    }

    // Card Dimensions
    object Card {
        val minHeight = 64.dp
        val padding = Spacing.lg
        val innerPadding = Spacing.md
        val borderWidth = 1.dp
    }

    // Animation Durations (in milliseconds)
    object Animation {
        const val fast = 150
        const val normal = 300
        const val slow = 500
        const val verySlow = 800
    }

    // Touch Targets
    object TouchTarget {
        val minimum = 48.dp
        val comfortable = 56.dp
        val large = 64.dp
    }

    // Layout Constants
    object Layout {
        val screenPadding = Spacing.lg
        val sectionSpacing = Spacing.xxl
        val itemSpacing = Spacing.md
        val maxContentWidth = 600.dp
    }

    // Typography Line Heights (multipliers)
    object LineHeight {
        const val tight = 1.2f
        const val normal = 1.4f
        const val relaxed = 1.6f
        const val loose = 1.8f
    }

    // Opacity Levels
    object Opacity {
        const val disabled = 0.38f
        const val medium = 0.6f
        const val high = 0.87f
        const val glassMorphism = 0.1f
        const val overlay = 0.5f
    }
}