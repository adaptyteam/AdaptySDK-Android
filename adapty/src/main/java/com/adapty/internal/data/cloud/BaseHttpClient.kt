package com.adapty.internal.data.cloud

import androidx.annotation.RestrictTo
import androidx.annotation.WorkerThread
import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.utils.Logger
import com.adapty.utils.AdaptyLogLevel.Companion.VERBOSE
import com.adapty.utils.AdaptyLogLevel.Companion.WARN
import java.net.HttpURLConnection

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class BaseHttpClient(
    private val connectionCreator: NetworkConnectionCreator,
    private val responseManager: HttpResponseManager,
) : HttpClient {

    @WorkerThread
    @JvmSynthetic
    override fun <T> newCall(request: Request, classOfT: Class<T>): Response<T> {
        Logger.log(VERBOSE) {
            "${request.method.name} ${request.url}${
                request.body.takeIf(String::isNotEmpty)?.let { body -> " Body: $body" }.orEmpty()
            }"
        }

        var connection: HttpURLConnection? = null

        try {
            connection = connectionCreator.createUrlConnection(request)
            connection.connect()
            return responseManager.handleResponse(connection, request.responseCacheKeys, classOfT)

        } catch (e: Exception) {
            Logger.log(WARN) { e.localizedMessage ?: e.message.orEmpty() }
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