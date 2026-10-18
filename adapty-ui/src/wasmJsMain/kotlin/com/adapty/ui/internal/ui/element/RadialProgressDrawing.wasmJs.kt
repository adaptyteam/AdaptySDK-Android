package com.adapty.ui.internal.ui.element

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.adapty.ui.internal.platform.webStub
import com.adapty.ui.internal.ui.attributes.ComposeFill

internal actual fun DrawScope.drawRadialProgress(
    topLeft: Offset,
    diameter: Float,
    startAngle: Float,
    sweepAngle: Float,
    strokeWidth: Float?,
    roundCaps: Boolean,
    brush: Brush?,
    imageFill: ComposeFill.Image?,
) {
    webStub("drawRadialProgress")
}
