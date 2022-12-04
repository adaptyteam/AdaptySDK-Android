package com.adapty.internal.data.cloud

import androidx.annotation.RestrictTo
import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.data.cache.CacheRepository
import com.adapty.internal.data.cache.ResponseCacheKeys
import com.adapty.internal.utils.Logger
import com.adapty.utils.AdaptyLogLevel.Companion.ERROR
import com.adapty.utils.AdaptyLogLevel.Companion.VERBOSE
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.util.zip.GZIPInputStream

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal interface HttpResponseManager {

    fun <T> handleResponse(
        connection: HttpURLConnection,
        responseCacheKeys: ResponseCacheKeys?,
        classOfT: Class<T>
    ): Response<T>
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class DefaultHttpResponseManager(
    private val bodyConverter: ResponseBodyConverter,
    private val cacheRepository: CacheRepository,
) : HttpResponseManager {

    override fun <T> handleResponse(
        connection: HttpURLConnection,
        responseCacheKeys: ResponseCacheKeys?,
        classOfT: Class<T>
    ): Response<T> {
        val isInGzip =
            connection.getHeaderField("Content-Encoding")?.contains("gzip", true) ?: false

        if (connection.isSuccessful()) {
            val response = connection.evaluateSuccessfulResponse(isInGzip, responseCacheKeys)
            Logger.log(VERBOSE) { "Request is successful. ${connection.url} Response: $response" }
            return bodyConverter.convertSuccess(response, classOfT)

        } else {
            val response = toStringUtf8(connection.errorStream, isInGzip)
            val errorMessage =
                "Request is unsuccessful. ${connection.url} Code: ${connection.responseCode}, Response: $response"
            Logger.log(ERROR) { errorMessage }
            return Response.Error(
                AdaptyError(
                    message = errorMessage,
                    adaptyErrorCode = AdaptyErrorCode.fromNetwork(connection.responseCode)
                )
            )
        }
    }

    private fun toStringUtf8(inputStream: InputStream, isInGzip: Boolean): String {
        return BufferedReader(
            InputStreamReader(
                if (isInGzip) GZIPInputStream(inputStream) else inputStream,
                Charsets.UTF_8
            )
        ).useLines { lines ->
            lines.fold(StringBuilder()) { total, line ->
                if (total.isNotEmpty()) total.append('\n')
                total.append(line)
            }.toString()
        }
    }

    private fun HttpURLConnection.isSuccessful() = responseCode in 200..299

    private fun HttpURLConnection.evaluateSuccessfulResponse(
        isInGzip: Boolean,
        responseCacheKeys: ResponseCacheKeys?,
    ): String {
        val previousResponseHash = getRequestProperty("ADAPTY-SDK-PREVIOUS-RESPONSE-HASH")
        val currentResponseHash = getHeaderField("X-Response-Hash")

        return if (!previousResponseHash.isNullOrEmpty() && previousResponseHash == currentResponseHash) {
            responseCacheKeys?.responseKey?.let(cacheRepository::getString)
                ?: toStringUtf8(inputStream, isInGzip)
        } else {
            toStringUtf8(inputStream, isInGzip).also { response ->
                if (responseCacheKeys != null && currentResponseHash != null) {
                    cacheRepository.saveRequestOrResponseLatestData(
                        mapOf(
                            responseCacheKeys.responseKey to response,
                            responseCacheKeys.responseHashKey to currentResponseHash
                        )
                    )
                }
            }
        }
    }
}