package com.adapty.internal.data.cloud

import androidx.annotation.RestrictTo
import com.adapty.internal.data.cache.CacheRepository
import com.adapty.internal.data.cloud.Request.Method.GET
import com.adapty.internal.data.cloud.Request.Method.POST
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset
import java.security.MessageDigest
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal interface NetworkConnectionCreator {

    @Throws(NoKeysForKinesisException::class)
    fun createUrlConnection(request: Request): HttpURLConnection

    fun getTimeOut() = 30 * 1000
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class DefaultConnectionCreator(
    private val cacheRepository: CacheRepository,
) : NetworkConnectionCreator {

    companion object {
        private const val AUTHORIZATION_KEY = "Authorization"
        private const val API_KEY_PREFIX = "Api-Key "
    }

    override fun createUrlConnection(request: Request) =
        (URL(request.url).openConnection() as HttpURLConnection).apply {
            connectTimeout = getTimeOut()
            readTimeout = getTimeOut()
            requestMethod = request.method.name
            doInput = true

            setRequestProperty("Content-type", "application/vnd.api+json")
            setRequestProperty("Accept-Encoding", "gzip")
            setRequestProperty("ADAPTY-SDK-PROFILE-ID", cacheRepository.getProfileId())
            setRequestProperty("ADAPTY-SDK-PLATFORM", "Android")
            setRequestProperty("ADAPTY-SDK-VERSION", com.adapty.BuildConfig.VERSION_NAME)
            setRequestProperty(AUTHORIZATION_KEY, "$API_KEY_PREFIX${cacheRepository.getAppKey()}")
            request.responseCacheKeys?.responseHashKey?.let(cacheRepository::getString)
                ?.let { latestResponseHash ->
                    setRequestProperty("ADAPTY-SDK-PREVIOUS-RESPONSE-HASH", latestResponseHash)
                }

            if (request.method != GET) {
                doOutput = true
                val os = outputStream
                BufferedWriter(OutputStreamWriter(os, "UTF-8")).apply {
                    write(request.body)
                    flush()
                    close()
                }
                os.close()
            }
        }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class KinesisConnectionCreator(
    private val cacheRepository: CacheRepository,
) : NetworkConnectionCreator {

    private val serviceType = "kinesis"
    private val region = "us-east-1"
    private val SIGNING_ALGORITHM = "AWS4-HMAC-SHA256"
    private val MAC_ALGORITHM = "HmacSHA256"

    override fun createUrlConnection(request: Request): HttpURLConnection {
        val iamAccessKeyId = cacheRepository.getIamAccessKeyId()
        val iamSecretKey = cacheRepository.getIamSecretKey()
        val iamSessionToken = cacheRepository.getIamSessionToken()

        if (iamAccessKeyId == null || iamSecretKey == null || iamSessionToken == null)
            throw NoKeysForKinesisException()

        val url = URL(request.url)

        val date = getIso8601Time()

        val headersMap = mapOf(
            "X-Amz-Security-Token" to iamSessionToken,
            "Host" to "kinesis.us-east-1.amazonaws.com",
            "X-Amz-Date" to date,
            "X-Amz-Target" to "Kinesis_20131202.PutRecords",
            "Content-Type" to "application/x-amz-json-1.1",
            "Accept-Encoding" to "gzip, deflate, br",
            "Accept-Language" to "ru"
        )

        val signedHeaders =
            headersMap.map { it.key.trim().toLowerCase(Locale.ENGLISH) }.sorted()
                .joinToString(";")
        val canonicalPath = if (url.path.isEmpty()) "/" else url.path
        val canonicalQuery = url.query.orEmpty()
        val canonicalHeaders = headersMap.map {
            it.key.trim().toLowerCase(Locale.ENGLISH) + ":" + it.value
        }.sorted().joinToString("\n")
        val canonicalRequest = listOf(
            POST.name,
            canonicalPath,
            canonicalQuery,
            canonicalHeaders,
            "",
            signedHeaders,
            request.body.sha256()
        ).joinToString("\n")

        val credential = listOf(
            date.substring(0..7),
            region,
            serviceType,
            "aws4_request"
        ).joinToString("/")
        val stringToSign = listOf(
            "AWS4-HMAC-SHA256",
            date,
            credential,
            canonicalRequest.sha256()
        ).joinToString("\n")

        val k1 = "AWS4$iamSecretKey"
        val sk1 = hmacSha256(k1, date.substring(0..7))
        val sk2 = hmacSha256(sk1, region)
        val sk3 = hmacSha256(sk2, serviceType)
        val sk4 = hmacSha256(sk3, "aws4_request")
        val signature = hmacSha256(sk4, stringToSign)
        val s = signature.toHexString()

        val authorization =
            "$SIGNING_ALGORITHM Credential=$iamAccessKeyId/$credential, SignedHeaders=$signedHeaders, Signature=$s"

        return (url.openConnection() as HttpURLConnection).apply {
            readTimeout = getTimeOut()
            connectTimeout = getTimeOut()
            requestMethod = POST.name

            headersMap.forEach {
                setRequestProperty(it.key, it.value)
            }

            setRequestProperty("Authorization", authorization)

            doInput = true

            val os = outputStream
            BufferedWriter(OutputStreamWriter(os, "UTF-8")).apply {
                write(request.body)
                flush()
                close()
            }
            os.close()
        }
    }

    private fun getIso8601Time(): String {
        val c = Calendar.getInstance().time
        val tz = TimeZone.getTimeZone("GMT")
        val df: DateFormat =
            SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US)

        df.timeZone = tz
        return df.format(c)
    }

    private fun String.sha256(): String {
        return hashString(this, "SHA-256")
    }

    private fun hashString(input: String, algorithm: String): String {
        return MessageDigest
            .getInstance(algorithm)
            .digest(input.toByteArray())
            .fold("", { str, it -> str + "%02x".format(it) })
    }

    @Throws(Exception::class)
    private fun hmacSha256(key: ByteArray, data: String): ByteArray {
        val sha256Hmac = Mac.getInstance(MAC_ALGORITHM)
        val secretKey = SecretKeySpec(key, MAC_ALGORITHM)
        sha256Hmac.init(secretKey)

        return sha256Hmac.doFinal(data.toByteArray(charset("UTF-8")))
    }

    @Throws(Exception::class)
    private fun hmacSha256(key: String, data: String) =
        hmacSha256(key.toByteArray(Charset.forName("utf-8")), data)

    private fun ByteArray.toHexString() =
        fold("") { str, it -> str + "%02x".format(it) }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class NoKeysForKinesisException : Exception()