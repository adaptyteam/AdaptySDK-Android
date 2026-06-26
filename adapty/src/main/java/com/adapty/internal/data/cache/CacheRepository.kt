@file:OptIn(InternalAdaptyApi::class)

package com.adapty.internal.data.cache

import androidx.annotation.RestrictTo
import com.adapty.errors.AdaptyError
import com.adapty.internal.data.models.*
import com.adapty.internal.data.models.requests.ValidateReceiptRequest
import com.adapty.internal.domain.VariationType
import com.adapty.internal.domain.models.IdentityParams
import com.adapty.internal.utils.FallbackPaywallRetriever
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.ProfileStateChange
import com.adapty.internal.utils.combinedProductId
import com.adapty.internal.utils.execute
import com.adapty.internal.utils.extractLanguageCode
import com.adapty.internal.utils.generateUuid
import com.adapty.internal.utils.getAs
import com.adapty.internal.utils.getLanguageCode
import com.adapty.internal.utils.orDefault
import com.adapty.internal.utils.unlockQuietly
import com.adapty.internal.utils.withLockSafe
import com.adapty.models.AdaptyConfig.ServerCluster
import com.adapty.utils.AdaptyResult
import com.adapty.utils.FileLocation
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.take
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.locks.ReentrantReadWriteLock

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class CacheRepository(
    private val preferenceManager: PreferenceManager,
    private val fallbackPaywallRetriever: FallbackPaywallRetriever,
    private val crossPlacementInfoLock: ReentrantReadWriteLock,
    private val productPALMappingLock: ReentrantReadWriteLock,
    private val validateDataLock: ReentrantReadWriteLock,
    serverCluster: ServerCluster,
) {

    private val currentProfile = MutableSharedFlow<ProfileDto>()

    private val installRegistration = MutableSharedFlow<AdaptyResult<InstallRegistrationResponseData>>()

    private val cache = ConcurrentHashMap<String, Any>(32)

    suspend fun updateDataOnCreateProfile(
        profile: ProfileDto,
        installationMeta: InstallationMeta,
        profileStateChange: ProfileStateChange,
    ) {
        if (profileStateChange == ProfileStateChange.OUTDATED)
            return
        val profileIdHasChanged =
            profileStateChange in listOf(ProfileStateChange.NEW, ProfileStateChange.IDENTIFIED_TO_ANOTHER)
        if (profileIdHasChanged)
            onNewProfileIdReceived(profile.profileId)
        if (profileIdHasChanged || (getCustomerUserId() != profile.customerUserId)) {
            clearSyncedPurchases()
        }
        saveCustomerUserId(profile.customerUserId)
        saveProfile(profile)

        cache.remove(UNSYNCED_PROFILE_ID)
        val currentUnsyncedCUIdDiffers = profile.customerUserId != cache.getAs<IdentityParams>(UNSYNCED_IDENTITY_PARAMS)?.customerUserId
        if (!currentUnsyncedCUIdDiffers)
            cache.remove(UNSYNCED_IDENTITY_PARAMS)
        saveLastSentInstallationMeta(installationMeta)
    }

    suspend fun updateOnProfileReceived(
        profile: ProfileDto,
        profileIdWhenRequestSent: String?,
    ): ProfileDto {
        if (profileIdWhenRequestSent != null && getProfileId() != profileIdWhenRequestSent && profile.timestamp.orDefault() < getProfile()?.timestamp.orDefault()) {
            return profile
        }

        return saveProfile(profile)
    }

    private suspend fun saveProfile(profile: ProfileDto) =
        profile.also {
            execute {
                currentProfile.emit(profile)
            }
            saveData(PROFILE, profile)
        }

    fun subscribeOnProfileChanges() =
        currentProfile

    fun subscribeOnInstallRegistration() =
        installRegistration
            .take(1)

    fun getAppKey() = preferenceManager.getString(APP_KEY)

    fun saveAppKey(appKey: String) {
        preferenceManager.saveString(APP_KEY, appKey)
    }

    fun getProfileId() = (cache.getAs<String>(UNSYNCED_PROFILE_ID) ?: getString(PROFILE_ID))
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
            startsWithKeys = setOf(PAYWALL_RESPONSE_START_PART, ONBOARDING_RESPONSE_START_PART, FLOW_RESPONSE_START_PART),
        )
        saveProfileId(newProfileId)
    }

    fun getCustomerUserId() = getIdentityParams()?.customerUserId

    fun getIdentityParams(): IdentityParams? {
        return cache.safeGetOrPut(
            IDENTITY_PARAMS,
            {
                IdentityParams.from(
                    preferenceManager.getString(CUSTOMER_USER_ID),
                    preferenceManager.getString(GP_OBFUSCATED_ACCOUNT_ID),
                )
            },
        ) as? IdentityParams
    }

    fun getUnsyncedAuthData(): Pair<String?, IdentityParams?> {
        return cache.getAs<String>(UNSYNCED_PROFILE_ID) to cache.getAs<IdentityParams>(UNSYNCED_IDENTITY_PARAMS)
    }

    private fun saveCustomerUserId(customerUserId: String?) {
        val obfuscatedAccountId = cache.getAs<IdentityParams>(UNSYNCED_IDENTITY_PARAMS)?.obfuscatedAccountId
        val identityParams = IdentityParams.from(
            customerUserId,
            obfuscatedAccountId,
        )
        if (identityParams != null)
            cache[IDENTITY_PARAMS] = identityParams
        else
            cache.remove(IDENTITY_PARAMS)
        if (customerUserId != null)
            preferenceManager.saveString(CUSTOMER_USER_ID, customerUserId)
        else
            preferenceManager.clearData(setOf(CUSTOMER_USER_ID))
        if (obfuscatedAccountId != null)
            preferenceManager.saveString(GP_OBFUSCATED_ACCOUNT_ID, obfuscatedAccountId)
        else
            preferenceManager.clearData(setOf(GP_OBFUSCATED_ACCOUNT_ID))
    }

    fun prepareIdentityParamsToSync(newIdentityParams: IdentityParams?) {
        if (newIdentityParams == null) {
            cache.remove(UNSYNCED_IDENTITY_PARAMS)
        } else if (getIdentityParams() != newIdentityParams) {
            cache[UNSYNCED_IDENTITY_PARAMS] = newIdentityParams
        }
    }

    fun prepareProfileIdToSync() {
        if (cache[UNSYNCED_PROFILE_ID] == null && getString(PROFILE_ID).isNullOrEmpty()) {
            cache[UNSYNCED_PROFILE_ID] = generateUuid()
        }
    }

    private val installationMetaLock = ReentrantReadWriteLock()

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

    fun getInstallationMeta() =
        getData(LAST_SENT_INSTALLATION_META, InstallationMeta::class.java)

    fun saveLastSentInstallationMeta(installationMeta: InstallationMeta) {
        saveData(LAST_SENT_INSTALLATION_META, installationMeta)
    }

    fun getLastSentIp() = (cache[LAST_SENT_IP] as? Pair<*, *>)
        ?.let { (profileId, ip) ->
            if (profileId != getProfileId()) {
                cache.remove(LAST_SENT_IP)
                return@let null
            }
            ip as? String
        }

    fun saveLastSentIp(ip: String, profileId: String? = null) {
        cache[LAST_SENT_IP] = (profileId ?: getProfileId()) to ip
    }

    fun getPurchasesHaveBeenSynced() =
        cache.safeGetOrPut(
            PURCHASES_HAVE_BEEN_SYNCED,
            { preferenceManager.getBoolean(PURCHASES_HAVE_BEEN_SYNCED, false) }) as? Boolean
            ?: false

    fun setPurchasesHaveBeenSynced(synced: Boolean) {
        cache[PURCHASES_HAVE_BEEN_SYNCED] = synced
        preferenceManager.saveBoolean(PURCHASES_HAVE_BEEN_SYNCED, synced)
    }

    fun getExternalAnalyticsEnabled() =
        cache.safeGetOrPut(
            EXTERNAL_ANALYTICS_ENABLED,
            { preferenceManager.getBoolean(EXTERNAL_ANALYTICS_ENABLED, null) }) as? Boolean

    fun saveExternalAnalyticsEnabled(enabled: Boolean) {
        cache[EXTERNAL_ANALYTICS_ENABLED] = enabled
        preferenceManager.saveBoolean(EXTERNAL_ANALYTICS_ENABLED, enabled)
    }

    fun getLastAppOpenedTime() =
        cache.safeGetOrPut(
            APP_OPENED_TIME,
            { preferenceManager.getLong(APP_OPENED_TIME, 0L) }) as? Long ?: 0L

    fun saveLastAppOpenedTime(timeMillis: Long) {
        cache[APP_OPENED_TIME] = timeMillis
        preferenceManager.saveLong(APP_OPENED_TIME, timeMillis)
    }

    fun getLastWebPaywallOpenedTime() = cache[WEB_PAYWALL_OPENED_TIME] as? Long ?: 0L

    fun saveLastWebPaywallOpenedTime(timeMillis: Long) {
        cache[WEB_PAYWALL_OPENED_TIME] = timeMillis
    }

    fun getLastWebPaywallProfileRefreshStartTime() = cache[WEB_PAYWALL_PROFILE_REFRESH_START_TIME] as? Long ?: 0L

    fun saveLastWebPaywallProfileRefreshStartTime(timeMillis: Long) {
        cache[WEB_PAYWALL_PROFILE_REFRESH_START_TIME] = timeMillis
    }

    fun getLastRequestedCrossPlacementInfoTime() =
        cache.safeGetOrPut(
            CROSSPLACEMENT_INFO_REQUESTED_TIME,
            { preferenceManager.getLong(CROSSPLACEMENT_INFO_REQUESTED_TIME, 0L) }) as? Long ?: 0L

    fun saveLastRequestedCrossPlacementInfoTime(timeMillis: Long) {
        cache[CROSSPLACEMENT_INFO_REQUESTED_TIME] = timeMillis
        preferenceManager.saveLong(CROSSPLACEMENT_INFO_REQUESTED_TIME, timeMillis)
    }

    fun clearLastRequestedCrossPlacementInfoTime() {
        clearData(
            containsKeys = setOf(CROSSPLACEMENT_INFO_REQUESTED_TIME),
            startsWithKeys = setOf(),
        )
    }

    fun getProfile() =
        getData(PROFILE, ProfileDto::class.java)

    fun getPaywallVariationsFallback(placementId: String): FallbackVariations? {
        val fallbackPaywallsInfo = getFallbackPaywallsMetaInfo() ?: return null
        if (placementId !in fallbackPaywallsInfo.meta.developerIds) return null
        return fallbackPaywallRetriever.getPaywall(fallbackPaywallsInfo.source, placementId)
    }

    fun getFlowViewConfigFallback(viewConfigurationId: String): Map<String, Any>? {
        val fallbackPaywallsInfo = getFallbackPaywallsMetaInfo() ?: return null
        return fallbackPaywallRetriever.getUiSchema(fallbackPaywallsInfo.source, viewConfigurationId)
    }

    fun getFallbackPaywallsSnapshotAt() =
        getFallbackPaywallsMetaInfo()?.meta?.snapshotAt

    private fun getFallbackPaywallsMetaInfo() = cache[FALLBACK_FILE] as? FallbackPaywallsInfo

    fun getSyncedPurchases() =
        getData<HashSet<SyncedPurchase>>(SYNCED_PURCHASES).orEmpty()

    fun saveSyncedPurchases(data: Set<SyncedPurchase>) {
        saveData(SYNCED_PURCHASES, data)
    }

    fun getAnalyticsData(isSystemLog: Boolean) =
        getData<AnalyticsData>(getAnalyticsKey(isSystemLog)) ?: AnalyticsData.DEFAULT

    fun saveAnalyticsData(data: AnalyticsData, isSystemLog: Boolean) {
        saveData(getAnalyticsKey(isSystemLog), data)
    }

    private fun getAnalyticsKey(isSystemLog: Boolean) =
        if (isSystemLog) ANALYTICS_LOW_PRIORITY_DATA else ANALYTICS_DATA

    @Volatile
    var netConfig = NetConfig.createDefault(serverCluster)

    fun getVariation(id: String, locales: Set<String>, variationType: VariationType, maxAgeMillis: Long? = null): Variation? {
        val cacheKey: String
        val cacheVersion: Int

        when (variationType) {
            VariationType.Onboarding -> {
                cacheKey = getOnboardingCacheKey(id)
                cacheVersion = CURRENT_CACHED_ONBOARDING_VERSION
            }
            VariationType.Flow -> {
                cacheKey = getFlowCacheKey(id)
                cacheVersion = CURRENT_CACHED_FLOW_VERSION
            }
        }

        return getData<CacheEntity<Variation>>(cacheKey)?.let { (variation, version, cachedAt) ->
            if (version < cacheVersion) return@let null
            if ((maxAgeMillis != null) && (System.currentTimeMillis() - cachedAt > maxAgeMillis)) return@let null
            val languageCodes = locales.mapNotNull { locale -> extractLanguageCode(locale) }
            if (variation.getLanguageCode() !in languageCodes) return@let null
            variation
        }
    }

    fun saveVariation(id: String, variation: Variation) {
        when (variation) {
            is Onboarding -> saveOnboarding(id, variation)
            is FlowDto -> saveFlow(id, variation)
        }
    }

    private fun saveOnboarding(id: String, onboarding: Onboarding) {
        saveData(getOnboardingCacheKey(id), CacheEntity(onboarding, CURRENT_CACHED_ONBOARDING_VERSION))
    }

    private fun saveFlow(id: String, flow: FlowDto) {
        saveData(getFlowCacheKey(id), CacheEntity(flow, CURRENT_CACHED_FLOW_VERSION))
    }

    private fun getOnboardingCacheKey(id: String) =
        getVariationCacheKey(id, ONBOARDING_RESPONSE_START_PART)

    private fun getFlowCacheKey(id: String) =
        getVariationCacheKey(id, FLOW_RESPONSE_START_PART)

    fun getFlowViewConfig(flowId: String, viewConfigurationId: String): Map<String, Any>? =
        getData<CacheEntity<FlowViewConfig>>(getFlowViewConfigCacheKey(flowId))?.let { (viewConfig, version, _) ->
            if (version < CURRENT_CACHED_FLOW_BUILDER_VERSION) return@let null
            viewConfig.takeIf { it.viewConfigurationId == viewConfigurationId }?.config
        }

    fun saveFlowViewConfig(flowId: String, viewConfigurationId: String, config: Map<String, Any>) {
        saveData(
            getFlowViewConfigCacheKey(flowId),
            CacheEntity(FlowViewConfig(viewConfigurationId, config), CURRENT_CACHED_FLOW_BUILDER_VERSION),
        )
    }

    private fun getFlowViewConfigCacheKey(flowId: String) =
        "$FLOW_BUILDER_RESPONSE_START_PART${flowId}$VARIATION_RESPONSE_END_PART"

    private fun getVariationCacheKey(id: String, startPart: String) =
        "$startPart${id}$VARIATION_RESPONSE_END_PART"

    fun getOnboardingVariationId() = getString(ONBOARDING_VARIATION_ID)

    fun saveOnboardingVariationId(onboardingVariationId: String) {
        cache[ONBOARDING_VARIATION_ID] = onboardingVariationId
        preferenceManager.saveString(ONBOARDING_VARIATION_ID, onboardingVariationId)
    }

    fun saveFallback(source: FileLocation) {
        cache[FALLBACK_FILE] = fallbackPaywallRetriever.getMetaInfo(source)
    }

    fun getCrossPlacementInfo() =
        try {
            crossPlacementInfoLock.readLock().lock()
            getCrossPlacementInfoInternal()
        } finally {
            crossPlacementInfoLock.readLock().unlockQuietly()
        }

    private fun getCrossPlacementInfoInternal() =
        getData<CacheEntity<CrossPlacementInfo>>(CROSS_PLACEMENT_INFO)?.value

    fun saveCrossPlacementInfo(crossPlacementInfo: CrossPlacementInfo) {
        try {
            crossPlacementInfoLock.writeLock().lock()
            val oldVersion = getCrossPlacementInfoInternal()?.version ?: -1
            if (crossPlacementInfo.version > oldVersion)
                saveData(CROSS_PLACEMENT_INFO, CacheEntity(crossPlacementInfo))
        } finally {
            crossPlacementInfoLock.writeLock().unlockQuietly()
        }
    }

    fun saveCrossPlacementInfoFromPaywall(crossPlacementInfo: CrossPlacementInfo) {
        try {
            crossPlacementInfoLock.writeLock().lock()
            saveData(
                CROSS_PLACEMENT_INFO,
                CacheEntity(
                    getCrossPlacementInfoInternal()
                        ?.copy(placementWithVariationMap = crossPlacementInfo.placementWithVariationMap)
                        ?: crossPlacementInfo
                )
            )
        } finally {
            crossPlacementInfoLock.writeLock().unlockQuietly()
        }
    }

    fun getProductPALMappings() =
        try {
            productPALMappingLock.readLock().lock()
            getProductPALMappingsInternal()
        } finally {
            productPALMappingLock.readLock().unlockQuietly()
        }

    private fun getProductPALMappingsInternal() =
        getData<CacheEntity<ProductPALMappings>>(PRODUCT_PAL_MAPPINGS)
            ?.takeIf { it.version == CURRENT_CACHED_PRODUCT_PAL_MAPPING_VERSION }
            ?.value

    fun saveProductPALMappings(productPALMappings: ProductPALMappings) {
        try {
            productPALMappingLock.writeLock().lock()
            saveData(
                PRODUCT_PAL_MAPPINGS,
                CacheEntity(productPALMappings, CURRENT_CACHED_PRODUCT_PAL_MAPPING_VERSION),
            )
        } finally {
            productPALMappingLock.writeLock().unlockQuietly()
        }
    }

    fun saveProductPALMappingsFromPaywall(products: Collection<ProductDto>) {
        try {
            productPALMappingLock.writeLock().lock()
            val palMappings = getProductPALMappingsInternal()
            val palMappingsFromPaywall = ProductPALMappings(
                products.mapNotNull {
                    it.vendorProductId ?: return@mapNotNull null
                    val key = combinedProductId(it.vendorProductId, it.basePlanId)
                    key to ProductPALMappings.Item(it.accessLevelId, it.productType)
                }.toMap()
            )
            saveData(
                PRODUCT_PAL_MAPPINGS,
                CacheEntity(
                    palMappings
                        ?.let { it.copy(items = it.items + palMappingsFromPaywall.items) }
                        ?: palMappingsFromPaywall,
                    CURRENT_CACHED_PRODUCT_PAL_MAPPING_VERSION,
                )
            )
        } finally {
            productPALMappingLock.writeLock().unlockQuietly()
        }
    }

    fun getInstallData(): InstallData? {
        return getData<CacheEntity<InstallData>>(INSTALL_DATA)?.value
    }

    fun saveInstallData(installData: InstallData) {
        saveData(INSTALL_DATA, CacheEntity(installData))
    }

    fun getInstallRegistrationResponseData(): InstallRegistrationResponseData? {
        return getData<CacheEntity<InstallRegistrationResponseData>>(INSTALL_REGISTRATION_RESPONSE_DATA)?.value
    }

    fun saveInstallRegistrationResponseError(error: AdaptyError) {
        execute {
            installRegistration.emit(AdaptyResult.Error(error))
        }
    }

    fun saveInstallRegistrationResponseData(installRegistrationResponseData: InstallRegistrationResponseData) {
        execute {
            installRegistration.emit(AdaptyResult.Success(installRegistrationResponseData))
        }
        saveData(INSTALL_REGISTRATION_RESPONSE_DATA, CacheEntity(installRegistrationResponseData))
    }

    fun getUnsyncedValidateData() =
        validateDataLock.readLock().withLockSafe {
            getUnsyncedValidateDataInternal()
        }

    private fun getUnsyncedValidateDataInternal() =
        getData<CacheEntity<Map<String, ValidateReceiptRequest>>>(UNSYNCED_VALIDATE_DATA)?.value

    fun saveUnsyncedValidateData(key: String, validateData: ValidateReceiptRequest) {
        validateDataLock.writeLock().withLockSafe {
            val unsyncedValidateData = getUnsyncedValidateDataInternal().orEmpty() + (key to validateData)
            saveData(UNSYNCED_VALIDATE_DATA, CacheEntity(unsyncedValidateData))
        }
    }

    fun removeUnsyncedValidateData(key: String) {
        validateDataLock.writeLock().withLockSafe {
            val unsyncedValidateData = getUnsyncedValidateDataInternal()?.minus(key) ?: return@withLockSafe
            saveData(UNSYNCED_VALIDATE_DATA, CacheEntity(unsyncedValidateData))
        }
    }

    fun getSessionCount() =
        cache.safeGetOrPut(
            SESSION_COUNT,
            { preferenceManager.getLong(SESSION_COUNT, 0L) }) as? Long ?: 0L

    fun incrementSessionCount() {
        val sessionCount = getSessionCount() + 1
        cache[SESSION_COUNT] = sessionCount
        preferenceManager.saveLong(SESSION_COUNT, sessionCount)
    }

    fun clearOnLogout() {
        clearData(
            containsKeys = setOf(
                CUSTOMER_USER_ID,
                GP_OBFUSCATED_ACCOUNT_ID,
                IDENTITY_PARAMS,
                PROFILE_ID,
                PROFILE,
                SYNCED_PURCHASES,
                PURCHASES_HAVE_BEEN_SYNCED,
                APP_OPENED_TIME,
                CROSSPLACEMENT_INFO_REQUESTED_TIME,
                PRODUCT_RESPONSE,
                PRODUCT_RESPONSE_HASH,
                PROFILE_RESPONSE,
                PROFILE_RESPONSE_HASH,
                CROSS_PLACEMENT_INFO,
            ),
            startsWithKeys = setOf(PAYWALL_RESPONSE_START_PART, ONBOARDING_RESPONSE_START_PART, FLOW_RESPONSE_START_PART),
        )
    }

    fun clearSyncedPurchases() {
        clearData(
            containsKeys = setOf(
                SYNCED_PURCHASES,
                PURCHASES_HAVE_BEEN_SYNCED,
            ),
            startsWithKeys = setOf(),
        )
    }

    fun clearOnAppKeyChanged() {
        clearData(
            containsKeys = setOf(
                CUSTOMER_USER_ID,
                GP_OBFUSCATED_ACCOUNT_ID,
                IDENTITY_PARAMS,
                PROFILE_ID,
                PROFILE,
                SYNCED_PURCHASES,
                PURCHASES_HAVE_BEEN_SYNCED,
                APP_OPENED_TIME,
                CROSSPLACEMENT_INFO_REQUESTED_TIME,
                PRODUCT_RESPONSE,
                PRODUCT_RESPONSE_HASH,
                PRODUCTS_RESPONSE,
                PRODUCTS_RESPONSE_HASH,
                PROFILE_RESPONSE,
                PROFILE_RESPONSE_HASH,
                CROSS_PLACEMENT_INFO,
                ANALYTICS_DATA,
                YET_UNPROCESSED_VALIDATE_PRODUCT_INFO,
                EXTERNAL_ANALYTICS_ENABLED,
            ),
            startsWithKeys = setOf(PAYWALL_RESPONSE_START_PART, ONBOARDING_RESPONSE_START_PART, FLOW_RESPONSE_START_PART),
        )
    }

    private fun clearData(containsKeys: Set<String>, startsWithKeys: Set<String>) {
        val keysToRemove = preferenceManager.getKeysToRemove(containsKeys, startsWithKeys)

        cache.apply { keysToRemove.forEach(::remove) }
        preferenceManager.clearData(keysToRemove)
    }

    internal fun getString(key: String) =
        cache.safeGetOrPut(key, { preferenceManager.getString(key) }) as? String

    internal fun saveRequestOrResponseLatestData(map: Map<String, String>) {
        map.forEach { (key, value) ->
            cache[key] = value
        }
        preferenceManager.saveStrings(map)
    }

    fun getSessionId() = cache.safeGetOrPut(SESSION_ID) { generateUuid() } as? String

    fun hasLocalProfile() = preferenceManager.contains(PROFILE)

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
        private const val CURRENT_CACHED_ONBOARDING_VERSION = 1
        private const val CURRENT_CACHED_FLOW_VERSION = 1
        private const val CURRENT_CACHED_FLOW_BUILDER_VERSION = 1
        private const val CURRENT_CACHED_PRODUCT_PAL_MAPPING_VERSION = 2
    }

    fun setLongValue(key: String, value: Long, isPersisted: Boolean) {
        cache[key] = value
        if (isPersisted)
            preferenceManager.saveLong(key, value)
    }

    fun getLongValue(key: String, isPersisted: Boolean): Long? {
        return if (isPersisted)
            cache.safeGetOrPut(
                key,
                { preferenceManager.getLong(key, null) }) as? Long
        else
            cache[key] as? Long
    }
}