package com.adapty.internal.utils

import androidx.annotation.RestrictTo
import java.util.Locale

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class PayloadProvider(
    private val hashingHelper: HashingHelper,
    private val metaInfoRetriever: MetaInfoRetriever,
) {

    fun getPayloadHashForPaywallRequest(locale: String, segmentId: String): String {
        val payload = "{\"locale\":\"${locale.lowercase(Locale.ENGLISH)}\",\"segment_hash\":\"$segmentId\",\"store\":\"${metaInfoRetriever.store}\"}"
        return hashingHelper.md5(payload)
    }

    fun getPayloadHashForPaywallBuilderRequest(locale: String, builderVersion: String): String {
        val payload = "{\"builder_version\":\"$builderVersion\",\"locale\":\"${locale.lowercase(Locale.ENGLISH)}\"}"
        return hashingHelper.md5(payload)
    }
}