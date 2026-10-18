@file:OptIn(InternalAdaptyUiApi::class)

package com.adapty.ui.internal.ui.element

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.adapty.ui.internal.ui.attributes.ComposeFill
import com.adapty.ui.internal.platform.InternalAdaptyUiApi

/**
 * Radial progress paths the shared renderer cannot draw with plain DrawScope arcs:
 * a stroked arc ([strokeWidth] != null) filled with [imageFill] or [brush], or a pie ([strokeWidth] == null) filled with [imageFill].
 */
internal expect fun DrawScope.drawRadialProgress(
    topLeft: Offset,
    diameter: Float,
    startAngle: Float,
    sweepAngle: Float,
    strokeWidth: Float?,
    roundCaps: Boolean,
    brush: Brush?,
    imageFill: ComposeFill.Image?,
)
