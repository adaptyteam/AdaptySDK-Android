package com.adapty.internal.data.cache

import androidx.annotation.RestrictTo
import com.google.gson.annotations.SerializedName

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class CacheEntity<T>(
    @SerializedName("value")
    val value: T,
    @SerializedName("version")
    val version: Int = 1,
    @SerializedName("cached_at")
    val cachedAt: Long = System.currentTimeMillis(),
) {
    operator fun component1() = value

    operator fun component2() = version

    operator fun component3() = cachedAt
}