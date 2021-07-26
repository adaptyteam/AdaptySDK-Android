package com.adapty.internal.domain

import android.content.Context
import androidx.annotation.RestrictTo
import com.adapty.internal.data.cache.CacheRepository
import com.adapty.internal.data.cloud.CloudRepository
import com.adapty.internal.utils.*
import com.adapty.internal.utils.AttributionHelper
import com.adapty.internal.utils.INFINITE_RETRY
import com.adapty.internal.utils.execute
import com.adapty.internal.utils.flowOnIO
import com.adapty.models.AttributionType
import com.adapty.utils.ProfileParameterBuilder
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import kotlinx.coroutines.flow.*

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class PurchaserInteractor(
    private val appContext: Context,
    private val authInteractor: AuthInteractor,
    private val cloudRepository: CloudRepository,
    private val cacheRepository: CacheRepository,
    private val attributionHelper: AttributionHelper,
) {

    @JvmSynthetic
    fun getPurchaserInfo(forceUpdate: Boolean) =
        if (forceUpdate) {
            getPurchaserInfoFromCloud()
        } else {
            flowOf(
                cacheRepository.getPurchaserInfo()
            ).flatMapConcat { cachedPurchaserInfo ->
                cachedPurchaserInfo?.let {
                    launchPurchaserInfoUpdate()
                    flowOf(it)
                } ?: getPurchaserInfoFromCloud()
            }
        }

    @JvmSynthetic
    fun updateProfile(params: ProfileParameterBuilder) =
        authInteractor.runWhenAuthDataSynced {
            cloudRepository.updateProfile(params)
        }
            .map(cacheRepository::updateDataOnUpdateProfile)
            .flowOnIO()

    @JvmSynthetic
    fun syncAttributions() {
        cacheRepository.getAttributionData().values.forEach { attributionData ->
            execute {
                authInteractor.runWhenAuthDataSynced(INFINITE_RETRY) {
                    cloudRepository.updateAttribution(attributionData)
                }
                    .map { cacheRepository.deleteAttributionData(attributionData.source) }
                    .catch { }
                    .collect()
            }
        }
    }

    @JvmSynthetic
    fun getPurchaserInfoFromCloud(maxAttemptCount: Long = DEFAULT_RETRY_COUNT) =
        authInteractor.runWhenAuthDataSynced(maxAttemptCount) {
            cloudRepository.getPurchaserInfo()
        }
            .map(cacheRepository::savePurchaserInfo)
            .flowOnIO()

    @JvmSynthetic
    fun getPurchaserInfoOnStart() =
        getPurchaserInfoFromCloud(INFINITE_RETRY)

    @JvmSynthetic
    fun syncMetaOnStart() =
        syncMeta(INFINITE_RETRY, null)

    private fun syncMeta(maxAttemptCount: Long, newToken: String?) =
        getAdIdIfAvailable()
            .flatMapConcat { adId ->
                authInteractor.runWhenAuthDataSynced(maxAttemptCount) {
                    cloudRepository.syncMeta(adId, newToken ?: cacheRepository.getPushToken())
                }
            }
            .map(cacheRepository::updateDataOnSyncMeta)
            .flowOnIO()

    @JvmSynthetic
    fun refreshPushToken(newToken: String): Flow<Unit> {
        cacheRepository.savePushToken(newToken)
        return syncMeta(DEFAULT_RETRY_COUNT, newToken)
    }

    @JvmSynthetic
    fun setExternalAnalyticsEnabled(enabled: Boolean) =
        authInteractor.runWhenAuthDataSynced {
            cloudRepository.setExternalAnalyticsEnabled(enabled)
        }.also { cacheRepository.saveExternalAnalyticsEnabled(enabled) }

    @JvmSynthetic
    fun updateAttribution(attribution: Any, source: AttributionType, networkUserId: String?) =
        saveAttributionData(attribution, source, networkUserId)
            .flatMapConcat { attributionData ->
                authInteractor.runWhenAuthDataSynced {
                    cloudRepository.updateAttribution(attributionData)
                }.map { cacheRepository.deleteAttributionData(attributionData.source) }
            }
            .flowOnIO()

    @JvmSynthetic
    fun saveAttributionData(attribution: Any, source: AttributionType, networkUserId: String?) =
        flow {
            val attributionData =
                attributionHelper.createAttributionData(attribution, source, networkUserId)
            cacheRepository.saveAttributionData(
                attributionData
            )
            emit(attributionData)
        }

    @JvmSynthetic
    fun subscribeOnPurchaserInfoChanges() =
        cacheRepository.subscribeOnPurchaserInfoChanges()

    @JvmSynthetic
    fun subscribeOnPromoChanges() =
        cacheRepository.subscribeOnPromoChanges()

    private fun launchPurchaserInfoUpdate() {
        execute {
            getPurchaserInfoFromCloud()
                .catch { }
                .collect()
        }
    }

    private fun getAdIdIfAvailable(): Flow<String?> =
        flow {
            emit(
                cacheRepository.getExternalAnalyticsEnabled().takeIf { it }?.let {
                    try {
                        AdvertisingIdClient.getAdvertisingIdInfo(appContext)
                            ?.takeIf { !it.isLimitAdTrackingEnabled }?.id
                    } catch (e: Exception) {
                        null
                    }
                }
            )
        }.flowOnIO()
}