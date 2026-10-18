@file:OptIn(InternalAdaptyUiApi::class)

package com.adapty.ui.internal.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.adapty.ui.internal.platform.InternalAdaptyUiApi

// Drawing seams.

/** Outer drop shadow of [outline]; [spread] is not supported on Android (callers pass 0). */
internal expect fun DrawScope.drawPlatformShadow(
    outline: Outline,
    color: Color,
    blurRadiusPx: Float,
    offset: Offset,
    spread: Float,
)

internal expect fun DrawScope.drawPlatformInnerShadow(
    outline: Outline,
    color: Color,
    blurRadiusPx: Float,
    offset: Offset,
)

/** Draws [image] transformed by a row-major 3x3 [matrix]. */
internal expect fun DrawScope.drawImageFill(image: ImageBitmap, matrix: FloatArray, alpha: Float)

/** Blurs the drawn content; [captureExpand] over-captures around the bounds (shadows). */
internal expect fun Modifier.platformBlur(radius: Dp, captureExpand: Dp = 0.dp): Modifier
