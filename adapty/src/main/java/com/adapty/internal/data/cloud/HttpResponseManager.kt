package com.adapty.internal.data.cloud

import androidx.annotation.RestrictTo
import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.data.cache.CacheRepository
import com.adapty.internal.data.models.AnalyticsEvent.BackendAPIResponseData
import com.adapty.internal.data.models.BackendError
import com.adapty.internal.utils.Logger
import com.adapty.utils.AdaptyLogLevel.Companion.ERROR
import com.adapty.utils.AdaptyLogLevel.Companion.INFO
import com.adapty.utils.AdaptyLogLevel.Companion.VERBOSE
import com.google.gson.reflect.TypeToken
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.reflect.Type
import java.net.HttpURLConnection
import java.util.zip.GZIPInputStream

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal interface HttpResponseManager {

    fun <T> handleResponse(
        connection: HttpURLConnection,
        request: Request,
        typeOfT: Type,
    ): Response<T>
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class DefaultHttpResponseManager(
    private val bodyConverter: ResponseBodyConverter,
    private val cacheRepository: CacheRepository,
    private val analyticsTracker: AnalyticsTracker,
) : HttpResponseManager {

    override fun <T> handleResponse(
        connection: HttpURLConnection,
        request: Request,
        typeOfT: Type,
    ): Response<T> {
        val isInGzip =
            connection.getHeaderField("Content-Encoding")?.contains("gzip", true) ?: false

        val requestId = connection.getHeaderField("request-id").orEmpty()
        if (connection.isSuccessful()) {
            val previousResponseHash = connection.getRequestProperty("ADAPTY-SDK-PREVIOUS-RESPONSE-HASH")
            val currentResponseHash = connection.getHeaderField("X-Response-Hash")

            connection.getHeaderField("CF-Cache-Status")?.let { header ->
                Logger.log(INFO) { "CF-Cache-Status: $header" }
            }

            val responseCacheKeys = request.responseCacheKeys
            val responseStr: String
            if (!previousResponseHash.isNullOrEmpty() && previousResponseHash == currentResponseHash) {
                responseStr = responseCacheKeys?.responseKey?.let(cacheRepository::getString)
                    ?: toStringUtf8(connection.inputStream, isInGzip)
            } else {
                responseStr = toStringUtf8(connection.inputStream, isInGzip)
                if (responseCacheKeys != null && currentResponseHash != null) {
                    cacheRepository.saveRequestOrResponseLatestData(
                        mapOf(
                            responseCacheKeys.responseKey to responseStr,
                            responseCacheKeys.responseHashKey to currentResponseHash
                        )
                    )
                }
            }
            Logger.log(VERBOSE) { "Request is successful. ${connection.url} Response: $responseStr" }
            request.systemLog?.let { customData ->
                analyticsTracker.trackSystemEvent(BackendAPIResponseData.create(requestId, connection.headerFields, customData))
            }
            return Response.Success(bodyConverter.convert(responseStr, typeOfT))

        } else {
            val responseStr = toStringUtf8(connection.errorStream, isInGzip)
            val errorMessage =
                "Request is unsuccessful. ${connection.url} Code: ${connection.responseCode}, Response: $responseStr"
            Logger.log(ERROR) { errorMessage }
            val e = AdaptyError(
                message = errorMessage,
                adaptyErrorCode = AdaptyErrorCode.fromNetwork(connection.responseCode),
                backendError = BackendError(
                    connection.responseCode,
                    runCatching {
                        bodyConverter.convert<Set<BackendError.InternalError>>(
                            responseStr,
                            object : TypeToken<Set<BackendError.InternalError>>() {}.type,
                        )
                    }.getOrNull() ?: emptySet(),
                ),
            )
            request.systemLog?.let { customData ->
                analyticsTracker.trackSystemEvent(BackendAPIResponseData.create(requestId, customData, e))
            }
            return Response.Error(e)
        }
    }

    private fun toStringUtf8(inputStream: InputStream, isInGzip: Boolean): String {
        val reader = if (isInGzip) GZIPInputStream(inputStream) else inputStream
        return reader.bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    private fun HttpURLConnection.isSuccessful() = responseCode in 200..299
}