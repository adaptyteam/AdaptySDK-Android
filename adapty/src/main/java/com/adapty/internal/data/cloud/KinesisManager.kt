package com.adapty.internal.data.cloud

import androidx.annotation.RestrictTo
import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.data.cache.CacheRepository
import com.adapty.internal.data.models.AwsRecordModel
import com.adapty.internal.domain.PurchaserInteractor
import com.adapty.internal.utils.Logger
import com.adapty.internal.utils.execute
import com.adapty.internal.utils.generateUuid
import com.google.android.gms.common.util.Base64Utils
import com.google.gson.Gson
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Semaphore
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.ArrayList

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class KinesisManager(
    private val cacheRepository: CacheRepository,
    private val gson: Gson,
    private val httpClient: HttpClient,
    private val requestFactory: RequestFactory,
    private val purchaserInteractor: PurchaserInteractor,
) {
    private val kinesisStream = "adapty-data-pipeline-prod"
    private val sessionId = generateUuid()

    private val keysSyncSemaphore = Semaphore(1)
    private val isSyncingKeys = AtomicBoolean(false)

    fun trackEvent(eventName: String, subMap: Map<String, String>? = null) {
        execute {
            flow<List<AwsRecordModel>> {
                if (!cacheRepository.getExternalAnalyticsEnabled()) {
                    Logger.logAnalytics { "We can't handle analytics events, since you've opted it out." }
                    throw ExternalAnalyticsDisabledException()
                }

                if (cacheRepository.getIamAccessKeyId() == null || cacheRepository.getIamSecretKey() == null || cacheRepository.getIamSessionToken() == null)
                    throw NoKeysForKinesisException()

                val dataStr = gson.toJson(
                    hashMapOf(
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

                val records = cacheRepository.getKinesisRecords().apply {
                    add(
                        AwsRecordModel(
                            Base64Utils.encode(dataStr.toByteArray()).replace("\n", ""),
                            cacheRepository.getInstallationMetaId().orEmpty()
                        )
                    )
                }
                cacheRepository.saveKinesisRecords(records.takeLast(50))

                val response = httpClient.newCall(
                    requestFactory.kinesisRequest(
                        hashMapOf("Records" to records, "StreamName" to kinesisStream)
                    ),
                    Unit::class.java
                )
                (response as? Response.Success)?.let {
                    emit(records)
                } ?: (response as? Response.Error)?.let {
                    throw it.error
                }
            }.onEach { sentRecords ->
                cacheRepository.getKinesisRecords().let { savedRecords ->
                    cacheRepository.saveKinesisRecords(
                        ArrayList(savedRecords.subtract(sentRecords)).takeLast(50)
                    )
                }
            }.catch { error ->
                if ((error as? AdaptyError)?.adaptyErrorCode == AdaptyErrorCode.BAD_REQUEST) {
                    val shouldResync = isSyncingKeys.compareAndSet(false, true)
                    keysSyncSemaphore.acquire()
                    if (shouldResync) {
                        purchaserInteractor.syncMetaOnStart()
                            .onEach { isSyncingKeys.set(false); keysSyncSemaphore.release() }
                            .catch { isSyncingKeys.set(false); keysSyncSemaphore.release() }
                            .collect()
                    } else {
                        keysSyncSemaphore.release()
                    }
                }

            }.collect()
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

    private fun <T> ArrayList<T>.takeLast(n: Int): ArrayList<T> {
        if (n == 0) return arrayListOf()
        val size = this.size
        if (n >= size) return this
        if (n == 1) return arrayListOf(last())
        val list = ArrayList<T>(n)
        for (index in size - n until size)
            list.add(this[index])
        return list
    }
}