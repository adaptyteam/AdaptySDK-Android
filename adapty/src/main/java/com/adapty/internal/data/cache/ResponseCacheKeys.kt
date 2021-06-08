package com.adapty.internal.data.cache

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class ResponseCacheKeys private constructor(
    val responseKey: String,
    val responseHashKey: String,
) {
    companion object {
        fun forGetPurchaserInfo() = ResponseCacheKeys(
            responseKey = "get_purchaser_info_response",
            responseHashKey = "get_purchaser_info_response_hash"
        )

        fun forGetPaywalls() = ResponseCacheKeys(
            responseKey = "get_paywalls_response",
            responseHashKey = "get_paywalls_response_hash"
        )

        fun forGetPromo() = ResponseCacheKeys(
            responseKey = "get_promo_response",
            responseHashKey = "get_promo_response_hash"
        )
    }
}