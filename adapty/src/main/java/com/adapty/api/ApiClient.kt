package com.adapty.api

import android.content.Context
import android.os.Handler
import android.util.Log
import android.widget.Toast
import com.adapty.api.requests.*
import com.adapty.api.responses.*
import com.adapty.utils.PreferenceManager
import com.google.gson.Gson
import java.io.*
import java.net.HttpURLConnection
import java.net.URL

const val AUTHORIZATION_KEY = "Authorization"
const val API_KEY_PREFIX = "Api-Key "
const val TAG = "[Adapty]"

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
        const val POST = "POST"
        const val PATCH = "PATCH"
        const val GET = "GET"
    }

    fun createProfile(request: CreateProfileRequest, adaptyCallback : AdaptyCallback?) {
        post(generateUrl(CREATE_PROFILE_REQ_ID), request, CreateProfileResponse(), CREATE_PROFILE_REQ_ID, adaptyCallback)
    }

    fun updateProfile(request: UpdateProfileRequest, adaptyCallback : AdaptyCallback?) {
        patch(generateUrl(UPDATE_PROFILE_REQ_ID), request, UpdateProfileResponse(), UPDATE_PROFILE_REQ_ID, adaptyCallback)
    }

    fun getProfile(request: PurchaserInfoRequest, adaptyCallback : AdaptyCallback?) {
        get(generateUrl(UPDATE_PROFILE_REQ_ID), request, PurchaserInfoResponse(), UPDATE_PROFILE_REQ_ID, adaptyCallback)
    }

    fun syncMeta(request: SyncMetaInstallRequest, adaptyCallback : AdaptyCallback?) {
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

        Thread(Runnable {

            var rString = ""

            try {

                val req = gson.toJson(request)

                val myUrl = URL(url)

                val conn = myUrl.openConnection() as HttpURLConnection

                conn.readTimeout = 10000 * 6
                conn.connectTimeout = 15000 * 4
                conn.requestMethod = type

                conn.setRequestProperty("Content-type", "application/vnd.api+json")

                conn.setRequestProperty("ADAPTY-SDK-PROFILE-ID", preferenceManager.profileID)
                conn.setRequestProperty("ADAPTY-SDK-PLATFORM", "Android")
                conn.setRequestProperty(AUTHORIZATION_KEY, API_KEY_PREFIX.plus(preferenceManager.appKey))

                conn.doInput = true

                if (type != GET) {
                    val os = conn.outputStream
                    val writer = BufferedWriter(OutputStreamWriter(os, "UTF-8"))
                    writer.write(req)
                    writer.flush()
                    writer.close()
                    os.close()
                }

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
                    Log.e(TAG,"Response $myUrl: $rString")

                } else {
                    rString = toStringUtf8(conn.errorStream)
                    Log.e(TAG, "Response $myUrl: $rString")
                    fail("Request is unsuccessful. Response Code: $response", reqID, adaptyCallback)
                    return@Runnable
                }
            } catch (e: Exception) {
                e.printStackTrace()

                if (rString.isEmpty()) {
                    fail("Unknown Error", reqID, adaptyCallback)
                    return@Runnable
                }
            }

            var responseObj: Any?

            try {
                responseObj = if (oresponse != null) {
                    gson.fromJson(rString, oresponse.javaClass)
                } else
                    rString
                success(responseObj, reqID, adaptyCallback)
            } catch (e: Exception) {
                e.printStackTrace()
                responseObj = rString
                success(responseObj, reqID, adaptyCallback)
            }
        }).start()
    }

    private fun post(url: String, request: Any, oresponse: Any?, reqID: Int, adaptyCallback : AdaptyCallback?) {
        request(POST, url, request, oresponse, reqID, adaptyCallback)
    }

    private fun patch(url: String, request: Any, oresponse: Any?, reqID: Int, adaptyCallback : AdaptyCallback?) {
        request(PATCH, url, request, oresponse, reqID, adaptyCallback)
    }

    private fun get(url: String, request: Any, oresponse: Any?, reqID: Int, adaptyCallback : AdaptyCallback?) {
        request(GET, url, request, oresponse, reqID, adaptyCallback)
    }

    private fun success(response: Any?, reqID: Int, adaptyCallback : AdaptyCallback?) {
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
                    }
                }
            }
            mainHandler.post(myRunnable)
        } catch (e: Exception) {
            Log.e("$TAG success", e.localizedMessage)
        }
    }

    private fun fail(error: String, reqID: Int, adaptyCallback : AdaptyCallback?) {
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
                    }
                }
            }
            mainHandlerE.post(myRunnableE)
        } catch (e: Exception) {
            Log.e("$TAG fail", e.localizedMessage)
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
            else -> serverUrl
        }
    }
}
