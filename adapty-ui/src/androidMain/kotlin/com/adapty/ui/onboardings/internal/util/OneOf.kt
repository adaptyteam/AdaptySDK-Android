package com.adapty.ui.onboardings.internal.util

internal sealed class OneOf<A : Any, B : Any> {
    data class First<A : Any, B : Any>(val value: A) : OneOf<A, B>()
    data class Second<A : Any, B : Any>(val value: B) : OneOf<A, B>()
}

internal sealed class OneOf3<A : Any, B : Any, C : Any> {
    data class First<A : Any, B : Any, C : Any>(val value: A) : OneOf3<A, B, C>()
    data class Second<A : Any, B : Any, C : Any>(val value: B) : OneOf3<A, B, C>()
    data class Third<A : Any, B : Any, C : Any>(val value: C) : OneOf3<A, B, C>()
}