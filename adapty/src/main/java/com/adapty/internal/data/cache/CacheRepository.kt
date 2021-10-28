package com.adapty.internal.data.cache

import android.os.Build
import androidx.annotation.RestrictTo
import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.data.models.*
import com.adapty.internal.data.models.ProfileResponseData.Attributes
import com.adapty.internal.data.models.responses.PaywallsResponse
import com.adapty.internal.data.models.responses.SyncMetaResponse
import com.adapty.internal.utils.*
import com.adapty.models.PromoModel
import com.adapty.models.PurchaserInfoModel
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.atomic.AtomicBoolean

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class CacheRepository(
    private val preferenceManager: PreferenceManager,
    private val tokenRetriever: PushTokenRetriever,
    private val gson: Gson,
) {

    private val currentPurchaserInfo = MutableSharedFlow<PurchaserInfoModel>()
    private val currentPromo = MutableSharedFlow<PromoModel>()

    private val cache = ConcurrentHashMap<String, Any>(32)

    @JvmSynthetic
    @JvmField
    val arePaywallsReceivedFromBackend = AtomicBoolean(false)

    @JvmSynthetic
    @JvmField
    val canFallbackPaywallsBeSet = AtomicBoolean(true)

    @JvmSynthetic
    fun updateDataOnSyncMeta(attributes: SyncMetaResponse.Data.Attributes?) {
        attributes?.let { attrs ->
            attrs.iamAccessKeyId?.let(::saveIamAccessKeyId)
            attrs.iamSecretKey?.let(::saveIamSecretKey)
            attrs.iamSessionToken?.let(::saveIamSessionToken)
        }
    }

    @JvmSynthetic
    suspend fun updateDataOnCreateProfile(attributes: Attributes?) {
        attributes?.let { attrs ->
            (attrs.profileId ?: (cache[UNSYNCED_PROFILE_ID] as? String))
                ?.let(::onNewProfileIdReceived)
            attrs.customerUserId?.let(::saveCustomerUserId)
            savePurchaserInfo(PurchaserInfoMapper.map(attrs))

            cache.remove(UNSYNCED_PROFILE_ID)
            cache.remove(UNSYNCED_CUSTOMER_USER_ID)
        }
    }

    @JvmSynthetic
    suspend fun updateOnPurchaserInfoReceived(attrs: ContainsPurchaserInfo?): PurchaserInfoModel? {
        attrs?.profileId?.takeIf { it != getProfileId() }?.let(::onNewProfileIdReceived)
        return attrs?.let { savePurchaserInfo(PurchaserInfoMapper.map(attrs)) }
    }

    private suspend fun savePurchaserInfo(purchaserInfo: PurchaserInfoModel) =
        purchaserInfo.also {
            currentPurchaserInfo.emit(purchaserInfo)
            saveData(PURCHASER_INFO, purchaserInfo)
        }

    @JvmSynthetic
    suspend fun setCurrentPromo(promo: PromoModel) =
        promo.also { currentPromo.emit(promo) }

    @JvmSynthetic
    fun subscribeOnPurchaserInfoChanges() =
        currentPurchaserInfo
            .distinctUntilChanged()

    @JvmSynthetic
    fun subscribeOnPromoChanges() =
        currentPromo
            .distinctUntilChanged()

    @JvmSynthetic
    fun getAppKey() = getString(APP_KEY)

    @JvmSynthetic
    fun saveAppKey(appKey: String) {
        cache[APP_KEY] = appKey
        preferenceManager.saveString(APP_KEY, appKey)
    }

    @JvmSynthetic
    fun getProfileId() = ((cache[UNSYNCED_PROFILE_ID] as? String) ?: getString(PROFILE_ID))
        ?.takeIf(String::isNotEmpty) ?: generateUuid().also { cache[UNSYNCED_PROFILE_ID] = it }

    private fun saveProfileId(profileId: String) {
        cache[PROFILE_ID] = profileId
        preferenceManager.saveString(PROFILE_ID, profileId)
    }

    private fun onNewProfileIdReceived(newProfileId: String) {
        clearCachedRequestData()
        saveProfileId(newProfileId)
    }

    @JvmSynthetic
    fun getOrCreateMetaUUID() =
        getInstallationMetaId()?.takeIf(String::isNotEmpty)
            ?: generateUuid().also(::saveInstallationMetaId)

    @JvmSynthetic
    fun getCustomerUserId() = getString(CUSTOMER_USER_ID)

    @JvmSynthetic
    fun getUnsyncedAuthData(): Pair<String?, String?> {
        return (cache[UNSYNCED_PROFILE_ID] as? String) to (cache[UNSYNCED_CUSTOMER_USER_ID] as? String)
    }

    private fun saveCustomerUserId(customerUserId: String) {
        cache[CUSTOMER_USER_ID] = customerUserId
        preferenceManager.saveString(CUSTOMER_USER_ID, customerUserId)
    }

    @JvmSynthetic
    fun prepareCustomerUserIdToSync(newCustomerUserId: String?) {
        if (newCustomerUserId.isNullOrBlank()) {
            cache.remove(UNSYNCED_CUSTOMER_USER_ID)
        } else if (getString(CUSTOMER_USER_ID) != newCustomerUserId) {
            cache[UNSYNCED_CUSTOMER_USER_ID] = newCustomerUserId
        }
    }

    @JvmSynthetic
    fun prepareProfileIdToSync() {
        if (cache[UNSYNCED_PROFILE_ID] == null && getString(PROFILE_ID).isNullOrEmpty()) {
            cache[UNSYNCED_PROFILE_ID] = generateUuid()
        }
    }

    @JvmSynthetic
    fun getInstallationMetaId() = getString(INSTALLATION_META_ID)

    private fun saveInstallationMetaId(installationMetaId: String) {
        cache[INSTALLATION_META_ID] = installationMetaId
        preferenceManager.saveString(INSTALLATION_META_ID, installationMetaId)
    }

    @JvmSynthetic
    fun getIamSessionToken() = getString(IAM_SESSION_TOKEN)

    private fun saveIamSessionToken(iamSessionToken: String) {
        cache[IAM_SESSION_TOKEN] = iamSessionToken
        preferenceManager.saveString(IAM_SESSION_TOKEN, iamSessionToken)
    }

    @JvmSynthetic
    fun getIamSecretKey() = getString(IAM_SECRET_KEY)

    private fun saveIamSecretKey(iamSecretKey: String) {
        cache[IAM_SECRET_KEY] = iamSecretKey
        preferenceManager.saveString(IAM_SECRET_KEY, iamSecretKey)
    }

    @JvmSynthetic
    fun getIamAccessKeyId() = getString(IAM_ACCESS_KEY_ID)

    private fun saveIamAccessKeyId(iamAccessKeyId: String) {
        cache[IAM_ACCESS_KEY_ID] = iamAccessKeyId
        preferenceManager.saveString(IAM_ACCESS_KEY_ID, iamAccessKeyId)
    }

    @JvmSynthetic
    fun getExternalAnalyticsEnabled() =
        cache.safeGetOrPut(
            EXTERNAL_ANALYTICS_ENABLED,
            { preferenceManager.getBoolean(EXTERNAL_ANALYTICS_ENABLED, true) }) as? Boolean ?: true

    @JvmSynthetic
    fun saveExternalAnalyticsEnabled(enabled: Boolean) {
        cache[EXTERNAL_ANALYTICS_ENABLED] = enabled
        preferenceManager.saveBoolean(EXTERNAL_ANALYTICS_ENABLED, enabled)
    }

    @JvmSynthetic
    fun getPurchaserInfo() =
        getData<PurchaserInfoModel>(PURCHASER_INFO, PurchaserInfoModel::class.java)

    @JvmSynthetic
    fun getContainers() = getData<ArrayList<PaywallsResponse.Data>>(CONTAINERS)

    @JvmSynthetic
    fun getPaywalls() = getContainers()?.let(PaywallMapper::map)

    @JvmSynthetic
    fun getFallbackPaywalls() = (cache[FALLBACK_PAYWALLS] as? PaywallsResponse)?.let { fallback ->
        Pair(fallback.data ?: arrayListOf(), fallback.meta?.products ?: arrayListOf())
    }

    @JvmSynthetic
    fun getProducts() = getData<ArrayList<ProductDto>>(PRODUCTS)

    @JvmSynthetic
    fun getSyncedPurchases() =
        getData<ArrayList<RestoreProductInfo>>(SYNCED_PURCHASES) ?: arrayListOf()

    @JvmSynthetic
    fun saveSyncedPurchases(data: ArrayList<RestoreProductInfo>) {
        saveData(SYNCED_PURCHASES, data)
    }

    @JvmSynthetic
    fun getKinesisRecords() = getData<ArrayList<AwsRecordModel>>(KINESIS_RECORDS) ?: arrayListOf()

    @JvmSynthetic
    fun saveKinesisRecords(data: ArrayList<AwsRecordModel>) {
        saveData(KINESIS_RECORDS, data)
    }

    @JvmSynthetic
    fun getAttributionData(): MutableMap<String, AttributionData> =
        getData<MutableMap<String, AttributionData>>(ATTRIBUTION_DATA) ?: mutableMapOf()

    @JvmSynthetic
    fun saveAttributionData(attributionData: AttributionData) {
        attributionData.source.let { key ->
            getAttributionData().let {
                it[key] = attributionData
                saveData(ATTRIBUTION_DATA, it)
            }
        }
    }

    @JvmSynthetic
    fun deleteAttributionData(key: String?) {
        key?.let {
            getAttributionData().let {
                it.remove(key)
                saveData(ATTRIBUTION_DATA, it)
            }
        }
    }

    @JvmSynthetic
    fun getPaywallsAndProducts() = Pair(getPaywalls(), getProducts()?.map(ProductMapper::map))

    @JvmSynthetic
    fun saveContainersAndProducts(
        containers: ArrayList<PaywallsResponse.Data>?,
        products: ArrayList<ProductDto>?
    ) {
        containers?.let {
            cache[CONTAINERS] = it
        } ?: cache.remove(CONTAINERS)
        products?.let {
            cache[PRODUCTS] = it
        } ?: cache.remove(PRODUCTS)
        preferenceManager.saveContainersAndProducts(containers, products)
        arePaywallsReceivedFromBackend.set(true)
    }

    fun saveFallbackPaywalls(paywalls: String): AdaptyError? =
        if (canFallbackPaywallsBeSet.get() && getContainers() == null) {
            try {
                cache[FALLBACK_PAYWALLS] = gson.fromJson(paywalls, PaywallsResponse::class.java)
                null
            } catch (e: JsonSyntaxException) {
                Logger.logError { "Couldn't set fallback paywalls. $e" }
                AdaptyError(
                    originalError = e,
                    message = "Couldn't set fallback paywalls. Invalid JSON",
                    adaptyErrorCode = AdaptyErrorCode.INVALID_JSON
                )
            }
        } else {
            val errorMessage = "Fallback paywalls are not required"
            Logger.logError { errorMessage }
            AdaptyError(
                message = errorMessage,
                adaptyErrorCode = AdaptyErrorCode.FALLBACK_PAYWALLS_NOT_REQUIRED
            )
        }

    @JvmSynthetic
    fun clearOnLogout() {
        cache.apply {
            remove(CUSTOMER_USER_ID)
            remove(INSTALLATION_META_ID)
            remove(PROFILE_ID)
            remove(CONTAINERS)
            remove(PRODUCTS)
            remove(SYNCED_PURCHASES)
            remove(PURCHASER_INFO)
            remove(IAM_ACCESS_KEY_ID)
            remove(IAM_SECRET_KEY)
            remove(IAM_SESSION_TOKEN)
        }
        preferenceManager.clearOnLogout()
        arePaywallsReceivedFromBackend.set(false)
    }

    private fun clearCachedRequestData() {
        cache.apply {
            remove(UPDATE_PROFILE_REQUEST_KEY)
            remove(UPDATE_ADJUST_REQUEST_KEY)
            remove(UPDATE_APPSFLYER_REQUEST_KEY)
            remove(UPDATE_BRANCH_REQUEST_KEY)
            remove(UPDATE_CUSTOM_ATTRIBUTION_REQUEST_KEY)
            remove(SYNC_META_REQUEST_KEY)
        }
        preferenceManager.clearCachedRequestData()
    }

    @JvmSynthetic
    fun getPushToken() =
        cache.safeGetOrPut(PUSH_TOKEN, { tokenRetriever.getTokenOrNull() }) as? String

    @JvmSynthetic
    fun savePushToken(token: String) {
        cache[PUSH_TOKEN] = token
    }

    @get:JvmSynthetic
    val deviceName: String by lazy {
        (if (Build.MODEL.startsWith(Build.MANUFACTURER)) Build.MODEL else "${Build.MANUFACTURER} ${Build.MODEL}")
            .capitalize(Locale.ENGLISH)
    }

    @JvmSynthetic
    internal fun getString(key: String) =
        cache.safeGetOrPut(key, { preferenceManager.getString(key) }) as? String

    @JvmSynthetic
    internal fun saveRequestOrResponseLatestData(map: Map<String, String>) {
        map.forEach { (key, value) ->
            cache[key] = value
        }
        preferenceManager.saveStrings(map)
    }

    private inline fun <reified T> getData(key: String, classOfT: Class<T>? = null): T? =
        cache.safeGetOrPut(key, { preferenceManager.getData<T>(key, classOfT) }) as? T

    private fun saveData(key: String, data: Any?) {
        data?.let { cache[key] = it } ?: cache.remove(key)
        preferenceManager.saveData(key, data)
    }

    private inline fun <K, V> ConcurrentMap<K, V>.safeGetOrPut(key: K, defaultValue: () -> V): V? {
        return get(key) ?: defaultValue()?.let { default -> putIfAbsent(key, default) ?: default }
    }
}