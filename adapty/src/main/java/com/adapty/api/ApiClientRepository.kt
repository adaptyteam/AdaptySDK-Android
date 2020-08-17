package com.adapty.api

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageInfo
import android.os.AsyncTask
import android.os.Build
import com.adapty.Adapty.Companion.context
import com.adapty.api.entity.BaseData
import com.adapty.api.entity.attribution.AttributeUpdateAttributionReq
import com.adapty.api.entity.attribution.DataUpdateAttributionReq
import com.adapty.api.entity.containers.Product
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
import com.adapty.purchase.SUBS
import com.adapty.utils.*
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.gson.Gson
import org.json.JSONObject
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import kotlin.collections.HashMap


class ApiClientRepository(var preferenceManager: PreferenceManager, private val gson : Gson) {

    private var apiClient = ApiClient(context, gson)

    private val priceFormatter by lazy {
        DecimalFormat("0.00", DecimalFormatSymbols(Locale.US))
    }

    fun createProfile(customerUserId: String?, adaptyCallback: AdaptyCallback) {

        val uuid = getOrCreateUUID()

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
        customerUserId: String?,
        email: String?,
        phoneNumber: String?,
        facebookUserId: String?,
        mixpanelUserId: String?,
        amplitudeUserId: String?,
        amplitudeDeviceId: String?,
        appsflyerId: String?,
        appmetricaProfileId: String? = null,
        appmetricaDeviceId: String? = null,
        firstName: String?,
        lastName: String?,
        gender: String?,
        birthday: String?,
        customAttributes: Map<String, Any>?,
        adaptyCallback: AdaptyCallback
    ) {

        val uuid = getOrCreateUUID(false)

        val profileRequest = UpdateProfileRequest().apply {
            data = DataProfileReq().apply {
                id = uuid
                type = "adapty_analytics_profile"
                attributes = AttributeProfileReq().apply {
                    this.customerUserId = customerUserId
                    this.email = email
                    this.phoneNumber = phoneNumber
                    this.facebookUserId = facebookUserId
                    this.mixpanelUserId = mixpanelUserId
                    this.amplitudeUserId = amplitudeUserId
                    this.amplitudeDeviceId = amplitudeDeviceId
                    this.appsflyerId = appsflyerId
                    this.firstName = firstName
                    this.lastName = lastName
                    this.gender = gender
                    this.birthday = birthday
                    this.appmetricaProfileId = appmetricaProfileId
                    this.appmetricaDeviceId = appmetricaDeviceId
                    customAttributes?.let {
                        this.customAttributes = gson.toJson(it)
                    }
                }
            }
        }

        apiClient.updateProfile(profileRequest, adaptyCallback)
    }

    fun getProfile(
        adaptyCallback: AdaptyCallback
    ) {
        val uuid = getOrCreateUUID(false)

        val purchaserInfoRequest = PurchaserInfoRequest().apply {
            data = BaseData().apply {
                id = uuid
            }
        }

        apiClient.getProfile(purchaserInfoRequest, adaptyCallback)
    }

    fun getPaywalls(
        adaptyCallback: AdaptyCallback
    ) {
        val uuid = getOrCreateUUID(false)

        val request = PaywallsRequest().apply {
            data = BaseData().apply {
                id = uuid
            }
        }

        apiClient.getPaywalls(request, adaptyCallback)
    }

    fun syncMetaInstall(adaptyCallback: AdaptyCallback? = null) {

        val uuid = getOrCreateUUID()

        val syncMetaRequest = SyncMetaInstallRequest().apply {
            data = DataSyncMetaReq().apply {
                id = uuid
                type = "adapty_analytics_profile_installation_meta"
                attributes = AttributeSyncMetaReq().apply {
                    adaptySdkVersion = com.adapty.BuildConfig.VERSION_NAME
                    adaptySdkVersionBuild = ADAPTY_SDK_VERSION_INT
                    try {
                        context.applicationContext?.let { ctx ->
                            val mainPackageName = ctx.packageName
                            val packageInfo: PackageInfo =
                                ctx.packageManager.getPackageInfo(mainPackageName, 0)
                            val versionCode: Long =
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                    packageInfo.longVersionCode
                                } else {
                                    packageInfo.versionCode.toLong()
                                }
                            appBuild = versionCode.toString()
                            appVersion = packageInfo.versionName
                        }
                    } catch (e: java.lang.Exception) {
                    }

                    device = getDeviceName()
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
        product: Product?,
        adaptyCallback: AdaptyCallback? = null
    ) {
        val uuid = getOrCreateUUID()

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
                            priceFormatter.format(
                                BigDecimal.valueOf(it.priceAmountMicros)
                                    .divide(BigDecimal.valueOf(1_000_000L))
                            )
                        }
                    }
                }
            }
        }

        apiClient.validatePurchase(validateReceiptRequest, adaptyCallback)
    }

    fun restore(purchases: ArrayList<RestoreItem>, adaptyCallback: AdaptyCallback? = null) {
        val uuid = getOrCreateUUID()

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
        attribution: Any,
        source: String,
        networkUserId: String?,
        adaptyCallback: AdaptyCallback? = null
    ) {
        getOrCreateUUID()

        val updateAttributionRequest = UpdateAttributionRequest().apply {
            data = DataUpdateAttributionReq().apply {
                type = "adapty_analytics_profile_attribution"
                attributes = AttributeUpdateAttributionReq().apply {
                    this.source = source
                    this.attribution = if (attribution is JSONObject) {
                        val jo = HashMap<String, Any>()
                        for (a in attribution.keys()) {
                            jo[a] = attribution.get(a)
                        }
                        jo
                    } else {
                        attribution
                    }

                    this.networkUserId = networkUserId
                }
            }
        }

        apiClient.updateAttribution(updateAttributionRequest, adaptyCallback)
    }

    private fun getOrCreateUUID(updateMetaId: Boolean = true): String {
        return preferenceManager.profileID.takeIf { it.isNotEmpty() }
            ?: kotlin.run {
                generateUuid().toString().also { uuid ->
                    preferenceManager.profileID = uuid
                    if (updateMetaId)
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