package com.adapty.api

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageInfo
import android.os.AsyncTask
import android.os.Build
import com.adapty.Adapty
import com.adapty.Adapty.Companion.context
import com.adapty.api.entity.BaseData
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
import java.time.ZoneId
import java.util.*
import kotlin.collections.ArrayList


class ApiClientRepository(var preferenceManager: PreferenceManager) {

    private var apiClient = ApiClient(context)

    fun createProfile(customerUserId: String?, adaptyCallback: AdaptyCallback) {
        LogHelper.logVerbose(" apiclientRepository create profile")
        var uuid = preferenceManager.profileID
        if (uuid.isEmpty()) {
            uuid = generateUuid().toString()
            preferenceManager.profileID = uuid
            preferenceManager.installationMetaID = uuid
        }
        LogHelper.logVerbose(" apiclientRepository create profile uuid ok")

        val profileRequest = CreateProfileRequest()
        profileRequest.data = DataProfileReq()
        profileRequest.data?.id = uuid
        profileRequest.data?.type = "adapty_analytics_profile"
        if (!customerUserId.isNullOrEmpty()) {
            profileRequest.data?.attributes = AttributeProfileReq()
            profileRequest.data?.attributes?.customerUserId = customerUserId
        }
        LogHelper.logVerbose(" apiclientRepository create profile data added")
        apiClient.createProfile(profileRequest, adaptyCallback)
    }

    fun updateProfile(
        customerUserId: String?,
        email: String?,
        phoneNumber: String?,
        facebookUserId: String?,
        mixpanelUserId: String?,
        amplitudeUserId: String?,
        appsflyerId: String?,
        firstName: String?,
        lastName: String?,
        gender: String?,
        birthday: String?,
        adaptyCallback: AdaptyCallback
    ) {

        var uuid = preferenceManager.profileID
        if (uuid.isEmpty()) {
            uuid = generateUuid().toString()
            preferenceManager.profileID = uuid
        }

        val profileRequest = UpdateProfileRequest()
        profileRequest.data = DataProfileReq()
        profileRequest.data?.id = uuid
        profileRequest.data?.type = "adapty_analytics_profile"
        profileRequest.data?.attributes = AttributeProfileReq()
        profileRequest.data?.attributes?.apply {
            this.customerUserId = customerUserId
            this.email = email
            this.phoneNumber = phoneNumber
            this.facebookUserId = facebookUserId
            this.mixpanelUserId = mixpanelUserId
            this.amplitudeUserId = amplitudeUserId
            this.appsflyerId = appsflyerId
            this.firstName = firstName
            this.lastName = lastName
            this.gender = gender
            this.birthday = birthday
        }

        apiClient.updateProfile(profileRequest, adaptyCallback)
    }

    fun getProfile(
        adaptyCallback: AdaptyCallback
    ) {
        LogHelper.logVerbose("getPurchaserInfo() getProfile()")
        var uuid = preferenceManager.profileID
        if (uuid.isEmpty()) {
            uuid = generateUuid().toString()
            preferenceManager.profileID = uuid
        }
        LogHelper.logVerbose("getPurchaserInfo() getProfile() uuid ok")
        val purchaserInfoRequest = PurchaserInfoRequest()
        purchaserInfoRequest.data = BaseData()
        purchaserInfoRequest.data?.id = uuid
        LogHelper.logVerbose("getPurchaserInfo() getProfile() go to ApiClient")

        apiClient.getProfile(purchaserInfoRequest, adaptyCallback)
    }

    fun getPurchaseContainers(
        adaptyCallback: AdaptyCallback
    ) {
        var uuid = preferenceManager.profileID
        if (uuid.isEmpty()) {
            uuid = generateUuid().toString()
            preferenceManager.profileID = uuid
        }

        val request = PurchaseContainersRequest()
        request.data = BaseData()
        request.data?.id = uuid

        apiClient.getPurchaseContainers(request, adaptyCallback)
    }

