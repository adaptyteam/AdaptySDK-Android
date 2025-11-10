package com.adapty.internal.data.cache

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class ResponseCacheKeyProvider {

    fun forGetProfile() = ResponseCacheKeys(
        responseKey = PROFILE_RESPONSE,
        responseHashKey = PROFILE_RESPONSE_HASH
    )

    fun forGetProducts() = ResponseCacheKeys(
        responseKey = PRODUCTS_RESPONSE,
        responseHashKey = PRODUCTS_RESPONSE_HASH
    )
}