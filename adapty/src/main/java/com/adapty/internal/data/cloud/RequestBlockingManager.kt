package com.adapty.internal.data.cloud

import androidx.annotation.RestrictTo
import com.adapty.models.AdaptyConfig.ServerCluster
import java.util.concurrent.ConcurrentHashMap

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class RequestBlockingManager(
    private val serverCluster: ServerCluster,
) {

    private var authBlockErrors = ConcurrentHashMap<String, Response.Error>()
    private val tempBlockErrors = ConcurrentHashMap<String, TempBlockInfo>()

    fun getBlockedError(request: Request): Response.Error? {
        authBlockErrors[serverCluster.baseUrl]
            ?.let { return it }
        if (request.baseUrl == serverCluster.uaBaseUrl)
            authBlockErrors[request.baseUrl]
                ?.let { return it }
        val currentTime = System.currentTimeMillis()
        return request.endpointTemplate?.let { tempBlockErrors[it] }?.takeIf { it.blockUntil > currentTime }?.error
    }

    fun handleError(error: Response.Error) {
        when (error.backendError?.responseCode) {
            401 -> setAuthenticationFailed(error)
            444 -> setTemporaryBlock(error)
        }
    }

    private fun setAuthenticationFailed(error: Response.Error) {
        val key = when {
            error.request.baseUrl == serverCluster.uaBaseUrl -> error.request.baseUrl
            error.request.endpointTemplate != null -> serverCluster.baseUrl
            else -> null
        } ?: return
        authBlockErrors[key] = error
    }

    private fun setTemporaryBlock(error: Response.Error) {
        val endpointTemplate = error.request.endpointTemplate ?: return
        val minutes = error.backendError?.responseBody?.toIntOrNull() ?: 1440
        val durationMillis = minutes * 60 * 1000L
        val blockUntil = System.currentTimeMillis() + durationMillis
        tempBlockErrors[endpointTemplate]?.let { (_, currentBlockUntil) ->
            if (currentBlockUntil > blockUntil) return
        }
        tempBlockErrors[endpointTemplate] = TempBlockInfo(error, blockUntil)
    }

    private data class TempBlockInfo(
        val error: Response.Error,
        val blockUntil: Long,
    )
}
