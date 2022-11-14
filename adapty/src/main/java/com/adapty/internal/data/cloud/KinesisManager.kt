package com.adapty.internal.data.cloud

import androidx.annotation.RestrictTo
import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode
import com.adapty.errors.AdaptyErrorCode.ANALYTICS_DISABLED
import com.adapty.internal.data.cache.CacheRepository
import com.adapty.internal.data.models.AwsRecordModel
import com.adapty.internal.domain.ProfileInteractor
import com.adapty.internal.utils.*
import com.adapty.utils.ErrorCallback
import com.google.android.gms.common.util.Base64Utils
import com.google.gson.Gson
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Semaphore
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class KinesisManager(
    private val cacheRepository: CacheRepository,
    private val gson: Gson,
    private val httpClient: HttpClient,
    private val requestFactory: RequestFactory,
    private val profileInteractor: ProfileInteractor,
) {
    private val kinesisStream = "adapty-data-pipeline-prod"
    private val sessionId = generateUuid()

    private val dataSyncSemaphore = Semaphore(1)

    fun trackEvent(
        eventName: String,
        subMap: Map<String, Any>? = null,
        callback: ErrorCallback? = null,
    ) {
        execute {
            prepareEvents(eventName, subMap)
                .run {
                    if (callback != null) {
                        this.flowOnIO()
                            .onEach {
                                callback.onResult(null) //since the event has been saved and will be sent at least later
                            }
                            .catch { error ->
                                if (error is ExternalAnalyticsDisabledException) {
                                    val errorMessage = "We can't handle analytics events, since you've opted it out."
                                    Logger.logAnalytics { errorMessage }
                                    callback.onResult(
                                        AdaptyError(
                                            message = errorMessage,
                                            adaptyErrorCode = ANALYTICS_DISABLED
                                        )
                                    )
                                } else {
                                    throw error
                                }
                            }
                            .flowOnMain()
                    } else {
                        this
                    }
                }
                .flatMapConcat { records ->
                    if (cacheRepository.getIamAccessKeyId() == null || cacheRepository.getIamSecretKey() == null || cacheRepository.getIamSessionToken() == null) {
                        throw NoKeysForKinesisException()
                    }
                    sendEvents(records)
                        .catch { error ->
                            if ((error as? AdaptyError)?.adaptyErrorCode == AdaptyErrorCode.BAD_REQUEST) {
                                emitAll(
                                    profileInteractor.getAnalyticsCreds(DEFAULT_RETRY_COUNT)
                                        .flatMapConcat { sendEvents(records) }
                                )
                            } else {
                                throw error
                            }
                        }
                }.catch { dataSyncSemaphore.release() }
                .flowOnIO()
                .collect()
        }
    }

    private fun prepareEvents(
        eventName: String,
        subMap: Map<String, Any>?,
    ): Flow<List<AwsRecordModel>> =
        flow {
            if (cacheRepository.getExternalAnalyticsEnabled() == false) {
                throw ExternalAnalyticsDisabledException()
            }

            val dataStr = gson.toJson(
                hashMapOf<String, Any>(
                    "profile_id" to cacheRepository.getProfileId(),
                    "session_id" to sessionId,
                    "event_name" to eventName,
                    "profile_installation_meta_id" to cacheRepository.getInstallationMetaId(),
                    "event_id" to generateUuid(),
                    "created_at" to formatCurrentDateTime(),
                    "platform" to "Android"
                ).apply {
                    subMap?.let(::putAll)
                }
            )

            dataSyncSemaphore.acquire()
            val records = cacheRepository.getKinesisRecords().apply {
                add(
                    AwsRecordModel(
                        Base64Utils.encode(dataStr.toByteArray()).replace("\n", ""),
                        cacheRepository.getInstallationMetaId()
                    )
                )
            }
            cacheRepository.saveKinesisRecords(records.takeLast(50))

            emit(records)
        }

    private fun sendEvents(records: List<AwsRecordModel>) =
        flow {
            val response = httpClient.newCall(
                requestFactory.kinesisRequest(
                    mapOf("Records" to records, "StreamName" to kinesisStream)
                ),
                Unit::class.java
            )
            when (response) {
                is Response.Success -> {
                    saveRecordsOnSuccess(records)
                    dataSyncSemaphore.release()
                    emit(Unit)
                }
                is Response.Error -> {
                    throw response.error
                }
            }
        }

    private fun saveRecordsOnSuccess(records: List<AwsRecordModel>) {
        cacheRepository.getKinesisRecords().let { savedRecords ->
            cacheRepository.saveKinesisRecords(
                savedRecords.subtract(records).toList().takeLast(50)
            )
        }
    }

    private class ExternalAnalyticsDisabledException : Exception()

    private fun formatCurrentDateTime(): String =
        Calendar.getInstance().time.let(dateFormatter::format)

    private val dateFormatter: DateFormat by lazy {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("GMT")
        }
    }
}