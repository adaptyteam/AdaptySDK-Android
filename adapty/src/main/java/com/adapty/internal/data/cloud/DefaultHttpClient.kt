package com.adapty.internal.data.cloud

import androidx.annotation.RestrictTo
import androidx.annotation.WorkerThread
import com.adapty.internal.data.cache.CacheRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class DefaultHttpClient(
    private val httpClient: HttpClient,
    private val cacheRepository: CacheRepository,
    private val gson: Gson,
) : HttpClient {

    private val type by lazy {
        object : TypeToken<Map<String, Any>>() {}.type
    }

    @WorkerThread
    @JvmSynthetic
    override fun <T> newCall(request: Request, classOfT: Class<T>): Response<T> {
        val cacheOptions = request.requestCacheOptions

        when {
            cacheOptions == null || getRequestBodyMap(request.body) != getRequestBodyMap(cacheRepository.getString(cacheOptions.requestKey)) -> {
                return httpClient.newCall(request, classOfT)
                    .also { response ->
                        if (response is Response.Success) {
                            cacheOptions?.let { cacheOptions ->
                                cacheRepository.saveRequestOrResponseLatestData(mapOf(cacheOptions.requestKey to request.body))
                            }
                        }
                    }
            }

            cacheOptions.shouldSendEmptyRequest -> {
                return httpClient.newCall(request.apply { body = "" }, classOfT)
            }

            else -> throw RequestShouldNotBeSentException()
        }
    }

    private fun getRequestBodyMap(rawRequestBody: String?): Map<String, Any>? =
        gson.fromJson<Map<String, Any>>(rawRequestBody, type)
}