package com.adapty.internal.data.cloud

import androidx.annotation.RestrictTo
import com.adapty.internal.data.cache.CacheRepository
import com.adapty.internal.data.models.AwsRecordModel
import com.adapty.internal.utils.Logger
import com.adapty.internal.utils.execute
import com.adapty.internal.utils.generateUuid
import com.google.android.gms.common.util.Base64Utils
import com.google.gson.Gson
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class KinesisManager(
    private val cacheRepository: CacheRepository,
    private val gson: Gson,
    private val httpClient: HttpClient,
    private val requestFactory: RequestFactory
) {
    private val kinesisStream = "adapty-data-pipeline-prod"
    private val sessionId = generateUuid()

    fun trackEvent(eventName: String, subMap: Map<String, String>? = null) {
        if (!cacheRepository.getExternalAnalyticsEnabled()) {
            Logger.logVerbose { "We can't handle analytics events, since you've opted it out." }
            return
        }

        val iamAccessKeyId = cacheRepository.getIamAccessKeyId()
        val iamSecretKey = cacheRepository.getIamSecretKey()
        val iamSessionToken = cacheRepository.getIamSessionToken()

        if (iamAccessKeyId == null || iamSecretKey == null || iamSessionToken == null)
            return

        val dataStr = gson.toJson(
            hashMapOf(
                "profile_id" to cacheRepository.getProfileId(),
                "session_id" to sessionId,
                "event_name" to eventName,
                "profile_installation_meta_id" to cacheRepository.getInstallationMetaId(),
                "event_id" to generateUuid(),
                "created_at" to getIso8601TimeDate(),
                "platform" to "Android"
            ).apply {
                subMap?.let(::putAll)
            }
        )

        val records = cacheRepository.getKinesisRecords()
        records.add(
            AwsRecordModel(
                Base64Utils.encode(dataStr.toByteArray()).replace("\n", ""),
                cacheRepository.getInstallationMetaId().orEmpty()
            )
        )
        cacheRepository.saveKinesisRecords(records.takeLast(50))

        val body = hashMapOf<String, Any>("Records" to records, "StreamName" to kinesisStream)

        execute {
            flow {
                val response = httpClient.newCall(
                    requestFactory.kinesisRequest(body),
                    Unit::class.java
                )
                (response as? Response.Success)?.let {
                    emit(Unit)
                } ?: (response as? Response.Error)?.let {
                    throw it.error
                }
            }.onEach {
                val sentRecords =
                    (body[records] as? ArrayList<*>)?.filterIsInstance<AwsRecordModel>() ?: listOf()
                val savedRecords = cacheRepository.getKinesisRecords()
                val notSent = ArrayList(savedRecords.subtract(sentRecords))

                cacheRepository.saveKinesisRecords(notSent.takeLast(50))
            }
                .catch { }
                .collect()
        }
    }

    private fun getIso8601TimeDate(): String {
        val c = Calendar.getInstance().time
        val tz = TimeZone.getTimeZone("GMT")
        val df: DateFormat =
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US)

        df.timeZone = tz
        return df.format(c)
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