    fun syncMetaInstall(applicationContext: Context, adaptyCallback: AdaptyCallback? = null) {
        LogHelper.logVerbose("sendSyncMetaInstallRequest Enter")
        var uuid = preferenceManager.profileID
        LogHelper.logVerbose("sendSyncMetaInstallRequest uuid: ${uuid ?: ""}")
        if (uuid.isEmpty()) {
            LogHelper.logVerbose("sendSyncMetaInstallRequest uuid isEmpty")
            uuid = generateUuid().toString()
            preferenceManager.profileID = uuid
            preferenceManager.installationMetaID = uuid
        }

        val syncMetaRequest = SyncMetaInstallRequest()
        syncMetaRequest.data = DataSyncMetaReq()
        syncMetaRequest.data?.id = uuid
        syncMetaRequest.data?.type = "adapty_analytics_profile_installation_meta"
        syncMetaRequest.data?.attributes = AttributeSyncMetaReq()
        LogHelper.logVerbose("sendSyncMetaInstallRequest Attribute Created")
        syncMetaRequest.data?.attributes?.apply {
            adaptySdkVersion = com.adapty.BuildConfig.VERSION_NAME
            adaptySdkVersionBuild = ADAPTY_SDK_VERSION_INT
            LogHelper.logVerbose("sendSyncMetaInstallRequest Attribute Sdk version added")
            try {
                applicationContext.applicationContext?.let { ctx ->
                    val mainPackageName = applicationContext.applicationContext?.packageName
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
                LogHelper.logVerbose("sendSyncMetaInstallRequest Attribute added version")
            } catch (e : java.lang.Exception) {
                LogHelper.logVerbose("sendSyncMetaInstallRequest catch ${e.message}, ${e.localizedMessage}")
            }

            device = getDeviceName()
            locale = Locale.getDefault().toLanguageTag()
            os = getDeviceOsVersion()
            platform = "Android"
            timezone = TimeZone.getDefault().id
            LogHelper.logVerbose("sendSyncMetaInstallRequest Attribute all info added")
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
                        LogHelper.logVerbose("sendSyncMetaInstallRequest advertising catch: ${e.message}, ${e.localizedMessage}")
                    }

                    return advertId
                }

                override fun onPostExecute(advertId: String?) {
                    LogHelper.logVerbose("sendSyncMetaInstallRequest advertId get: ${advertId ?: ""}")
                    if (advertId != null) {
                        syncMetaRequest.data?.attributes?.advertisingId = advertId
                    }
                    LogHelper.logVerbose("sendSyncMetaInstallRequest go to apiCLient")
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
        var uuid = preferenceManager.profileID
        if (uuid.isEmpty()) {
            uuid = generateUuid().toString()
            preferenceManager.profileID = uuid
            preferenceManager.installationMetaID = uuid
        }

        val validateReceiptRequest = ValidateReceiptRequest()
        validateReceiptRequest.data = DataValidateReceiptReq()
        validateReceiptRequest.data?.id = uuid
        validateReceiptRequest.data?.type = "google_receipt_validation_result"
        validateReceiptRequest.data?.attributes = AttributeValidateReceiptReq()
        validateReceiptRequest.data?.attributes?.apply {
            profileId = uuid
            this.productId = productId
            this.purchaseToken = purchaseToken
            isSubscription = (purchaseType == SUBS)

            purchaseOrderId?.let {
                transactionId = it
            }

            product?.let { p ->
                variationId = p.variationId
                priceLocale = p.currencyCode
                originalPrice = p.price
            }
        }


        apiClient.validatePurchase(validateReceiptRequest, adaptyCallback)
    }

    fun restore(purchases: ArrayList<RestoreItem>, adaptyCallback: AdaptyCallback? = null) {
        var uuid = preferenceManager.profileID
        if (uuid.isEmpty()) {
            uuid = generateUuid().toString()
            preferenceManager.profileID = uuid
            preferenceManager.installationMetaID = uuid
        }

        val restoreReceiptRequest = RestoreReceiptRequest()
        restoreReceiptRequest.data = DataRestoreReceiptReq()
        restoreReceiptRequest.data?.type = "google_receipt_validation_result"
        restoreReceiptRequest.data?.attributes = AttributeRestoreReceiptReq()
        restoreReceiptRequest.data?.attributes?.profileId = uuid
        restoreReceiptRequest.data?.attributes?.restoreItems = purchases

        apiClient.restorePurchase(restoreReceiptRequest, adaptyCallback)
    }

    companion object Factory {

        private lateinit var instance: ApiClientRepository

        @Synchronized
        fun getInstance(preferenceManager: PreferenceManager): ApiClientRepository {
            if (!::instance.isInitialized)
                instance = ApiClientRepository(preferenceManager)

            return instance
        }
    }
}