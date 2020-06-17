package com.adapty.api

import android.content.Context
import android.os.Handler
import android.util.Log
import com.adapty.api.requests.*
import com.adapty.api.responses.*
import com.adapty.utils.ADAPTY_SDK_VERSION_INT
import com.adapty.utils.LogHelper
import com.adapty.utils.PreferenceManager
import com.google.gson.Gson
import java.io.*
import java.net.HttpURLConnection
import java.net.URL

const val AUTHORIZATION_KEY = "Authorization"
const val API_KEY_PREFIX = "Api-Key "
const val TAG = "[Adapty]"
const val TIMEOUT = 30 * 1000

class ApiClient(private var context: Context) {

    private val serverUrl = "https://api.adapty.io/api/v1/"
    private val preferenceManager = PreferenceManager(context)

    companion object {
        const val CREATE_PROFILE_REQ_ID = 0
        const val UPDATE_PROFILE_REQ_ID = 1
        const val SYNC_META_REQ_ID = 2
        const val VALIDATE_PURCHASE_REQ_ID = 3
        const val RESTORE_PURCHASE_REQ_ID = 4
        const val GET_PROFILE_REQ_ID = 5
        const val GET_CONTAINERS_REQ_ID = 6
        const val POST = "POST"
        const val PATCH = "PATCH"
        const val GET = "GET"
    }

    fun createProfile(request: CreateProfileRequest, adaptyCallback : AdaptyCallback?) {
        LogHelper.logVerbose(" apiclient create profile make post")
        post(generateUrl(CREATE_PROFILE_REQ_ID), request, CreateProfileResponse(), CREATE_PROFILE_REQ_ID, adaptyCallback)
    }

    fun updateProfile(request: UpdateProfileRequest, adaptyCallback : AdaptyCallback?) {
        patch(generateUrl(UPDATE_PROFILE_REQ_ID), request, UpdateProfileResponse(), UPDATE_PROFILE_REQ_ID, adaptyCallback)
    }

    fun getProfile(request: PurchaserInfoRequest, adaptyCallback : AdaptyCallback?) {
        LogHelper.logVerbose("getPurchaserInfo() getProfile() make get")
        get(generateUrl(GET_PROFILE_REQ_ID), request, PurchaserInfoResponse(), GET_PROFILE_REQ_ID, adaptyCallback)
    }

    fun getPurchaseContainers(request: PurchaseContainersRequest, adaptyCallback : AdaptyCallback?) {
        get(generateUrl(GET_CONTAINERS_REQ_ID), request, PurchaseContainersResponse(), GET_CONTAINERS_REQ_ID, adaptyCallback)
    }

    fun syncMeta(request: SyncMetaInstallRequest, adaptyCallback : AdaptyCallback?) {
        LogHelper.logVerbose("sendSyncMetaInstallRequest make post")
        post(generateUrl(SYNC_META_REQ_ID), request, SyncMetaInstallResponse(), SYNC_META_REQ_ID, adaptyCallback)
    }

    fun validatePurchase(request: ValidateReceiptRequest, adaptyCallback : AdaptyCallback?) {
        post(generateUrl(VALIDATE_PURCHASE_REQ_ID), request, ValidateReceiptResponse(), VALIDATE_PURCHASE_REQ_ID, adaptyCallback)
    }

    fun restorePurchase(request: RestoreReceiptRequest, adaptyCallback : AdaptyCallback?) {
        post(generateUrl(RESTORE_PURCHASE_REQ_ID), request, RestoreReceiptResponse(), RESTORE_PURCHASE_REQ_ID, adaptyCallback)
    }

    val gson = Gson()

