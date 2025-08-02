package com.example.pulse.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.pulse.ui.theme.SurfaceGlass
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedCard

@Composable
fun ModernCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(16.dp),
    backgroundColor: Color = SurfaceGlass,
    borderColor: Color = Color.White.copy(alpha = 0.3f),
    borderWidth: Dp = 1.dp,
    blurRadius: Dp = 0.dp,
    elevation: Dp = 8.dp,
    isClickable: Boolean = onClick != null,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && isClickable) 0.96f else 1f,
        animationSpec = tween(durationMillis = 150)
    )

    val cardModifier = if (onClick != null) {
        modifier.clickable(
            interactionSource = interactionSource,
            indication = null
        ) { onClick() }
    } else {
        modifier
    }

    Box(
        modifier = cardModifier
            .scale(scale)
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        backgroundColor.copy(alpha = 0.9f),
                        backgroundColor.copy(alpha = 0.7f)
                    )
                )
            )
            .border(borderWidth, borderColor, shape)
            .then(
                if (blurRadius > 0.dp) {
                    Modifier.blur(blurRadius)
                } else {
                    Modifier
                }
            )
    ) {
        content()
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    ModernCard(
        modifier = modifier,
        onClick = onClick,
        backgroundColor = Color.White.copy(alpha = 0.15f),
        borderColor = Color.White.copy(alpha = 0.2f),
        blurRadius = 1.dp,
        content = content
    )
}

@Composable
fun ElevatedModernCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable () -> Unit
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            ),
        // FIX: Replaced Design.cornerRadius with a standard theme value
        shape = MaterialTheme.shapes.medium,
        // FIX: Replaced Design.elevation with a default elevation
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
    ) {
        content()
    }
}

@Composable
fun OutlinedModernCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    OutlinedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            ),
        // FIX: Replaced Design.cornerRadius with a standard theme value
        shape = MaterialTheme.shapes.medium,
        // FIX: Replaced Design.elevation with a default elevation
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
    ) {
        content()
    }
}
