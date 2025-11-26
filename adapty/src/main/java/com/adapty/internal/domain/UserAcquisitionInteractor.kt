@file:OptIn(InternalAdaptyApi::class)

package com.adapty.internal.domain

import android.os.Build
import com.adapty.internal.data.cache.CacheRepository
import com.adapty.internal.data.cloud.CloudRepository
import com.adapty.internal.data.cloud.ReferrerManager
import com.adapty.internal.data.models.InstallData
import com.adapty.internal.data.models.InstallRegistrationData
import com.adapty.internal.utils.AdIdRetriever
import com.adapty.internal.utils.AppSetIdRetriever
import com.adapty.internal.utils.INFINITE_RETRY
import com.adapty.internal.utils.InstallRegistrationResponseDataMapper
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.MetaInfoRetriever
import com.adapty.internal.utils.asAdaptyError
import com.adapty.internal.utils.releaseQuietly
import com.adapty.internal.utils.retryIfNecessary
import com.adapty.models.AdaptyInstallationStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flattenConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Semaphore
import java.util.Calendar
import kotlin.math.pow
import kotlin.random.Random

internal class UserAcquisitionInteractor(
    private val authInteractor: AuthInteractor,
    private val referrerManager: ReferrerManager,
    private val adIdRetriever: AdIdRetriever,
    private val appSetIdRetriever: AppSetIdRetriever,
    private val metaInfoRetriever: MetaInfoRetriever,
    private val cloudRepository: CloudRepository,
    private val cacheRepository: CacheRepository,
    private val installationStatusMapper: InstallRegistrationResponseDataMapper,
) {

    private val registerInstallSemaphore = Semaphore(1)

    fun handleFirstLaunch() {
        if (!metaInfoRetriever.isJustInstalled())
            return
        cacheRepository.saveInstallData(InstallData(Calendar.getInstance().timeInMillis))
    }

    fun handleNewSession() = flow {
        val installData = cacheRepository.getInstallData()
        if (installData == null) {
            emit(null)
            return@flow
        }
        cacheRepository.incrementSessionCount()
        emit(installData)
    }.flatMapConcat { installData ->
        registerInstall(installData)
    }

    fun registerInstall(): Flow<Unit> {
        return registerInstall(cacheRepository.getInstallData())
    }

    private fun registerInstall(installData: InstallData?): Flow<Unit> {
        if (installData == null || cacheRepository.getInstallRegistrationResponseData() != null)
            return flowOf(Unit)

        if (!registerInstallSemaphore.tryAcquire())
            return flowOf(Unit)

        if (cacheRepository.getInstallRegistrationResponseData() != null) {
            registerInstallSemaphore.releaseQuietly()
            return flowOf(Unit)
        }

        return combine(
            adIdRetriever.getAdIdIfAvailable(),
            referrerManager.getData(),
            appSetIdRetriever.getAppSetIdIfAvailable(),
        ) { adId, referrerDetails, appSetId ->
            val installTimeFormatted =
                metaInfoRetriever.formatDateTimeGMT(
                    referrerDetails?.installBeginTimestampSeconds
                        ?.takeIf { it > 0 }
                        ?.times(1000)
                        ?: installData.installTimestampMillis
                )

            val installRegistrationData = InstallRegistrationData(
                metaInfoRetriever.applicationId,
                referrerDetails?.installReferrer,
                adId.takeIf { it.isNotEmpty() },
                appSetId.takeIf { it.isNotEmpty() },
                metaInfoRetriever.androidId,
                metaInfoRetriever.platform,
                metaInfoRetriever.os,
                Build.BRAND,
                Build.MODEL,
                metaInfoRetriever.displayMetrics.widthPixels,
                metaInfoRetriever.displayMetrics.heightPixels,
                metaInfoRetriever.displayMetrics.density,
                metaInfoRetriever.timezone,
                metaInfoRetriever.currentLocaleFormatted ?: "unknown",
                metaInfoRetriever.formatDateTimeGMT(),
                installTimeFormatted,
            )

            installRegistrationData
        }.flatMapConcat { installRegistrationData ->
            var retryAttempt = 0L
            authInteractor.runWhenAuthDataSynced(maxAttemptCount = INFINITE_RETRY) {
                flow {
                    emit(
                        cloudRepository.registerInstall(
                            installRegistrationData.copy(clientTime = metaInfoRetriever.formatDateTimeGMT()),
                            retryAttempt++,
                            REGISTER_INSTALL_MAX_RETRIES,
                        ).data
                    )
                }
                    .retryIfNecessary(REGISTER_INSTALL_MAX_RETRIES) { attempt ->
                        val max = (2f.pow(attempt.toInt()).coerceAtMost(30f) * 1000L).toLong()
                        Random.nextLong(max + 1).coerceAtLeast(500L)
                    }
            }
                .flattenConcat()
        }.map { installRegistrationResponseData ->
            cacheRepository.saveInstallRegistrationResponseData(installRegistrationResponseData)
        }
            .onEach { registerInstallSemaphore.releaseQuietly() }
            .catch { error ->
                cacheRepository.saveInstallRegistrationResponseError(error.asAdaptyError())
                registerInstallSemaphore.releaseQuietly()
            }
    }

    fun getCurrentInstallationStatus() = authInteractor.runWhenAuthDataSynced {
        val installData = cacheRepository.getInstallData()
            ?: return@runWhenAuthDataSynced AdaptyInstallationStatus.Determined.NotAvailable

        val installRegistrationResponseData = cacheRepository.getInstallRegistrationResponseData()
            ?: return@runWhenAuthDataSynced AdaptyInstallationStatus.NotDetermined

        installationStatusMapper.mapStatus(
            installRegistrationResponseData,
            installData,
            cacheRepository.getSessionCount()
        )
    }

    fun subscribeOnInstallRegistration() =
        cacheRepository.subscribeOnInstallRegistration()
            .mapNotNull { result ->
                val installData = cacheRepository.getInstallData() ?: return@mapNotNull null
                result.map {
                    installationStatusMapper.mapDetails(
                        it,
                        installData,
                        cacheRepository.getSessionCount(),
                    )
                }
            }

    private companion object {
        const val REGISTER_INSTALL_MAX_RETRIES = 10L
    }
}