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
import java.lang.reflect.Type
import java.net.HttpURLConnection
import java.util.zip.GZIPInputStream

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal interface HttpResponseManager {

    fun <T> handleResponse(
        connection: HttpURLConnection,
        responseCacheKeys: ResponseCacheKeys?,
        typeOfT: Type,
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
        typeOfT: Type,
    ): Response<T> {
        val isInGzip =
            connection.getHeaderField("Content-Encoding")?.contains("gzip", true) ?: false

        if (connection.isSuccessful()) {
            val previousResponseHash = connection.getRequestProperty("ADAPTY-SDK-PREVIOUS-RESPONSE-HASH")
            val currentResponseHash = connection.getHeaderField("X-Response-Hash")

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
            return Response.Success(bodyConverter.convertSuccess(responseStr, typeOfT))

        } else {
            val responseStr = toStringUtf8(connection.errorStream, isInGzip)
            val errorMessage =
                "Request is unsuccessful. ${connection.url} Code: ${connection.responseCode}, Response: $responseStr"
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
}