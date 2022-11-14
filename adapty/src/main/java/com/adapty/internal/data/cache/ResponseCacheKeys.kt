package com.adapty.internal.data.cache

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class ResponseCacheKeys private constructor(
    val responseKey: String,
    val responseHashKey: String,
) {
    companion object {
        fun forGetProfile() = ResponseCacheKeys(
            responseKey = PROFILE_RESPONSE,
            responseHashKey = PROFILE_RESPONSE_HASH
        )

        fun forGetProducts() = ResponseCacheKeys(
            responseKey = PRODUCT_RESPONSE,
            responseHashKey = PRODUCT_RESPONSE_HASH
        )

        fun forGetProductIds() = ResponseCacheKeys(
            responseKey = PRODUCT_IDS_RESPONSE,
            responseHashKey = PRODUCT_IDS_RESPONSE_HASH
        )

        fun forGetPaywall(id: String) = ResponseCacheKeys(
            responseKey = "$PAYWALL_RESPONSE_START_PART${id}$PAYWALL_RESPONSE_END_PART",
            responseHashKey = "$PAYWALL_RESPONSE_START_PART${id}$PAYWALL_RESPONSE_HASH_END_PART"
        )
    }
}