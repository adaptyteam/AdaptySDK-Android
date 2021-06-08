package com.adapty.internal.data.cache

import android.os.Build
import androidx.annotation.RestrictTo
import com.adapty.internal.data.models.AttributionData
import com.adapty.internal.data.models.AwsRecordModel
import com.adapty.internal.data.models.ProductDto
import com.adapty.internal.data.models.ProfileResponseData.Attributes
import com.adapty.internal.data.models.RestoreProductInfo
import com.adapty.internal.data.models.responses.PaywallsResponse
import com.adapty.internal.data.models.responses.SyncMetaResponse
import com.adapty.internal.data.models.responses.UpdateProfileResponse
import com.adapty.internal.utils.PaywallMapper
import com.adapty.internal.utils.ProductMapper
import com.adapty.internal.utils.generateUuid
import com.adapty.models.PromoModel
import com.adapty.models.PurchaserInfoModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class CacheRepository(
    private val preferenceManager: PreferenceManager,
    private val tokenRetriever: PushTokenRetriever
) {

    private val currentPurchaserInfo = MutableSharedFlow<PurchaserInfoModel>()
    private val currentPromo = MutableSharedFlow<PromoModel>()

    private val cache = ConcurrentHashMap<String, Any>(32)

    @JvmSynthetic
    fun updateDataOnSyncMeta(attributes: SyncMetaResponse.Data.Attributes?) {
        attributes?.let { attrs ->
            attrs.iamAccessKeyId?.let(::saveIamAccessKeyId)
            attrs.iamSecretKey?.let(::saveIamSecretKey)
            attrs.iamSessionToken?.let(::saveIamSessionToken)
            attrs.profileId?.takeIf { it != getProfileId() }?.let(::saveProfileId)
        }
    }

    @JvmSynthetic
    fun updateDataOnCreateProfile(attributes: Attributes?) {
        attributes?.let { attrs ->
            attrs.profileId?.let(::saveProfileId) ?: getProfileId()?.let(::saveProfileIdOnDisk)
            attrs.customerUserId?.let(::saveCustomerUserId)
        }
    }

    @JvmSynthetic
    fun updateDataOnUpdateProfile(attributes: UpdateProfileResponse.Data.Attributes?) {
        attributes?.profileId
            ?.takeIf { it != getProfileId() }
            ?.let(::saveProfileId)
    }

    @JvmSynthetic
    suspend fun savePurchaserInfo(purchaserInfo: PurchaserInfoModel?) =
        purchaserInfo.also {
            purchaserInfo?.let { currentPurchaserInfo.emit(it) }
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
    fun getOrCreateProfileUUID() =
        getProfileId()?.takeIf(String::isNotEmpty)
            ?: generateUuid().also(::saveProfileIdInMemory)

    @JvmSynthetic
    fun getProfileId() = getString(PROFILE_ID)

    private fun saveProfileId(profileId: String) {
        saveProfileIdInMemory(profileId)
        saveProfileIdOnDisk(profileId)
    }

    private fun saveProfileIdInMemory(profileId: String) {
        cache[PROFILE_ID] = profileId
    }

    private fun saveProfileIdOnDisk(profileId: String) {
        preferenceManager.saveString(PROFILE_ID, profileId)
    }

    @JvmSynthetic
    fun getOrCreateMetaUUID() =
        getInstallationMetaId()?.takeIf(String::isNotEmpty)
            ?: generateUuid().also(::saveInstallationMetaId)

    @JvmSynthetic
    fun getCustomerUserId() = getString(CUSTOMER_USER_ID)

    private fun saveCustomerUserId(customerUserId: String) {
        cache[CUSTOMER_USER_ID] = customerUserId
        preferenceManager.saveString(CUSTOMER_USER_ID, customerUserId)
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
    }

    @JvmSynthetic
    fun clearContainersAndProducts() {
        saveContainersAndProducts(null, null)
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
    internal fun saveResponseData(map: Map<String, String>) {
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