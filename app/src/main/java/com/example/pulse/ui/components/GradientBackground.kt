package com.example.pulse.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.pulse.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GradientBackground(
    modifier: Modifier = Modifier,
    colors: List<Color> = listOf(
        GradientStart,
        GradientMiddle,
        GradientEnd
    ),
    isAnimated: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition()
    val density = LocalDensity.current

    val animatedOffset1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isAnimated) 1f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000),
            repeatMode = RepeatMode.Reverse
        )
    )

    val animatedOffset2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isAnimated) 1f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = modifier
            .background(
                brush = Brush.radialGradient(
                    colors = colors,
                    radius = with(density) { 800.dp.toPx() },
                    center = androidx.compose.ui.geometry.Offset(
                        x = 0.3f + 0.4f * sin(animatedOffset1 * 2 * Math.PI).toFloat(),
                        y = 0.3f + 0.4f * cos(animatedOffset2 * 2 * Math.PI).toFloat()
                    ),
                    tileMode = TileMode.Clamp
                )
            )
    ) {
        content()
    }
}

@Composable
fun StaticGradientBackground(
    modifier: Modifier = Modifier,
    colors: List<Color> = listOf(
        GradientStart,
        GradientEnd
    ),
    direction: GradientDirection = GradientDirection.TopToBottom,
    content: @Composable BoxScope.() -> Unit
) {
    val brush = when (direction) {
        GradientDirection.TopToBottom -> Brush.verticalGradient(colors)
        GradientDirection.LeftToRight -> Brush.horizontalGradient(colors)
        GradientDirection.TopLeftToBottomRight -> Brush.linearGradient(
            colors = colors,
            start = androidx.compose.ui.geometry.Offset.Zero,
            end = androidx.compose.ui.geometry.Offset.Infinite
        )
    }

    Box(
        modifier = modifier.background(brush)
    ) {
        content()
    }
}

enum class GradientDirection {
    TopToBottom,
    LeftToRight,
    TopLeftToBottomRight
}

@Composable
fun FloatingElements(
    modifier: Modifier = Modifier,
    elementCount: Int = 3
) {
    val infiniteTransition = rememberInfiniteTransition()

    repeat(elementCount) { index ->
        val animatedY by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 100f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 3000 + index * 1000),
                repeatMode = RepeatMode.Reverse
            )
        )

        val animatedX by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 50f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 4000 + index * 800),
                repeatMode = RepeatMode.Reverse
            )
        )

        Box(
            modifier = Modifier
                .offset(
                    x = (50 + index * 100).dp + animatedX.dp,
                    y = (50 + index * 80).dp + animatedY.dp
                )
                .size((20 + index * 10).dp)
                .alpha(0.1f)
                .background(
                    color = Color.White,
                    shape = androidx.compose.foundation.shape.CircleShape
                )
        )
    }
}