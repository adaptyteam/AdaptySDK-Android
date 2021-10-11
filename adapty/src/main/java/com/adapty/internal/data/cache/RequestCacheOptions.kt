package com.adapty.internal.data.cache

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class RequestCacheOptions private constructor(
    val requestKey: String,
    val shouldSendEmptyRequest: Boolean,
) {
    companion object {

        fun forUpdateProfile() = RequestCacheOptions(
            requestKey = UPDATE_PROFILE_REQUEST_KEY,
            shouldSendEmptyRequest = false,
        )

        fun forUpdateAttribution(source: String) = RequestCacheOptions(
            requestKey = "update_${source}_attribution_request",
            shouldSendEmptyRequest = false,
        )

        fun forSyncMeta() = RequestCacheOptions(
            requestKey = SYNC_META_REQUEST_KEY,
            shouldSendEmptyRequest = true,
        )
    }
}