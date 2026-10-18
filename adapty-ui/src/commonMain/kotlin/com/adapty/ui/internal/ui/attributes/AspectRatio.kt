@file:OptIn(InternalAdaptyUiApi::class)

package com.adapty.ui.internal.ui.attributes

import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import com.adapty.ui.internal.platform.InternalAdaptyUiApi

@InternalAdaptyUiApi
public enum class AspectRatio {
    FIT, FILL, STRETCH
}

internal fun AspectRatio.toComposeContentScale() =
    when (this) {
        AspectRatio.STRETCH -> ContentScale.FillBounds
        AspectRatio.FILL -> ContentScale.Crop
        else -> ContentScale.Fit
    }

internal fun AspectRatio.evaluateComposeImageAlignment(parentContentAlignment: Alignment) =
    when (this) {
        AspectRatio.FILL -> Alignment.TopCenter
        AspectRatio.FIT -> parentContentAlignment
        else -> Alignment.Center
    }