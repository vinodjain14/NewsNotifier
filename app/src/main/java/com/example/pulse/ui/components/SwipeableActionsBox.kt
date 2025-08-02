package com.example.pulse.ui.components

import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class SwipeState {
    Open,
    Closed
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SwipeableActionsBox(
    modifier: Modifier = Modifier,
    actions: @Composable BoxScope.() -> Unit,
    swipeThreshold: Dp = 80.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    val state = remember {
        AnchoredDraggableState(
            initialValue = SwipeState.Closed,
            positionalThreshold = { distance: Float -> distance * 0.5f },
            velocityThreshold = { with(density) { 100.dp.toPx() } },
            animationSpec = tween(),
        ).apply {
            updateAnchors(
                DraggableAnchors {
                    SwipeState.Closed at 0f
                    SwipeState.Open at with(density) { -swipeThreshold.toPx() }
                }
            )
        }
    }

    Box(
        modifier = modifier
            .anchoredDraggable(
                state = state,
                orientation = Orientation.Horizontal
            )
    ) {
        Box(
            modifier = Modifier.matchParentSize(),
            content = actions
        )

        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = state
                            .requireOffset()
                            .roundToInt(),
                        y = 0
                    )
                }
                .clickable(
                    onClick = {
                        if (state.currentValue == SwipeState.Open) {
                            scope.launch {
                                // FIX: 'animateTo' is incorrect. 'settle' is the correct function.
                                state.settle(0f)
                            }
                        }
                    },
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                )
        ) {
            content()
        }
    }
}