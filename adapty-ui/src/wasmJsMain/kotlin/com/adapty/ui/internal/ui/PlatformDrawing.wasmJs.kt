package com.adapty.ui.internal.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import com.adapty.ui.internal.platform.webStub

internal actual fun DrawScope.drawPlatformShadow(
    outline: Outline,
    color: Color,
    blurRadiusPx: Float,
    offset: Offset,
    spread: Float,
) {
    webStub("drawPlatformShadow")
}

internal actual fun DrawScope.drawPlatformInnerShadow(
    outline: Outline,
    color: Color,
    blurRadiusPx: Float,
    offset: Offset,
) {
    webStub("drawPlatformInnerShadow")
}

internal actual fun DrawScope.drawImageFill(image: ImageBitmap, matrix: FloatArray, alpha: Float) {
    webStub("drawImageFill")
}

internal actual fun Modifier.platformBlur(radius: Dp, captureExpand: Dp): Modifier {
    webStub("platformBlur")
    return this
}
