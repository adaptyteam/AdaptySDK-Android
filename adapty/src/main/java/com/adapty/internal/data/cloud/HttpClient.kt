package com.adapty.internal.data.cloud

import androidx.annotation.RestrictTo
import androidx.annotation.WorkerThread
import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.utils.NetworkLogger
import java.net.HttpURLConnection

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class HttpClient(
    private val connectionCreator: NetworkConnectionCreator,
    private val responseManager: HttpResponseManager,
    private val networkLogger: NetworkLogger,
) {

    @WorkerThread
    @JvmSynthetic
    fun <T> newCall(request: Request, classOfT: Class<T>): Response<T> {
        networkLogger.logRequest {
            "${request.method.name} ${request.url}${
                request.body.takeIf(String::isNotEmpty)?.let { body -> " Body: $body" } ?: ""
            }"
        }

        var connection: HttpURLConnection? = null

        try {
            connection = connectionCreator.createUrlConnection(request)
            connection.connect()
            return responseManager.handleResponse(connection, request.responseCacheKeys, classOfT)

        } catch (e: Exception) {
            networkLogger.logError { e.localizedMessage ?: e.message ?: "" }
            return Response.Error(
                AdaptyError(
                    originalError = e,
                    message = "Request Error: ${e.localizedMessage ?: e.message}",
                    adaptyErrorCode = AdaptyErrorCode.REQUEST_FAILED
                )
            )
        } finally {
            connection?.disconnect()
        }
    }

}