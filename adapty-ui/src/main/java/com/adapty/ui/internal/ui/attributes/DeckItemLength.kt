@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.ui.attributes

import com.adapty.internal.utils.InternalAdaptyApi

internal sealed class DeckItemLength {
    data class Fixed(val value: DimUnit) : DeckItemLength()
    data class Parent(val fraction: Float) : DeckItemLength()
    object Content : DeckItemLength()
}
