package com.adapty.internal.data.cache

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class ResponseCacheKeys(
    val responseKey: String,
    val responseHashKey: String,
)