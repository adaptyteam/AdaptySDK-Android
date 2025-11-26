package com.adapty.internal.data.cloud

import androidx.annotation.RestrictTo
import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.data.cache.CacheRepository
import com.adapty.internal.data.models.NetConfig
import com.adapty.internal.utils.Logger
import com.adapty.internal.utils.withLockSafe
import com.adapty.utils.AdaptyLogLevel.Companion.ERROR
import java.util.concurrent.locks.ReentrantReadWriteLock

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class NetConfigManager(
    private val httpClient: HttpClient,
    private val cacheRepository: CacheRepository,
    private val auxRequestFactory: AuxRequestFactory,
    private val netConfigLock: ReentrantReadWriteLock,
) {

    fun getConfig(): NetConfig {
        netConfigLock.readLock().withLockSafe {
            val netConfig = cacheRepository.netConfig
            if (System.currentTimeMillis() < netConfig.expiresAt) {
                return netConfig
            }
        }

        return netConfigLock.writeLock().withLockSafe {
            val netConfig = cacheRepository.netConfig
            if (System.currentTimeMillis() < netConfig.expiresAt) {
                return netConfig
            }

            runCatching {
                fetchNetConfig().also { cacheRepository.netConfig = it }
            }.getOrElse {
                cacheRepository.netConfig.also { it.extend() }
            }
        }
    }

    fun getBaseUrl(): String = getConfig().getCurrentEndpointOrNull()
        ?: run {
            val message = "Request can't be processed: no valid endpoint available"
            Logger.log(ERROR) { message }
            throw AdaptyError(
                message = message,
                adaptyErrorCode = AdaptyErrorCode.BAD_REQUEST,
            )
        }

    fun switch(fromBaseUrl: String) {
        netConfigLock.writeLock().withLockSafe {
            cacheRepository.netConfig
                .takeIf { it.getCurrentEndpointOrNull() == fromBaseUrl }
                ?.switch()
        }
    }

    private fun fetchNetConfig(): NetConfig =
        httpClient.newCall<NetConfig>(
            auxRequestFactory.fetchNetConfigRequest(),
            NetConfig::class.java
        ).data
}
