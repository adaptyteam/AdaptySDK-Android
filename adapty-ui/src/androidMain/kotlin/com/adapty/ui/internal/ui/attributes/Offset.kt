@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.ui.attributes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import com.adapty.internal.utils.InternalAdaptyApi

internal data class Offset(
    val y: DimUnit,
    val x: DimUnit,
) {
    constructor(value: DimUnit): this(value, DimUnit.Zero)

    @Transient
    var consumed: Boolean = false

    companion object {
        val Default = Offset(DimUnit.Zero)
    }
}

@Immutable
internal data class DpOffset constructor(val y: Float, val x: Float)

@Composable
internal fun Offset.asDpOffset() = DpOffset(
    y = this.y.toExactDp(DimSpec.Axis.Y).value,
    x = this.x.toExactDp(DimSpec.Axis.X).value,
)
