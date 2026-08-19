@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.ui.element

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.ui.attributes.Transition
import com.adapty.ui.internal.ui.attributes.easing

@Composable
internal fun <T> AdaptiveContentTransition(
    targetVariant: T,
    transition: Transition,
    content: @Composable (T) -> Unit,
) {
    val duration = transition.durationMillis.toInt()
    val easing = transition.easing
    AnimatedContent(
        targetState = targetVariant,
        transitionSpec = {
            ContentTransform(
                targetContentEnter = EnterTransition.None,
                initialContentExit = ExitTransition.None,
                sizeTransform = SizeTransform(clip = false) { _, _ -> tween(duration, easing = easing) },
            )
        },
        label = "AdaptiveContentTransition",
    ) { variant ->
        val variantAlpha by this.transition.animateFloat(
            transitionSpec = { tween(duration, easing = easing) },
            label = "AdaptiveContentAlpha",
        ) { state -> if (state == EnterExitState.Visible) 1f else 0f }
        Box(
            Modifier.graphicsLayer {
                alpha = variantAlpha
                compositingStrategy = CompositingStrategy.ModulateAlpha
            },
            propagateMinConstraints = true,
        ) {
            content(variant)
        }
    }
}
