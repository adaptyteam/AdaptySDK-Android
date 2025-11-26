package com.adapty.internal.data.cloud

import androidx.annotation.RestrictTo
import androidx.annotation.WorkerThread
import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.data.models.AnalyticsEvent.BackendAPIResponseData
import com.adapty.internal.utils.Logger
import com.adapty.utils.AdaptyLogLevel.Companion.VERBOSE
import com.adapty.utils.AdaptyLogLevel.Companion.WARN
import java.lang.reflect.Type
import java.net.HttpURLConnection

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class BaseHttpClient(
    private val connectionCreator: NetworkConnectionCreator,
    private val responseManager: HttpResponseManager,
    private val analyticsTracker: AnalyticsTracker,
    private val requestBlockingManager: RequestBlockingManager,
) : HttpClient {

    @WorkerThread
    @JvmSynthetic
    override fun <T> newCall(request: Request, typeOfT: Type): Response<T> {
        Logger.log(VERBOSE) {
            "${request.method.name} ${request.url}${
                request.body?.takeIf(String::isNotEmpty)?.let { body -> " Body: $body" }.orEmpty()
            }"
        }
        request.systemLog?.let { customData ->
            customData.resetFlowId()
            analyticsTracker.trackSystemEvent(customData)
        }

        val blockedError = requestBlockingManager.getBlockedError(request)
        if (blockedError != null) {
            Logger.log(WARN) { blockedError.message }
            request.systemLog?.let { customData ->
                analyticsTracker.trackSystemEvent(BackendAPIResponseData.create("", customData, blockedError))
            }
            throw blockedError
        }

        var connection: HttpURLConnection? = null

        try {
            connection = connectionCreator.createUrlConnection(request)
            connection.connect()
            return responseManager.handleResponse(connection, request, typeOfT)

        } catch (e: Throwable) {
            if (e is Response.Error) throw e
            val message = "Request Error: ${e.localizedMessage ?: e.message}"
            Logger.log(WARN) { message }
            request.systemLog?.let { customData ->
                analyticsTracker.trackSystemEvent(BackendAPIResponseData.create("", customData, e))
            }
            throw Response.Error(
                originalError = e,
                message = message,
                adaptyErrorCode = AdaptyErrorCode.REQUEST_FAILED,
                request = request,
            )
        } finally {
            connection?.disconnect()
        }
    }

}