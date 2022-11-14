package com.adapty.internal.utils

import android.content.Context
import androidx.annotation.RestrictTo
import com.adapty.internal.data.cache.CacheRepository
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Semaphore

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class AdIdRetriever(
    private val appContext: Context,
    private val cacheRepository: CacheRepository,
) {

    private val adIdSemaphore = Semaphore(1)

    @Volatile
    private var cachedAdvertisingId: String? = null

    init {
        execute { getAdIdIfAvailable().catch { }.collect() }
    }

    fun getAdIdIfAvailable(): Flow<String> =
        flow {
            if (cacheRepository.getExternalAnalyticsEnabled() == false) {
                emit("")
                return@flow
            }
            cachedAdvertisingId?.let { cachedAdId ->
                emit(cachedAdId)
                return@flow
            }

            adIdSemaphore.acquire()
            if (cacheRepository.getExternalAnalyticsEnabled() == false) {
                adIdSemaphore.release()
                emit("")
                return@flow
            }
            cachedAdvertisingId?.let { cachedAdId ->
                adIdSemaphore.release()
                emit(cachedAdId)
                return@flow
            }

            val adId = try {
                AdvertisingIdClient.getAdvertisingIdInfo(appContext)
                    .takeIf { !it.isLimitAdTrackingEnabled }?.id
            } catch (e: Exception) {
                null
            }

            cachedAdvertisingId = adId
            adIdSemaphore.release()
            emit(adId.orEmpty())
        }.flowOnIO()
}