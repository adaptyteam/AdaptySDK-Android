package com.adapty.api

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.util.Log
import com.adapty.api.requests.*
import com.adapty.api.responses.*
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

class ApiClient(private var context: Context, private val gson : Gson) {

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
        const val UPDATE_ATTRIBUTION_REQ_ID = 7
        const val GET_PROMO_REQ_ID = 8
        const val EXTERNAL_ANALYTICS_ENABLED_REQ_ID = 9
        const val TRANSACTION_VARIATION_REQ_ID = 10
        const val POST = "POST"
        const val PATCH = "PATCH"
        const val GET = "GET"
    }

    fun createProfile(
        request: CreateProfileRequest,
        adaptyCallback: AdaptyCallback?
    ) {
        post(
            generateUrl(CREATE_PROFILE_REQ_ID),
            request,
            CreateProfileResponse(),
            CREATE_PROFILE_REQ_ID,
            adaptyCallback
        )
    }

    fun updateProfile(
        request: UpdateProfileRequest,
        adaptyCallback: AdaptyCallback?
    ) {
        patch(
            generateUrl(UPDATE_PROFILE_REQ_ID),
            request,
            UpdateProfileResponse(),
            UPDATE_PROFILE_REQ_ID,
            adaptyCallback
        )
    }

    fun getProfile(
        adaptyCallback: AdaptyCallback?
    ) {
        get(
            generateUrl(GET_PROFILE_REQ_ID),
            PurchaserInfoResponse(),
            GET_PROFILE_REQ_ID,
            adaptyCallback
        )
    }

    fun getPaywalls(
        adaptyCallback: AdaptyCallback?
    ) {
        get(
            generateUrl(GET_CONTAINERS_REQ_ID),
            PaywallsResponse(),
            GET_CONTAINERS_REQ_ID,
            adaptyCallback
        )
    }

    fun syncMeta(
        request: SyncMetaInstallRequest,
        adaptyCallback: AdaptyCallback?
    ) {
        post(
            generateUrl(SYNC_META_REQ_ID),
            request,
            SyncMetaInstallResponse(),
            SYNC_META_REQ_ID,
            adaptyCallback
        )
    }

    fun validatePurchase(
        request: ValidateReceiptRequest,
        adaptyCallback: AdaptyCallback?
    ) {
        post(
            generateUrl(VALIDATE_PURCHASE_REQ_ID),
            request,
            ValidateReceiptResponse(),
            VALIDATE_PURCHASE_REQ_ID,
            adaptyCallback
        )
    }

    fun restorePurchase(
        request: RestoreReceiptRequest,
        adaptyCallback: AdaptyCallback?
    ) {
        post(
            generateUrl(RESTORE_PURCHASE_REQ_ID),
            request,
            RestoreReceiptResponse(),
            RESTORE_PURCHASE_REQ_ID,
            adaptyCallback
        )
    }

    fun updateAttribution(
        request: UpdateAttributionRequest,
        adaptyCallback: AdaptyCallback?
    ) {
        post(
            generateUrl(UPDATE_ATTRIBUTION_REQ_ID),
            request,
            Any(),
            UPDATE_ATTRIBUTION_REQ_ID,
            adaptyCallback
        )
    }

    fun setExternalAnalyticsEnabled(
        request: ExternalAnalyticsEnabledRequest,
        adaptyCallback: AdaptyCallback?
    ) {
        post(
            generateUrl(EXTERNAL_ANALYTICS_ENABLED_REQ_ID),
            request,
            Any(),
            EXTERNAL_ANALYTICS_ENABLED_REQ_ID,
            adaptyCallback
        )
    }

    fun setTransactionVariationId(
        request: TransactionVariationIdRequest,
        adaptyCallback: AdaptyCallback?
    ) {
        post(
            generateUrl(TRANSACTION_VARIATION_REQ_ID),
            request,
            Any(),
            TRANSACTION_VARIATION_REQ_ID,
            adaptyCallback
        )
    }

    fun getPromo(
        adaptyCallback: AdaptyCallback?
    ) {
        get(
            generateUrl(GET_PROMO_REQ_ID),
            PromoResponse(),
            GET_PROMO_REQ_ID,
            adaptyCallback
        )
    }

    private fun request(
        type: String,
        url: String,
        request: Any?,
        oresponse: Any?,
        reqID: Int,
        adaptyCallback: AdaptyCallback?
    ) {

        Thread(Runnable {

            var rString = ""

            try {

                val myUrl = URL(url)

                val conn = myUrl.openConnection() as HttpURLConnection

                conn.readTimeout = TIMEOUT
                conn.connectTimeout = TIMEOUT
                conn.requestMethod = type

                conn.setRequestProperty("Content-type", "application/vnd.api+json")

                conn.setRequestProperty("ADAPTY-SDK-PROFILE-ID", preferenceManager.profileID)
                conn.setRequestProperty("ADAPTY-SDK-PLATFORM", "Android")
                getCurrentLocale(context)?.let {
                    conn.setRequestProperty(
                        "ADAPTY-SDK-LOCALE",
                        "${it.language}_${it.country}"
                    )
                }
                conn.setRequestProperty("ADAPTY-SDK-VERSION", com.adapty.BuildConfig.VERSION_NAME)
                conn.setRequestProperty(
                    "ADAPTY-SDK-VERSION-BUILD",
                    com.adapty.BuildConfig.VERSION_CODE.toString()
                )
                conn.setRequestProperty(
                    AUTHORIZATION_KEY,
                    API_KEY_PREFIX.plus(preferenceManager.appKey)
                )

                conn.setRequestProperty("Connection", "close")
                System.setProperty("java.net.preferIPv4Stack", "true")
                System.setProperty("http.keepAlive", "false")

                conn.doInput = true

                if (type != GET) {
                    val req = gson.toJson(request)
                    conn.doOutput = true
                    val os = conn.outputStream
                    val writer = BufferedWriter(OutputStreamWriter(os, "UTF-8"))
                    writer.write(req)
                    writer.flush()
                    writer.close()
                    os.close()
                }

                conn.connect()

                val responseCode = conn.responseCode

                if (responseCode == HttpURLConnection.HTTP_OK
                    || responseCode == HttpURLConnection.HTTP_CREATED
                    || responseCode == HttpURLConnection.HTTP_ACCEPTED
                    || responseCode == HttpURLConnection.HTTP_NO_CONTENT
                    || responseCode == 207
                    || responseCode == 206
                ) {

                    val inputStream = conn.inputStream

                    rString = toStringUtf8(inputStream)

                } else {
                    rString = toStringUtf8(conn.errorStream)
                    fail(
                        AdaptyError(
                            message = "Request is unsuccessful. $reqID Url: $myUrl Response Code: $responseCode, Message: $rString",
                            adaptyErrorCode = AdaptyErrorCode.fromNetwork(responseCode)
                        ),
                        reqID,
                        adaptyCallback
                    )
                    return@Runnable
                }

            } catch (e: Exception) {
                e.printStackTrace()

                fail(
                    AdaptyError(
                        originalError = e,
                        message = "Request Exception. $reqID ${e.message} ${e.localizedMessage} Message: $rString",
                        adaptyErrorCode = AdaptyErrorCode.UNKNOWN
                    ),
                    reqID,
                    adaptyCallback
                )
                return@Runnable
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

    private fun getCurrentLocale(context: Context) =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales.get(0)
        } else {
            context.resources.configuration.locale
        }

    private fun post(
        url: String,
        request: Any,
        oresponse: Any?,
        reqID: Int,
        adaptyCallback: AdaptyCallback?
    ) {
        request(POST, url, request, oresponse, reqID, adaptyCallback)
    }

    private fun patch(
        url: String,
        request: Any,
        oresponse: Any?,
        reqID: Int,
        adaptyCallback: AdaptyCallback?
    ) {
        request(PATCH, url, request, oresponse, reqID, adaptyCallback)
    }

    private fun get(
        url: String,
        oresponse: Any?,
        reqID: Int,
        adaptyCallback: AdaptyCallback?
    ) {
        request(GET, url, null, oresponse, reqID, adaptyCallback)
    }

    private fun success(
        response: Any?,
        reqID: Int,
        adaptyCallback: AdaptyCallback?
    ) {
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
                            it.onResult((response as UpdateProfileResponse), null)
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
                        is AdaptyPaywallsCallback -> {
                            val data = (response as PaywallsResponse).data ?: arrayListOf()

                            val meta = (response).meta?.products ?: arrayListOf()
                            it.onResult(data, meta, null)
                        }
                        is AdaptyPromosCallback -> {
                            it.onResult((response as? PromoResponse)?.data?.attributes, null)
                        }
                    }
                }
            }
            mainHandler.post(myRunnable)
        } catch (e: Exception) {
            LogHelper.logError("Callback success error $reqID: ${e.message} ${e.localizedMessage}")
        }
    }

    private fun fail(
        error: AdaptyError,
        reqID: Int,
        adaptyCallback: AdaptyCallback?
    ) {
        LogHelper.logError("Request failed $reqID ${error.message}")
        try {
            val mainHandlerE = Handler(context.mainLooper)
            val myRunnableE = Runnable {
                adaptyCallback?.let {
                    when (it) {
                        is AdaptySystemCallback -> {
                            it.fail(error, reqID)
                        }
                        is AdaptyProfileCallback -> {
                            it.onResult(null, error)
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
                        is AdaptyPaywallsCallback -> {
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

    private fun generateUrl(reqId: Int): String {
        return when (reqId) {
            CREATE_PROFILE_REQ_ID, UPDATE_PROFILE_REQ_ID, GET_PROFILE_REQ_ID ->
                "${serverUrl}sdk/analytics/profiles/${preferenceManager.profileID}/"
            SYNC_META_REQ_ID ->
                "${serverUrl}sdk/analytics/profiles/${preferenceManager.profileID}/installation-metas/${preferenceManager.installationMetaID}/"
            VALIDATE_PURCHASE_REQ_ID ->
                "${serverUrl}sdk/in-apps/google/token/validate/"
            RESTORE_PURCHASE_REQ_ID ->
                "${serverUrl}sdk/in-apps/google/token/restore/"
            GET_CONTAINERS_REQ_ID ->
                "${serverUrl}sdk/in-apps/purchase-containers/?profile_id=${preferenceManager.profileID}${queryParamAboutTrackingPaywalls()}"
            UPDATE_ATTRIBUTION_REQ_ID ->
                "${serverUrl}sdk/analytics/profiles/${preferenceManager.profileID}/attribution/"
            GET_PROMO_REQ_ID ->
                "${serverUrl}sdk/analytics/profiles/${preferenceManager.profileID}/promo/"
            EXTERNAL_ANALYTICS_ENABLED_REQ_ID ->
                "${serverUrl}sdk/analytics/profiles/${preferenceManager.profileID}/analytics-enabled/"
            TRANSACTION_VARIATION_REQ_ID ->
                "${serverUrl}sdk/in-apps/transaction-variation-id/"
            else -> serverUrl
        }
    }

    private fun queryParamAboutTrackingPaywalls() =
        context.packageManager
            .getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
            .metaData
            ?.getBoolean("AdaptyAutomaticPaywallsScreenReportingEnabled", true)
            ?.takeIf(Boolean::not)
            ?.let { "&automatic_paywalls_screen_reporting_enabled=$it" }
            ?: ""
}
