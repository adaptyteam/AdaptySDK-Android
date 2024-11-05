package com.adapty.ui.internal.ui.attributes

import androidx.compose.runtime.Stable

@Stable
internal data class Offset(
    val y: Float,
    val x: Float,
) {
    constructor(value: Float): this(value, value)

    @Transient
    var consumed: Boolean = false
}