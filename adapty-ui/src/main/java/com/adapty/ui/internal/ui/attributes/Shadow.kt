package com.adapty.ui.internal.ui.attributes

import androidx.compose.runtime.Immutable

@Immutable
internal data class Shadow(
    val color: String?,
    val blurRadius: Float?,
    val offset: Offset?,
)
