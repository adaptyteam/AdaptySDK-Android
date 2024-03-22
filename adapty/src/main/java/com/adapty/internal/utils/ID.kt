package com.adapty.internal.utils

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
internal value class ID<out T: String?>(val value: T) {

    companion object {
        val UNSPECIFIED = ID("")
    }
}