    private fun request(type: String, url: String, request: Any, oresponse: Any?, reqID: Int, adaptyCallback : AdaptyCallback?) {
        LogHelper.logVerbose("opened request $reqID")
        Thread(Runnable {
            LogHelper.logVerbose("opened thread for request $reqID")
            var rString = ""

            try {
                LogHelper.logVerbose("opened thread try for request $reqID")
                val req = gson.toJson(request)
                LogHelper.logVerbose("another thread gson success for request $reqID")
                val myUrl = URL(url)
                LogHelper.logVerbose("another thread url success for request $reqID")
                val conn = myUrl.openConnection() as HttpURLConnection
                LogHelper.logVerbose("another thread connection opened success for request $reqID")
                conn.readTimeout = TIMEOUT
                conn.connectTimeout = TIMEOUT
                LogHelper.logVerbose("another thread setTimeout for request $reqID")
                conn.requestMethod = type
                LogHelper.logVerbose("another thread setRequestMethod for request $reqID")

                conn.setRequestProperty("Content-type", "application/vnd.api+json")

                conn.setRequestProperty("ADAPTY-SDK-PROFILE-ID", preferenceManager.profileID)
                LogHelper.logVerbose("another thread added headers1 for request $reqID")
                conn.setRequestProperty("ADAPTY-SDK-PLATFORM", "Android")
                LogHelper.logVerbose("another thread added headers2 for request $reqID")
                conn.setRequestProperty("ADAPTY-SDK-VERSION", com.adapty.BuildConfig.VERSION_NAME)
                LogHelper.logVerbose("another thread added headers3 for request $reqID")
                conn.setRequestProperty("ADAPTY-SDK-VERSION-BUILD", ADAPTY_SDK_VERSION_INT.toString())
                LogHelper.logVerbose("another thread added headers4 for request $reqID")
                conn.setRequestProperty(AUTHORIZATION_KEY, API_KEY_PREFIX.plus(preferenceManager.appKey))
                LogHelper.logVerbose("another thread added headers5 for request $reqID")

                conn.doInput = true
                LogHelper.logVerbose("another thread doInput for request $reqID")

                if (type != GET) {
                    val os = conn.outputStream
                    LogHelper.logVerbose("another thread output for request $reqID")
                    val writer = BufferedWriter(OutputStreamWriter(os, "UTF-8"))
                    LogHelper.logVerbose("another thread buffering for request $reqID")
                    writer.write(req)
                    LogHelper.logVerbose("another thread write req for request $reqID")
                    writer.flush()
                    LogHelper.logVerbose("another thread flush for request $reqID")
                    writer.close()
                    LogHelper.logVerbose("another thread writer close for request $reqID")
                    os.close()
                    LogHelper.logVerbose("another thread os close for request $reqID")
                }

                conn.connect()
                LogHelper.logVerbose("another thread conn.connect() for request $reqID")

                val response = conn.responseCode
                LogHelper.logVerbose("another thread response ${conn.responseCode} for request $reqID")

                if (response == HttpURLConnection.HTTP_OK
                    || response == HttpURLConnection.HTTP_CREATED
                    || response == HttpURLConnection.HTTP_ACCEPTED
                    || response == HttpURLConnection.HTTP_NO_CONTENT
                    || response == 207
                    || response == 206
                ) {
                    LogHelper.logVerbose("another thread response success for request $reqID")
                    val inputStream = conn.inputStream
                    LogHelper.logVerbose("another thread response inputStream for request $reqID")

                    rString = toStringUtf8(inputStream)
                    LogHelper.logVerbose("another thread response toUtf8 for request $reqID")
                    LogHelper.logVerbose("Response $reqID $myUrl: $rString")

                } else {
                    rString = toStringUtf8(conn.errorStream)
                    LogHelper.logVerbose("another thread response toUtf8 non success for request $reqID")
                    fail(
                        "Request is unsuccessful. $reqID Url: $myUrl Response Code: $response, Message: $rString",
                        reqID,
                        adaptyCallback
                    )
                    return@Runnable
                }
            } catch (e: Exception) {
                LogHelper.logVerbose("another thread exception ${e.message} ${e.localizedMessage} for request $reqID")
                e.printStackTrace()

                fail(
                    "Request Exception. $reqID ${e.message} ${e.localizedMessage} Message: ${rString ?: ""}",
                    reqID,
                    adaptyCallback
                )
                return@Runnable
            }

            var responseObj: Any?

            try {
                responseObj = if (oresponse != null) {
                    gson.fromJson(rString, oresponse.javaClass)
                    LogHelper.logVerbose("another thread exception convertFromGson for request $reqID")
                } else {
                    rString
                    LogHelper.logVerbose("another thread exception oresponse null for request $reqID")
                }

                success(responseObj, reqID, adaptyCallback)
            } catch (e: Exception) {
                LogHelper.logVerbose("another thread exception2 ${e.message} ${e.localizedMessage} for request $reqID")
                e.printStackTrace()
                responseObj = rString
                LogHelper.logError("Request $reqID parse error: ${e.message}, ${e.localizedMessage}")
                success(responseObj, reqID, adaptyCallback)
            }
        }).start()
    }

