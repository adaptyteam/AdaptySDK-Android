package com.adapty.ui.internal.utils

internal sealed class Union<A, B> {
    data class First<A, B>(val value: A) : Union<A, B>()
    data class Second<A, B>(val value: B) : Union<A, B>()
}