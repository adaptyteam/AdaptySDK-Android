package com.adapty.api

import android.annotation.SuppressLint
import android.content.Context
import android.os.AsyncTask
import android.os.Build
import com.adapty.Adapty.Companion.context
import com.adapty.api.entity.attribution.AttributeUpdateAttributionReq
import com.adapty.api.entity.attribution.DataUpdateAttributionReq
import com.adapty.api.entity.paywalls.ProductModel
import com.adapty.api.entity.profile.AttributeProfileReq
import com.adapty.api.entity.profile.DataProfileReq
import com.adapty.api.entity.restore.RestoreItem
import com.adapty.api.entity.syncmeta.AttributeSyncMetaReq
import com.adapty.api.entity.syncmeta.DataSyncMetaReq
import com.adapty.api.entity.validate.AttributeRestoreReceiptReq
import com.adapty.api.entity.validate.AttributeValidateReceiptReq
import com.adapty.api.entity.validate.DataRestoreReceiptReq
import com.adapty.api.entity.validate.DataValidateReceiptReq
import com.adapty.api.requests.*
import com.adapty.utils.*
import com.adapty.utils.push.PushTokenRetriever
import com.android.billingclient.api.BillingClient.SkuType.SUBS
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.gson.Gson
import java.util.*


class ApiClientRepository(var preferenceManager: PreferenceManager, private val gson : Gson) {

    private var apiClient = ApiClient(context, gson)

    var pushToken: String? = null

    private val tokenRetriever: PushTokenRetriever by lazy {
        PushTokenRetriever()
    }

    fun createProfile(customerUserId: String?, adaptyCallback: AdaptyCallback) {

        val uuid = getOrCreateProfileUUID()

        val profileRequest = CreateProfileRequest().apply {
            data = DataProfileReq().apply {
                id = uuid
                type = "adapty_analytics_profile"
                if (!customerUserId.isNullOrEmpty()) {
                    attributes = AttributeProfileReq().apply {
                        this.customerUserId = customerUserId
                    }
                }
            }
        }

        apiClient.createProfile(profileRequest, adaptyCallback)
    }

    fun updateProfile(
        email: String?,
        phoneNumber: String?,
        facebookUserId: String?,
        mixpanelUserId: String?,
        amplitudeUserId: String?,
        amplitudeDeviceId: String?,
        appmetricaProfileId: String?,
        appmetricaDeviceId: String?,
        firstName: String?,
        lastName: String?,
        gender: String?,
        birthday: String?,
        customAttributes: Map<String, Any>?,
        adaptyCallback: AdaptyCallback
    ) {

        val uuid = getOrCreateProfileUUID()

        val profileRequest = UpdateProfileRequest().apply {
            data = DataProfileReq().apply {
                id = uuid
                type = "adapty_analytics_profile"
                attributes = AttributeProfileReq().apply {
                    this.email = email
                    this.phoneNumber = phoneNumber
                    this.facebookUserId = facebookUserId
                    this.mixpanelUserId = mixpanelUserId
                    this.amplitudeUserId = amplitudeUserId
                    this.amplitudeDeviceId = amplitudeDeviceId
                    this.appmetricaProfileId = appmetricaProfileId
                    this.appmetricaDeviceId = appmetricaDeviceId
                    this.firstName = firstName
                    this.lastName = lastName
                    this.gender = gender
                    this.birthday = birthday
                    this.customAttributes = customAttributes
                }
            }
        }

        apiClient.updateProfile(profileRequest, adaptyCallback)
    }

    fun getProfile(
        adaptyCallback: AdaptyCallback
    ) {
        apiClient.getProfile(adaptyCallback)
    }

    fun getPaywalls(
        adaptyCallback: AdaptyCallback
    ) {
        apiClient.getPaywalls(adaptyCallback)
    }

    fun getPromo(
        adaptyCallback: AdaptyCallback
    ) {
        apiClient.getPromo(adaptyCallback)
    }

    fun syncMetaInstall(adaptyCallback: AdaptyCallback? = null) {

        val uuid = getOrCreateMetaUUID()

        val syncMetaRequest = SyncMetaInstallRequest().apply {
            data = DataSyncMetaReq().apply {
                id = uuid
                type = "adapty_analytics_profile_installation_meta"
                attributes = AttributeSyncMetaReq().apply {
                    adaptySdkVersion = com.adapty.BuildConfig.VERSION_NAME
                    adaptySdkVersionBuild = com.adapty.BuildConfig.VERSION_CODE
                    try {
                        with(context.packageManager.getPackageInfo(context.packageName, 0)) {
                            appBuild = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                "$longVersionCode"
                            } else {
                                "$versionCode"
                            }
                            appVersion = versionName
                        }
                    } catch (e: java.lang.Exception) {
                    }

                    device = getDeviceName()
                    deviceToken = pushToken ?: tokenRetriever.getTokenOrNull()
                    locale =
                        getCurrentLocale(context)?.let { "${it.language}_${it.country}" }
                    os = getDeviceOsVersion()
                    platform = "Android"
                    timezone = TimeZone.getDefault().id
                }
            }
        }