    private fun post(url: String, request: Any, oresponse: Any?, reqID: Int, adaptyCallback : AdaptyCallback?) {
        LogHelper.logVerbose("make request $reqID")
        request(POST, url, request, oresponse, reqID, adaptyCallback)
    }

    private fun patch(url: String, request: Any, oresponse: Any?, reqID: Int, adaptyCallback : AdaptyCallback?) {
        request(PATCH, url, request, oresponse, reqID, adaptyCallback)
    }

    private fun get(url: String, request: Any, oresponse: Any?, reqID: Int, adaptyCallback : AdaptyCallback?) {
        request(GET, url, request, oresponse, reqID, adaptyCallback)
    }

    private fun success(response: Any?, reqID: Int, adaptyCallback : AdaptyCallback?) {
        LogHelper.logVerbose("Response success $reqID")
        try {
            val mainHandler = Handler(context.mainLooper)
            val myRunnable = Runnable {
                adaptyCallback?.let {
                    when (it) {
                        is AdaptySystemCallback -> {
                            it.success(response, reqID)
                        }
                        is AdaptyProfileCallback -> {
                            it.onResult(null)
                        }
                        is AdaptyValidateCallback -> {
                            it.onResult((response as ValidateReceiptResponse), null)
                        }
                        is AdaptyRestoreCallback -> {
                            it.onResult((response as RestoreReceiptResponse), null)
                        }
                        is AdaptyPurchaserInfoCallback -> {
                            val res = (response as PurchaserInfoResponse).data?.attributes
                            it.onResult(res, null)
                        }
                        is AdaptyPurchaseContainersCallback -> {
                            var data = (response as PurchaseContainersResponse).data
                            if (data == null)
                                data = arrayListOf()

                            var meta = (response).meta?.products
                            if (meta == null)
                                meta = arrayListOf()
                            it.onResult(data, meta, null)
                        }
                    }
                }
            }
            mainHandler.post(myRunnable)
        } catch (e: Exception) {
            LogHelper.logError("Callback success error $reqID: ${e.message} ${e.localizedMessage}")
        }
    }

    private fun fail(error: String, reqID: Int, adaptyCallback : AdaptyCallback?) {
        LogHelper.logError("Request failed $reqID $error")
        try {
            val mainHandlerE = Handler(context.mainLooper)
            val myRunnableE = Runnable {
                adaptyCallback?.let {
                    when (it) {
                        is AdaptySystemCallback -> {
                            it.fail(error, reqID)
                        }
                        is AdaptyProfileCallback -> {
                            it.onResult(error)
                        }
                        is AdaptyValidateCallback -> {
                            it.onResult(null, error)
                        }
                        is AdaptyRestoreCallback -> {
                            it.onResult(null, error)
                        }
                        is AdaptyPurchaserInfoCallback -> {
                            it.onResult(null, error)
                        }
                        is AdaptyPurchaseContainersCallback -> {
                            it.onResult(arrayListOf(), arrayListOf(), error)
                        }
                    }
                }
            }
            mainHandlerE.post(myRunnableE)
        } catch (e: Exception) {
            LogHelper.logError("Callback Fail error $reqID: ${e.message} ${e.localizedMessage}")
        }
    }

    private fun toStringUtf8(inputStream: InputStream): String{
        val r = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
        val total = StringBuilder()
        var line: String? = r.readLine()
        while (line != null) {
            total.append(line).append('\n')
            line = r.readLine()
        }
        return total.toString()
    }

    private fun generateUrl(reqId: Int): String{
        return when (reqId) {
            CREATE_PROFILE_REQ_ID, UPDATE_PROFILE_REQ_ID, GET_PROFILE_REQ_ID ->
                serverUrl + "sdk/analytics/profiles/" + preferenceManager.profileID + "/"
            SYNC_META_REQ_ID ->
                serverUrl + "sdk/analytics/profiles/" + preferenceManager.profileID + "/installation-metas/" + preferenceManager.installationMetaID + "/"
            VALIDATE_PURCHASE_REQ_ID ->
                serverUrl + "sdk/in-apps/google/token/validate/"
            RESTORE_PURCHASE_REQ_ID ->
                serverUrl + "sdk/in-apps/google/token/restore/"
            GET_CONTAINERS_REQ_ID ->
                serverUrl + "sdk/in-apps/purchase-containers/?profile_id=" + preferenceManager.profileID
            else -> serverUrl
        }
    }
}
