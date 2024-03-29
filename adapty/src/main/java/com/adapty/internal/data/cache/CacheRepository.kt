package com.adapty.internal.data.cache

import androidx.annotation.RestrictTo
import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.data.models.*
import com.adapty.internal.utils.Logger
import com.adapty.internal.utils.generateUuid
import com.adapty.utils.AdaptyLogLevel.Companion.ERROR
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.locks.ReentrantReadWriteLock

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class CacheRepository(
    private val preferenceManager: PreferenceManager,
    private val responseCacheKeyProvider: ResponseCacheKeyProvider,
    private val gson: Gson,
) {

    private val currentProfile = MutableSharedFlow<ProfileDto>()

    private val cache = ConcurrentHashMap<String, Any>(32)

    @JvmSynthetic
    suspend fun updateDataOnCreateProfile(
        profile: ProfileDto,
        installationMeta: InstallationMeta,
    ): Boolean {
        if ((profile.timestamp ?: 0L) < (getProfile()?.timestamp ?: 0L))
            return false
        var profileIdHasChanged = false
        (profile.profileId ?: (cache[UNSYNCED_PROFILE_ID] as? String))?.let { profileId ->
            profileIdHasChanged = profileId != preferenceManager.getString(PROFILE_ID)

            if (profileIdHasChanged) onNewProfileIdReceived(profileId)
        }
        if (profileIdHasChanged || (getCustomerUserId() != profile.customerUserId)) {
            clearSyncedPurchases()
        }
        profile.customerUserId?.let(::saveCustomerUserId)
        saveProfile(profile)

        cache.remove(UNSYNCED_PROFILE_ID)
        val currentUnsyncedCUIdDiffers = profile.customerUserId != cache[UNSYNCED_CUSTOMER_USER_ID]
        if (!currentUnsyncedCUIdDiffers)
            cache.remove(UNSYNCED_CUSTOMER_USER_ID)
        saveLastSentInstallationMeta(installationMeta)
        return profileIdHasChanged
    }

    @JvmSynthetic
    suspend fun updateOnProfileReceived(
        profile: ProfileDto,
        profileIdWhenRequestSent: String?,
    ): ProfileDto {
        if (profileIdWhenRequestSent != null && getProfileId() != profileIdWhenRequestSent && (profile.timestamp ?: 0L) < (getProfile()?.timestamp ?: 0L)) {
            return profile
        }

        return saveProfile(profile)
    }

    private suspend fun saveProfile(profile: ProfileDto) =
        profile.also {
            currentProfile.emit(profile)
            saveData(PROFILE, profile)
        }

    @JvmSynthetic
    fun subscribeOnProfileChanges() =
        currentProfile
            .distinctUntilChanged()

    @JvmSynthetic
    fun getAppKey() = preferenceManager.getString(APP_KEY)

    @JvmSynthetic
    fun saveAppKey(appKey: String) {
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
        clearData(
            containsKeys = setOf(
                PROFILE,
                SYNCED_PURCHASES,
                PURCHASES_HAVE_BEEN_SYNCED,
                APP_OPENED_TIME,
                PRODUCT_RESPONSE,
                PRODUCT_RESPONSE_HASH,
                PROFILE_RESPONSE,
                PROFILE_RESPONSE_HASH,
            ),
            startsWithKeys = setOf(PAYWALL_RESPONSE_START_PART),
        )
        saveProfileId(newProfileId)
    }

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

    private val installationMetaLock = ReentrantReadWriteLock()

    @JvmSynthetic
    fun getInstallationMetaId(): String {
        try {
            installationMetaLock.readLock().lock()
            val id = getString(INSTALLATION_META_ID)
            if (!id.isNullOrEmpty()) {
                return id
            }
        } finally {
            installationMetaLock.readLock().unlock()
        }

        try {
            installationMetaLock.writeLock().lock()
            val id = getString(INSTALLATION_META_ID)
            if (!id.isNullOrEmpty()) {
                return id
            }

            return generateUuid().also(::saveInstallationMetaId)
        } finally {
            installationMetaLock.writeLock().unlock()
        }
    }

    private fun saveInstallationMetaId(installationMetaId: String) {
        cache[INSTALLATION_META_ID] = installationMetaId
        preferenceManager.saveString(INSTALLATION_META_ID, installationMetaId)
    }

    @JvmSynthetic
    fun getInstallationMeta() =
        getData(LAST_SENT_INSTALLATION_META, InstallationMeta::class.java)

    @JvmSynthetic
    fun saveLastSentInstallationMeta(installationMeta: InstallationMeta) {
        saveData(LAST_SENT_INSTALLATION_META, installationMeta)
    }

    @JvmSynthetic
    fun getPurchasesHaveBeenSynced() =
        cache.safeGetOrPut(
            PURCHASES_HAVE_BEEN_SYNCED,
            { preferenceManager.getBoolean(PURCHASES_HAVE_BEEN_SYNCED, false) }) as? Boolean
            ?: false

    @JvmSynthetic
    fun setPurchasesHaveBeenSynced(synced: Boolean) {
        cache[PURCHASES_HAVE_BEEN_SYNCED] = synced
        preferenceManager.saveBoolean(PURCHASES_HAVE_BEEN_SYNCED, synced)
    }

    @JvmSynthetic
    fun getExternalAnalyticsEnabled() =
        cache.safeGetOrPut(
            EXTERNAL_ANALYTICS_ENABLED,
            { preferenceManager.getBoolean(EXTERNAL_ANALYTICS_ENABLED, null) }) as? Boolean

    @JvmSynthetic
    fun saveExternalAnalyticsEnabled(enabled: Boolean) {
        cache[EXTERNAL_ANALYTICS_ENABLED] = enabled
        preferenceManager.saveBoolean(EXTERNAL_ANALYTICS_ENABLED, enabled)
    }

    @JvmSynthetic
    fun getLastAppOpenedTime() =
        cache.safeGetOrPut(
            APP_OPENED_TIME,
            { preferenceManager.getLong(APP_OPENED_TIME, 0L) }) as? Long ?: 0L

    @JvmSynthetic
    fun saveLastAppOpenedTime(timeMillis: Long) {
        cache[APP_OPENED_TIME] = timeMillis
        preferenceManager.saveLong(APP_OPENED_TIME, timeMillis)
    }

    @JvmSynthetic
    fun getProfile() =
        getData(PROFILE, ProfileDto::class.java)

    @JvmSynthetic
    fun getFallbackPaywalls() = cache[FALLBACK_PAYWALLS] as? FallbackPaywalls

    @JvmSynthetic
    fun getSyncedPurchases() =
        getData<HashSet<SyncedPurchase>>(SYNCED_PURCHASES).orEmpty()

    @JvmSynthetic
    fun saveSyncedPurchases(data: Set<SyncedPurchase>) {
        saveData(SYNCED_PURCHASES, data)
    }

    @JvmSynthetic
    fun getAnalyticsData(isSystemLog: Boolean) =
        getData<AnalyticsData>(getAnalyticsKey(isSystemLog)) ?: AnalyticsData.DEFAULT

    @JvmSynthetic
    fun saveAnalyticsData(data: AnalyticsData, isSystemLog: Boolean) {
        saveData(getAnalyticsKey(isSystemLog), data)
    }

    private fun getAnalyticsKey(isSystemLog: Boolean) =
        if (isSystemLog) ANALYTICS_LOW_PRIORITY_DATA else ANALYTICS_DATA

    @Volatile
    var analyticsConfig = AnalyticsConfig.DEFAULT

    fun getPaywall(id: String, maxAgeMillis: Long? = null): PaywallDto? {
        return getData<CacheEntity<PaywallDto>>(getPaywallCacheKey(id))?.let { (paywall, cachedAt) ->
            paywall.takeIf { (maxAgeMillis == null) || (System.currentTimeMillis() - cachedAt <= maxAgeMillis) }
        }
    }

    fun savePaywall(id: String, paywallDto: PaywallDto) {
        saveData(getPaywallCacheKey(id), CacheEntity(paywallDto))
    }

    private fun getPaywallCacheKey(id: String) =
        "$PAYWALL_RESPONSE_START_PART${id}$PAYWALL_RESPONSE_END_PART"

    @JvmSynthetic
    fun saveFallbackPaywalls(paywalls: String): AdaptyError? =
        try {
            cache[FALLBACK_PAYWALLS] = gson.fromJson(paywalls, FallbackPaywalls::class.java).also { fallbackPaywalls ->
                val version = fallbackPaywalls.version
                if (version < CURRENT_FALLBACK_PAYWALL_VERSION) {
                    Logger.log(ERROR) { "The fallback paywalls version is not correct. Download a new one from the Adapty Dashboard." }
                } else if (version > CURRENT_FALLBACK_PAYWALL_VERSION) {
                    Logger.log(ERROR) { "The fallback paywalls version is not correct. Please update the AdaptySDK." }
                }
            }
            null
        } catch (e: Exception) {
            Logger.log(ERROR) { "Couldn't set fallback paywalls. $e" }
            AdaptyError(
                originalError = e,
                message = "Couldn't set fallback paywalls. Invalid JSON",
                adaptyErrorCode = AdaptyErrorCode.INVALID_JSON
            )
        }

    @JvmSynthetic
    fun clearOnLogout() {
        clearData(
            containsKeys = setOf(
                CUSTOMER_USER_ID,
                PROFILE_ID,
                PROFILE,
                SYNCED_PURCHASES,
                PURCHASES_HAVE_BEEN_SYNCED,
                APP_OPENED_TIME,
                PRODUCT_RESPONSE,
                PRODUCT_RESPONSE_HASH,
                PROFILE_RESPONSE,
                PROFILE_RESPONSE_HASH,
            ),
            startsWithKeys = setOf(PAYWALL_RESPONSE_START_PART),
        )
    }

    @JvmSynthetic
    fun clearSyncedPurchases() {
        clearData(
            containsKeys = setOf(
                SYNCED_PURCHASES,
                PURCHASES_HAVE_BEEN_SYNCED,
            ),
            startsWithKeys = setOf(),
        )
    }

    @JvmSynthetic
    fun clearOnAppKeyChanged() {
        clearData(
            containsKeys = setOf(
                CUSTOMER_USER_ID,
                PROFILE_ID,
                PROFILE,
                SYNCED_PURCHASES,
                PURCHASES_HAVE_BEEN_SYNCED,
                APP_OPENED_TIME,
                PRODUCT_RESPONSE,
                PRODUCT_RESPONSE_HASH,
                PRODUCT_IDS_RESPONSE,
                PRODUCT_IDS_RESPONSE_HASH,
                PROFILE_RESPONSE,
                PROFILE_RESPONSE_HASH,
                ANALYTICS_DATA,
                YET_UNPROCESSED_VALIDATE_PRODUCT_INFO,
                EXTERNAL_ANALYTICS_ENABLED,
            ),
            startsWithKeys = setOf(PAYWALL_RESPONSE_START_PART),
        )
    }

    private fun clearData(containsKeys: Set<String>, startsWithKeys: Set<String>) {
        val keysToRemove = preferenceManager.getKeysToRemove(containsKeys, startsWithKeys)

        cache.apply { keysToRemove.forEach(::remove) }
        preferenceManager.clearData(keysToRemove)
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

    @JvmSynthetic
    fun getSessionId() = cache.safeGetOrPut(SESSION_ID) { generateUuid() } as? String

    private inline fun <reified T> getData(key: String, classOfT: Class<T>? = null): T? =
        cache.safeGetOrPut(key, { preferenceManager.getData<T>(key, classOfT) }) as? T

    private fun saveData(key: String, data: Any?) {
        data?.let { cache[key] = it } ?: cache.remove(key)
        preferenceManager.saveData(key, data)
    }

    private inline fun <K, V> ConcurrentMap<K, V>.safeGetOrPut(key: K, defaultValue: () -> V): V? {
        return get(key) ?: defaultValue()?.let { default -> putIfAbsent(key, default) ?: default }
    }

    private companion object {
        private const val CURRENT_FALLBACK_PAYWALL_VERSION = 4
    }
}