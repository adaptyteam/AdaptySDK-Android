package com.adapty.ui.internal.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer

@Composable
internal fun Modifier.buttonPressDim(interactionSource: InteractionSource): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val alpha by animateFloatAsState(
        targetValue = if (pressed) PRESSED_CONTENT_ALPHA else 1f,
        animationSpec = tween(
            durationMillis = if (pressed) PRESS_FADE_IN_MS else PRESS_FADE_OUT_MS,
            easing = LinearEasing,
        ),
        label = "buttonPressAlpha",
    )
    return graphicsLayer {
        this.alpha = alpha
        compositingStrategy = CompositingStrategy.ModulateAlpha
    }
}

private const val PRESSED_CONTENT_ALPHA = 0.83f
private const val PRESS_FADE_IN_MS = 0
private const val PRESS_FADE_OUT_MS = 100
