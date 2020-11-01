package com.adapty.utils

import com.adapty.api.ApiClient.Companion.POST
import com.adapty.api.TIMEOUT
import com.adapty.api.aws.AwsRecordModel
import com.adapty.api.aws.hmacSha256
import com.adapty.api.aws.toHexString
import com.google.android.gms.common.util.Base64Utils
import com.google.gson.Gson
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


class KinesisManager(private val preferenceManager: PreferenceManager) {
    private val url = "https://kinesis.us-east-1.amazonaws.com/"
    private val serviceType = "kinesis"
    private val region = "us-east-1"
    private val kinesisStream = "adapty-data-pipeline-prod"
    private val sessionId = generateUuid().toString()
    private val SIGNING_ALGORITHM = "AWS4-HMAC-SHA256"
    private val gson = Gson()

    fun trackEvent(eventName: String, subMap: Map<String, String>? = null) {
        val iamAccessKeyId = preferenceManager.iamAccessKeyId
        val iamSecretKey = preferenceManager.iamSecretKey
        val iamSessionToken = preferenceManager.iamSessionToken

        if (iamAccessKeyId == null || iamSecretKey == null || iamSessionToken == null)
            return

        val dataStr = gson.toJson(
            hashMapOf(
                "profile_id" to preferenceManager.profileID,
                "session_id" to sessionId,
                "event_name" to eventName,
                "profile_installation_meta_id" to preferenceManager.installationMetaID,
                "event_id" to generateUuid().toString(),
                "created_at" to getIso8601TimeDate(),
                "platform" to "Android"
            ).apply {
                subMap?.let(::putAll)
            }
        )

        val records = preferenceManager.kinesisRecords
        records.add(
            AwsRecordModel(
                Base64Utils.encode(dataStr.toByteArray()).replace("\n", ""),
                preferenceManager.installationMetaID
            )
        )
        preferenceManager.kinesisRecords = records.takeLast(50)

        request(hashMapOf("Records" to records, "StreamName" to kinesisStream))
    }

    private fun request(
        request: HashMap<String, Any>
    ) {

        Thread(Runnable {

            val iamAccessKeyId = preferenceManager.iamAccessKeyId
            val iamSecretKey = preferenceManager.iamSecretKey
            val iamSessionToken = preferenceManager.iamSessionToken

            if (iamAccessKeyId == null || iamSecretKey == null || iamSessionToken == null)
                return@Runnable

            val date = getIso8601Time()

            var rString = ""

            try {
                val req = gson.toJson(request).replace("\\u003d", "=")

                val myUrl = URL(url)

                val headersMap = hashMapOf(
                    "X-Amz-Security-Token" to iamSessionToken,
                    "Host" to "kinesis.us-east-1.amazonaws.com",
                    "X-Amz-Date" to date,
                    "X-Amz-Target" to "Kinesis_20131202.PutRecords",
                    "Content-Type" to "application/x-amz-json-1.1",
                    "Accept-Encoding" to "gzip, deflate, br",
                    "Accept-Language" to "ru"
                )

                ///// TASK 1: CREATE A CANONICAL REQUEST
                val signedHeaders =
                    headersMap.map { it.key.trim().toLowerCase(Locale.ENGLISH) }.sorted()
                        .joinToString(";")
                val canonicalPath = if (myUrl.path.isEmpty()) "/" else myUrl.path
                val canonicalQuery = myUrl.query ?: ""
                val canonicalHeaders = headersMap.map {
                    it.key.trim().toLowerCase(Locale.ENGLISH) + ":" + it.value
                }.sorted().joinToString("\n")
                val canonicalRequest = arrayListOf(
                    POST,
                    canonicalPath,
                    canonicalQuery,
                    canonicalHeaders,
                    "",
                    signedHeaders,
                    req.sha256()
                ).joinToString("\n")

                ///// TASK 2: CREATE THE STRING TO SIGN

                val credential = arrayListOf(
                    date.substring(0..7),
                    region,
                    serviceType,
                    "aws4_request"
                ).joinToString("/")
                val stringToSign = arrayListOf(
                    "AWS4-HMAC-SHA256",
                    date,
                    credential,
                    canonicalRequest.sha256()
                ).joinToString("\n")

                ///// TASK 3: CALCULATE THE SIGNATURE

                val k1 = "AWS4$iamSecretKey"
                val sk1 = hmacSha256(k1, date.substring(0..7))
                val sk2 = hmacSha256(sk1, region)
                val sk3 = hmacSha256(sk2, serviceType)
                val sk4 = hmacSha256(sk3, "aws4_request")
                val signature = hmacSha256(sk4, stringToSign)
                val s = signature.toHexString()

                // ************* TASK 4: ADD SIGNING INFORMATION TO THE REQUEST *************

                val authorization =
                    "$SIGNING_ALGORITHM Credential=$iamAccessKeyId/$credential, SignedHeaders=$signedHeaders, Signature=$s"

                val conn = myUrl.openConnection() as HttpURLConnection

                conn.readTimeout = TIMEOUT
                conn.connectTimeout = TIMEOUT
                conn.requestMethod = POST

                headersMap.onEach {
                    conn.setRequestProperty(it.key, it.value)
                }

                conn.setRequestProperty("Authorization", authorization)

                conn.doInput = true

                val os = conn.outputStream
                val writer = BufferedWriter(OutputStreamWriter(os, "UTF-8"))
                writer.write(req)
                writer.flush()
                writer.close()
                os.close()

                conn.connect()

                val response = conn.responseCode

                if (response == HttpURLConnection.HTTP_OK
                    || response == HttpURLConnection.HTTP_CREATED
                    || response == HttpURLConnection.HTTP_ACCEPTED
                    || response == HttpURLConnection.HTTP_NO_CONTENT
                    || response == 207
                    || response == 206
                ) {

                    val inputStream = conn.inputStream

                    rString = toStringUtf8(inputStream)
                    LogHelper.logVerbose("Response $myUrl: $rString")

                    val sentRecords = request["Records"] as ArrayList<AwsRecordModel>
                    val savedRecords = preferenceManager.kinesisRecords
                    val notSent = ArrayList<AwsRecordModel>()
                    for (saved in savedRecords) {
                        var contains = false
                        for (sent in sentRecords) {
                            if (sent == saved)
                                contains = true
                        }
                        if (!contains)
                            notSent.add(saved)
                    }

                    preferenceManager.kinesisRecords = notSent.takeLast(50)

                } else {
                    rString = toStringUtf8(conn.errorStream)
                    LogHelper.logVerbose("Response $myUrl: $rString ${conn.responseMessage}")
                    return@Runnable
                }
            } catch (e: Exception) {
                e.printStackTrace()
                LogHelper.logError("Kinesis Request Exception ${e.message} ${e.localizedMessage}")

                if (rString.isEmpty()) {
                    return@Runnable
                }
            }
        }).start()
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


    private fun toStringUtf8(inputStream: InputStream): String {
        val r = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
        val total = StringBuilder()
        var line: String? = r.readLine()
        while (line != null) {
            total.append(line).append('\n')
            line = r.readLine()
        }
        return total.toString()
    }

    private fun getIso8601Time(): String {
        val c = Calendar.getInstance().time
        val tz = TimeZone.getTimeZone("GMT")
        val df: DateFormat =
            SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US)

        df.timeZone = tz
        return df.format(c)
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