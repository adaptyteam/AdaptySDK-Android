@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.ui.attributes

import androidx.compose.runtime.Immutable
import com.adapty.internal.utils.InternalAdaptyApi

@Immutable
internal data class Box(
    val width: DimUnit? = null,
    val height: DimUnit? = null
)
