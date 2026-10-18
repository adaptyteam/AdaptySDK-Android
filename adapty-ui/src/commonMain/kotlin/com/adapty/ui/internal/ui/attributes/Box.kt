@file:OptIn(InternalAdaptyUiApi::class)

package com.adapty.ui.internal.ui.attributes

import androidx.compose.runtime.Immutable
import com.adapty.ui.internal.platform.InternalAdaptyUiApi

@Immutable
internal data class Box(
    val width: DimUnit? = null,
    val height: DimUnit? = null
)
