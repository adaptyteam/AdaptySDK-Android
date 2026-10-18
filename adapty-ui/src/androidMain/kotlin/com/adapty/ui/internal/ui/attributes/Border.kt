@file:OptIn(InternalAdaptyUiApi::class)

package com.adapty.ui.internal.ui.attributes

import androidx.compose.runtime.Immutable
import com.adapty.ui.internal.platform.InternalAdaptyUiApi
import com.adapty.ui.internal.utils.VisualValue

@Immutable
internal data class Border(
    val color: VisualValue? = null,
    val thickness: Float? = null
)
