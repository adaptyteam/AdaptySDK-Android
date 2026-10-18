@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.cache

import androidx.annotation.RestrictTo
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.adaptyError
import com.adapty.internal.utils.errorCodeFromNetwork
import com.adapty.ui.internal.utils.LOG_PREFIX
import com.adapty.ui.internal.utils.log
import com.adapty.utils.AdaptyLogLevel.Companion.ERROR
import com.adapty.utils.AdaptyLogLevel.Companion.VERBOSE
import java.net.HttpURLConnection
import java.net.URL

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class MediaDownloader {

    private companion object {
        const val TIMEOUT = 30 * 1000
    }

    fun download(url: String): Result<HttpURLConnection> {
        log(VERBOSE) { "$LOG_PREFIX #AdaptyMediaCache# downloading media \"...${url.takeLast(10)}\"" }
        try {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = TIMEOUT
                readTimeout = TIMEOUT
                requestMethod = "GET"
            }
            connection.connect()

            if (connection.responseCode in 200..299) {
                log(VERBOSE) { "$LOG_PREFIX #AdaptyMediaCache# downloaded media \"...${url.takeLast(10)}\"" }
                return Result.success(connection)
            } else {
                log(ERROR) { "$LOG_PREFIX #AdaptyMediaCache# downloading media \"...${url.takeLast(10)}\" failed: code ${connection.responseCode}" }
                return Result.failure(
                    adaptyError(
                        message = "Request failed with code ${connection.responseCode}",
                        adaptyErrorCode = errorCodeFromNetwork(connection.responseCode)
                    )
                )
            }
        } catch (e: Exception) {
            log(ERROR) { "$LOG_PREFIX #AdaptyMediaCache# downloading media \"...${url.takeLast(10)}\" failed: code ${e.localizedMessage}" }
            return Result.failure(e)
        }
    }
}