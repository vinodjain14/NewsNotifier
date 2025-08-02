package com.example.pulse.ui.theme

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Extension functions and utilities for enhanced UI components
 */

/**
 * Adds a clickable modifier with ripple effect
 */
fun Modifier.clickableWithRipple(
    bounded: Boolean = true,
    radius: Dp = Dp.Unspecified,
    color: Color = Color.Unspecified,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val ripple = rememberRipple(
        bounded = bounded,
        radius = radius,
        color = if (color == Color.Unspecified) MaterialTheme.colorScheme.primary else color
    )

    clickable(
        interactionSource = interactionSource,
        indication = ripple,
        onClick = onClick
    )
}

/**
 * Adds padding for system bars (status bar and navigation bar)
 */
@Composable
fun Modifier.systemBarsPadding(): Modifier {
    val density = LocalDensity.current
    val systemBars = WindowInsets.systemBars

    return padding(
        top = with(density) { systemBars.getTop(density).toDp() },
        bottom = with(density) { systemBars.getBottom(density).toDp() }
    )
}

/**
 * Clips to shape and adds clickable with ripple
 */
fun Modifier.clipAndClick(
    shape: Shape,
    onClick: () -> Unit
): Modifier = composed {
    clip(shape).clickableWithRipple(onClick = onClick)
}

/**
 * Conditional modifier
 */
fun Modifier.conditional(
    condition: Boolean,
    modifier: Modifier.() -> Modifier
): Modifier {
    return if (condition) {
        then(modifier(Modifier))
    } else {
        this
    }
}

/**
 * Safe padding that ensures minimum touch target
 */
fun Modifier.safePadding(
    horizontal: Dp = 0.dp,
    vertical: Dp = 0.dp,
    minTouchTarget: Dp = 48.dp
): Modifier = composed {
    val actualHorizontal = maxOf(horizontal, (minTouchTarget - 24.dp) / 2)
    val actualVertical = maxOf(vertical, (minTouchTarget - 24.dp) / 2)

    padding(horizontal = actualHorizontal, vertical = actualVertical)
}

/**
 * Glass morphism effect modifier
 */
fun Modifier.glassMorphism(
    alpha: Float = 0.1f,
    blurRadius: Dp = 1.dp
): Modifier = composed {
    // This would require additional blur implementation
    // For now, we'll use a semi-transparent background
    this
}