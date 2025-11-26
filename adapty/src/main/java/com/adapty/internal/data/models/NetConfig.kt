package com.adapty.internal.data.models

import androidx.annotation.RestrictTo
import com.adapty.models.AdaptyConfig
import com.google.gson.annotations.SerializedName

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class NetConfig(
    @SerializedName("events_excluded_from_recording")
    val eventsExcludedFromRecording: List<String>,
    @SerializedName("events_excluded_from_sending")
    val eventsExcludedFromSending: List<String>,
    @SerializedName("api_endpoints")
    val apiEndpoints: List<String>,
    @SerializedName("expires_at")
    var expiresAt: Long,
    @SerializedName("retry_interval")
    val retryInterval: Long,
) {

    @Transient
    private var currentIndex = 0

    companion object {
        val SWITCHING_STATUSES = listOf(503, 520, 524, 526)

        fun createDefault(serverCluster: AdaptyConfig.ServerCluster): NetConfig {
            val now = System.currentTimeMillis()

            return NetConfig(
                emptyList(),
                listOf("system_log"),
                listOf(serverCluster.baseUrl),
                now,
                1800 * 1000L,
            )
        }
    }

    fun extend() {
        expiresAt += retryInterval
    }

    fun switch() {
        if (apiEndpoints.size <= 1) return
        if (currentIndex < apiEndpoints.lastIndex)
            currentIndex++
    }

    fun getCurrentEndpointOrNull(): String? {
        return apiEndpoints.getOrNull(currentIndex)
    }
}