        val task: AsyncTask<Void?, Void?, String?> =
            @SuppressLint("StaticFieldLeak")
            object : AsyncTask<Void?, Void?, String?>() {

                override fun doInBackground(vararg params: Void?): String? {
                    var idInfo: AdvertisingIdClient.Info? = null
                    var advertId: String? = null
                    try {
                        idInfo = AdvertisingIdClient.getAdvertisingIdInfo(context)
                        advertId = idInfo!!.id
                    } catch (e: Exception) {
                    }

                    return advertId
                }

                override fun onPostExecute(advertId: String?) {
                    if (advertId != null) {
                        syncMetaRequest.data?.attributes?.advertisingId = advertId
                    }
                    apiClient.syncMeta(syncMetaRequest, adaptyCallback)
                }
            }
        task.execute()

    }

    fun validatePurchase(
        purchaseType: String,
        productId: String,
        purchaseToken: String,
        purchaseOrderId: String?,
        product: ProductModel?,
        adaptyCallback: AdaptyCallback? = null
    ) {
        val uuid = getOrCreateProfileUUID()

        val validateReceiptRequest = ValidateReceiptRequest().apply {
            data = DataValidateReceiptReq().apply {
                id = uuid
                type = "google_receipt_validation_result"
                attributes = AttributeValidateReceiptReq().apply {
                    this.profileId = uuid
                    this.productId = productId
                    this.purchaseToken = purchaseToken
                    this.isSubscription = (purchaseType == SUBS)

                    purchaseOrderId?.let {
                        this.transactionId = it
                    }

                    product?.let { p ->
                        this.variationId = p.variationId
                        this.priceLocale = p.currencyCode
                        this.originalPrice = p.skuDetails?.let {
                            formatPrice(it.priceAmountMicros)
                        }
                    }
                }
            }
        }

        apiClient.validatePurchase(validateReceiptRequest, adaptyCallback)
    }

    fun restore(purchases: ArrayList<RestoreItem>, adaptyCallback: AdaptyCallback? = null) {
        val uuid = getOrCreateProfileUUID()

        val restoreReceiptRequest = RestoreReceiptRequest().apply {
            data = DataRestoreReceiptReq().apply {
                type = "google_receipt_validation_result"
                attributes = AttributeRestoreReceiptReq().apply {
                    profileId = uuid
                    restoreItems = purchases
                }
            }
        }

        apiClient.restorePurchase(restoreReceiptRequest, adaptyCallback)
    }

    fun updateAttribution(
        attributionData: AttributeUpdateAttributionReq,
        adaptyCallback: ((error: AdaptyError?) -> Unit)?
    ) {
        getOrCreateProfileUUID()

        val updateAttributionRequest = UpdateAttributionRequest().apply {
            data = DataUpdateAttributionReq().apply {
                type = "adapty_analytics_profile_attribution"
                attributes = attributionData
            }
        }

        apiClient.updateAttribution(updateAttributionRequest, object : AdaptySystemCallback {
            override fun success(response: Any?, reqID: Int) {
                adaptyCallback?.invoke(null)
                preferenceManager.deleteAttributionData(attributionData.source)
            }

            override fun fail(error: AdaptyError, reqID: Int) {
                adaptyCallback?.invoke(error)
            }
        })
    }

    fun syncAttributions() {
        preferenceManager.getAttributionData().values.forEach { attributionData ->
            updateAttribution(attributionData, null)
        }
    }

    private fun getOrCreateProfileUUID(): String {

        return preferenceManager.profileID.takeIf { it.isNotEmpty() }
            ?: kotlin.run {
                generateUuid().toString().also { uuid ->
                    preferenceManager.profileID = uuid
                }
            }
    }

    private fun getOrCreateMetaUUID(): String {

        return preferenceManager.installationMetaID.takeIf { it.isNotEmpty() }
            ?: kotlin.run {
                generateUuid().toString().also { uuid ->
                    preferenceManager.installationMetaID = uuid
                }
            }
    }

    private fun getCurrentLocale(context: Context) =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales.get(0)
        } else {
            context.resources.configuration.locale
        }
}