package com.adapty.internal.domain.models

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal enum class Source {
    CLOUD, CACHE, FALLBACK
}