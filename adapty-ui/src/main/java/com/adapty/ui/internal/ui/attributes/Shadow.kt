@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.ui.attributes

import androidx.compose.runtime.Immutable
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.utils.VisualValue

@Immutable
internal data class Shadow(
    val color: VisualValue?,
    val blurRadius: Float?,
    val offset: Offset?,
)
