@file:OptIn(InternalAdaptyApi::class)
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.adapty.ui.internal.ui

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.adapty.Adapty
import com.adapty.internal.di.DIObject
import com.adapty.internal.di.Dependencies
import com.adapty.internal.di.Dependencies.OBSERVER_MODE
import com.adapty.internal.domain.models.ProductType
import com.adapty.internal.data.cache.CacheRepository
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.PriceFormatter
import com.adapty.internal.utils.extractProducts
import com.adapty.models.AdaptyPaywall
import com.adapty.models.AdaptyPaywallProduct
import com.adapty.models.AdaptyWebPresentation
import com.adapty.utils.ErrorCallback
import com.adapty.ui.AdaptyCustomAssets
import com.adapty.ui.AdaptyCustomColorAsset
import com.adapty.ui.AdaptyCustomFontAsset
import com.adapty.ui.AdaptyCustomGradientAsset
import com.adapty.ui.AdaptyCustomImageAsset
import com.adapty.ui.AdaptyCustomVideoAsset
import com.adapty.ui.AdaptyPaywallInsets
import com.adapty.ui.AdaptyUI
import com.adapty.ui.AdaptyUI.LocalizedViewConfiguration.Asset
import com.adapty.ui.AdaptyUI.LocalizedViewConfiguration.Asset.Image
import com.adapty.ui.internal.cache.MediaFetchService
import com.adapty.ui.internal.text.PriceConverter
import com.adapty.ui.internal.text.StringId
import com.adapty.ui.internal.text.TagResolver
import com.adapty.ui.internal.text.TextResolver
import com.adapty.ui.internal.ui.element.BaseTextElement.Attributes
import com.adapty.ui.internal.utils.CUSTOM_ASSET_SUFFIX
import com.adapty.ui.internal.utils.DARK_THEME_ASSET_SUFFIX
import com.adapty.ui.internal.utils.EventCallback
import com.adapty.ui.internal.utils.LOADING_PRODUCTS_RETRY_DELAY
import com.adapty.ui.internal.utils.LOG_PREFIX
import com.adapty.ui.internal.utils.LOG_PREFIX_ERROR
import com.adapty.ui.internal.utils.ProductLoadingFailureCallback
import com.adapty.ui.internal.utils.log
import com.adapty.ui.listeners.AdaptyUiEventListener
import com.adapty.ui.listeners.AdaptyUiObserverModeHandler
import com.adapty.ui.listeners.AdaptyUiTagResolver
import com.adapty.ui.listeners.AdaptyUiTimerResolver
import com.adapty.utils.AdaptyLogLevel.Companion.ERROR
import com.adapty.utils.AdaptyLogLevel.Companion.VERBOSE
import com.adapty.utils.AdaptyLogLevel.Companion.WARN
import com.adapty.utils.AdaptyResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale

internal class PaywallViewModel(
    private val flowKey: String,
    private val isObserverMode: Boolean,
    private val mediaFetchService: MediaFetchService,
    private val cacheRepository: CacheRepository,
    private val textResolver: TextResolver,
    args: UserArgs?,
) : ViewModel() {

    val dataState = mutableStateOf(args)

    val state = mutableStateMapOf<String, Any>()

    val products = mutableStateMapOf<String, AdaptyPaywallProduct>()

    val assets = mutableStateMapOf<String, Asset>()

    val texts = mutableStateMapOf<String, AdaptyUI.LocalizedViewConfiguration.TextItem>()

    val isLoading = mutableStateOf(false)

    init {
        viewModelScope.launch {
            snapshotFlow { dataState.value }
                .collect { newData ->
                    if (newData == null) return@collect
                    val viewConfig = newData.viewConfig
                    val initialProducts = newData.products
                    val customAssets = newData.customAssets
                    updateData(newData)

                    if (initialProducts.isEmpty()) {
                        viewModelScope.launch {
                            toggleLoading(true)
                            val productResult = loadProducts(
                                viewConfig.paywall,
                                newData.productLoadingFailureCallback,
                            )
                            if (productResult is AdaptyResult.Success)
                                products.putAll(associateProductsToIds(productResult.value, viewConfig.paywall))
                            toggleLoading(false)
                        }
                    }

                    viewConfig.assets.forEach { (id, asset) ->
                        if (asset is Asset.RemoteImage) {
                            when (val customAsset = asset.customId?.let(customAssets::getImage)) {
                                is AdaptyCustomImageAsset.Remote -> {
                                    viewModelScope.launch {
                                        val image = loadImage(customAsset.value)
                                        val customAssetId =
                                            if (id.endsWith(DARK_THEME_ASSET_SUFFIX))
                                                "${id.substringBeforeLast(DARK_THEME_ASSET_SUFFIX)}${CUSTOM_ASSET_SUFFIX}${DARK_THEME_ASSET_SUFFIX}"
                                            else
                                                "${id}${CUSTOM_ASSET_SUFFIX}"
                                        assets[customAssetId] = image
                                    }
                                }
                                is AdaptyCustomImageAsset.Local -> {
                                    val customAssetId =
                                        if (id.endsWith(DARK_THEME_ASSET_SUFFIX))
                                            "${id.substringBeforeLast(DARK_THEME_ASSET_SUFFIX)}${CUSTOM_ASSET_SUFFIX}${DARK_THEME_ASSET_SUFFIX}"
                                        else
                                            "${id}${CUSTOM_ASSET_SUFFIX}"
                                    assets[customAssetId] = customAsset.value
                                }
                                else -> Unit
                            }

                            viewModelScope.launch {
                                val image = loadImage(asset)
                                assets[id] = image
                            }
                        }

                        if (asset is Image) {
                            when (val customAsset = asset.customId?.let(customAssets::getImage)) {
                                is AdaptyCustomImageAsset.Remote -> {
                                    viewModelScope.launch {
                                        val image = loadImage(customAsset.value)
                                        val customAssetId =
                                            if (id.endsWith(DARK_THEME_ASSET_SUFFIX))
                                                "${id.substringBeforeLast(DARK_THEME_ASSET_SUFFIX)}${CUSTOM_ASSET_SUFFIX}${DARK_THEME_ASSET_SUFFIX}"
                                            else
                                                "${id}${CUSTOM_ASSET_SUFFIX}"
                                        assets[customAssetId] = image
                                    }
                                }
                                is AdaptyCustomImageAsset.Local -> {
                                    val customAssetId =
                                        if (id.endsWith(DARK_THEME_ASSET_SUFFIX))
                                            "${id.substringBeforeLast(DARK_THEME_ASSET_SUFFIX)}${CUSTOM_ASSET_SUFFIX}${DARK_THEME_ASSET_SUFFIX}"
                                        else
                                            "${id}${CUSTOM_ASSET_SUFFIX}"
                                    assets[customAssetId] = customAsset.value
                                }
                                else -> Unit
                            }
                        }

                        if (asset is Asset.Color) {
                            val customAsset = asset.customId?.let { customAssetId ->
                                customAssets.getFirstAvailable(
                                    customAssetId,
                                    listOf(
                                        AdaptyCustomAssets.AssetType.COLOR,
                                        AdaptyCustomAssets.AssetType.GRADIENT,
                                    )
                                )
                            }
                            when (customAsset) {
                                is AdaptyCustomColorAsset -> {
                                    val customAssetId =
                                        if (id.endsWith(DARK_THEME_ASSET_SUFFIX))
                                            "${id.substringBeforeLast(DARK_THEME_ASSET_SUFFIX)}${CUSTOM_ASSET_SUFFIX}${DARK_THEME_ASSET_SUFFIX}"
                                        else
                                            "${id}${CUSTOM_ASSET_SUFFIX}"
                                    assets[customAssetId] = customAsset.value
                                }
                                is AdaptyCustomGradientAsset -> {
                                    val customAssetId =
                                        if (id.endsWith(DARK_THEME_ASSET_SUFFIX))
                                            "${id.substringBeforeLast(DARK_THEME_ASSET_SUFFIX)}${CUSTOM_ASSET_SUFFIX}${DARK_THEME_ASSET_SUFFIX}"
                                        else
                                            "${id}${CUSTOM_ASSET_SUFFIX}"
                                    assets[customAssetId] = customAsset.value
                                }
                                else -> Unit
                            }
                        }

                        if (asset is Asset.Gradient) {
                            val customAsset = asset.customId?.let { customAssetId ->
                                customAssets.getFirstAvailable(
                                    customAssetId,
                                    listOf(
                                        AdaptyCustomAssets.AssetType.GRADIENT,
                                        AdaptyCustomAssets.AssetType.COLOR,
                                    )
                                )
                            }
                            when (customAsset) {
                                is AdaptyCustomColorAsset -> {
                                    val customAssetId =
                                        if (id.endsWith(DARK_THEME_ASSET_SUFFIX))
                                            "${id.substringBeforeLast(DARK_THEME_ASSET_SUFFIX)}${CUSTOM_ASSET_SUFFIX}${DARK_THEME_ASSET_SUFFIX}"
                                        else
                                            "${id}${CUSTOM_ASSET_SUFFIX}"
                                    assets[customAssetId] = customAsset.value
                                }
                                is AdaptyCustomGradientAsset -> {
                                    val customAssetId =
                                        if (id.endsWith(DARK_THEME_ASSET_SUFFIX))
                                            "${id.substringBeforeLast(DARK_THEME_ASSET_SUFFIX)}${CUSTOM_ASSET_SUFFIX}${DARK_THEME_ASSET_SUFFIX}"
                                        else
                                            "${id}${CUSTOM_ASSET_SUFFIX}"
                                    assets[customAssetId] = customAsset.value
                                }
                                else -> Unit
                            }
                        }

                        if (asset is Asset.Video) {
                            when (val customAsset = asset.customId?.let(customAssets::getVideo)) {
                                is AdaptyCustomVideoAsset -> {
                                    val customAssetId =
                                        if (id.endsWith(DARK_THEME_ASSET_SUFFIX))
                                            "${id.substringBeforeLast(DARK_THEME_ASSET_SUFFIX)}${CUSTOM_ASSET_SUFFIX}${DARK_THEME_ASSET_SUFFIX}"
                                        else
                                            "${id}${CUSTOM_ASSET_SUFFIX}"
                                    assets[customAssetId] = customAsset.value

                                    when (val customPreviewAsset = customAsset.preview) {
                                        is AdaptyCustomImageAsset.Remote -> {
                                            viewModelScope.launch {
                                                val image = loadImage(customPreviewAsset.value)
                                                val customAssetId =
                                                    if (id.endsWith(DARK_THEME_ASSET_SUFFIX))
                                                        "${id.substringBeforeLast(DARK_THEME_ASSET_SUFFIX)}${CUSTOM_ASSET_SUFFIX}${DARK_THEME_ASSET_SUFFIX}"
                                                    else
                                                        "${id}${CUSTOM_ASSET_SUFFIX}"
                                                assets[customAssetId] = image
                                            }
                                        }
                                        is AdaptyCustomImageAsset.Local -> {
                                            val customAssetId =
                                                if (id.endsWith(DARK_THEME_ASSET_SUFFIX))
                                                    "${id.substringBeforeLast(DARK_THEME_ASSET_SUFFIX)}${CUSTOM_ASSET_SUFFIX}${DARK_THEME_ASSET_SUFFIX}"
                                                else
                                                    "${id}${CUSTOM_ASSET_SUFFIX}"
                                            assets[customAssetId] = customAsset.value
                                        }
                                        else -> Unit
                                    }
                                }
                                else -> Unit
                            }
                        }

                        if (asset is Asset.Font) {
                            when (val customAsset = asset.customId?.let(customAssets::getFont)) {
                                is AdaptyCustomFontAsset -> {
                                    val customAssetId =
                                        if (id.endsWith(DARK_THEME_ASSET_SUFFIX))
                                            "${id.substringBeforeLast(DARK_THEME_ASSET_SUFFIX)}${CUSTOM_ASSET_SUFFIX}${DARK_THEME_ASSET_SUFFIX}"
                                        else
                                            "${id}${CUSTOM_ASSET_SUFFIX}"
                                    assets[customAssetId] = customAsset.value
                                }
                                else -> Unit
                            }
                        }
                    }
                }
        }
    }

    fun setNewData(newData: UserArgs) {
        dataState.value = newData
        textResolver.setCustomTagResolver(newData.tagResolver)
    }

    private fun updateData(newData: UserArgs) {
        updateState(newData.viewConfig)
        updateAssets(newData.viewConfig)
        updateProducts(newData.products, newData.viewConfig)
        updateTexts(newData.viewConfig)
    }

    private fun updateState(viewConfig: AdaptyUI.LocalizedViewConfiguration) {
        with(state) {
            clear()
            putAll(viewConfig.screens.initialState)
        }
    }

    private fun updateProducts(
        initialProducts: List<AdaptyPaywallProduct>,
        viewConfig: AdaptyUI.LocalizedViewConfiguration,
    ) {
        with(products) {
            clear()
            putAll(associateProductsToIds(initialProducts, viewConfig.paywall))
        }
    }

    private fun updateTexts(
        viewConfig: AdaptyUI.LocalizedViewConfiguration,
    ) {
        with(texts) {
            clear()
            putAll(viewConfig.texts)
        }
    }

    private fun updateAssets(viewConfig: AdaptyUI.LocalizedViewConfiguration) {
        with(assets) {
            clear()
            putAll(
                viewConfig.assets.toList()
                    .mapNotNull { record ->
                        val (id, asset) = record
                        if (asset is Asset.RemoteImage)
                            asset.preview?.let { id to it }
                        else
                            record
                    }
                    .toMap()
            )
        }
    }

    private fun associateProductsToIds(
        products: List<AdaptyPaywallProduct>,
        paywall: AdaptyPaywall,
    ): Map<String, AdaptyPaywallProduct> {
        return if (products.isNotEmpty())
            extractProducts(paywall)
                .mapNotNull { paywallProduct ->
                    val adaptyId = paywallProduct.id
                    val vendorProductId = paywallProduct.vendorProductId
                    val basePlanId = (paywallProduct.type as? ProductType.Subscription)
                        ?.subscriptionData?.basePlanId
                    products
                        .firstOrNull { product -> product.vendorProductId == vendorProductId && (product.subscriptionDetails?.let { it.basePlanId == basePlanId } ?: true) }
                        ?.let { product -> adaptyId to product }
                }
                .toMap()
        else {
            mapOf()
        }
    }

    private suspend fun loadImage(remoteImage: Asset.RemoteImage): Image =
        suspendCancellableCoroutine { continuation ->
            mediaFetchService.loadImage(
                remoteImage,
                handlePreview = null,
                handleResult = { image ->
                    continuation.resumeWith(Result.success(image))
                }
            )
        }

    fun onPurchaseInitiated(
        activity: Activity,
        product: AdaptyPaywallProduct,
        eventListener: EventCallback,
        observerModeHandler: AdaptyUiObserverModeHandler?,
    ) {
        if (!isObserverMode) {
            if (observerModeHandler != null)
                log(WARN) { "$LOG_PREFIX $flowKey You should not pass observerModeHandler if you're using Adapty in Full Mode" }
            performMakePurchase(activity, product, eventListener)
        } else {
            if (observerModeHandler != null) {
                log(VERBOSE) { "$LOG_PREFIX $flowKey observerModeHandler: onPurchaseInitiated begin" }
                observerModeHandler.onPurchaseInitiated(
                    product,
                    {
                        log(VERBOSE) { "$LOG_PREFIX $flowKey observerModeHandler: onStartPurchase called" }
                        toggleLoading(true)
                    },
                    {
                        log(VERBOSE) { "$LOG_PREFIX $flowKey observerModeHandler: onFinishPurchase called" }
                        toggleLoading(false)
                    },
                )
            } else {
                log(WARN) { "$LOG_PREFIX $flowKey In order to handle purchases in Observer Mode enabled, provide the observerModeHandler!" }
                performMakePurchase(activity, product, eventListener)
            }
        }
    }

    fun onWebPurchaseInitiated(
        activity: Activity,
        presentation: AdaptyWebPresentation,
        paywall: AdaptyPaywall,
        product: AdaptyPaywallProduct? = null,
    ) {
        log(VERBOSE) { "$LOG_PREFIX $flowKey onWebPurchaseInitiated" }
        val callback = ErrorCallback { error ->
            if (error != null) {
                log(ERROR) { "$LOG_PREFIX_ERROR $flowKey openWebPaywall error: ${error.message}" }
            } else {
                log(VERBOSE) { "$LOG_PREFIX $flowKey openWebPaywall success" }
            }
        }

        if (product != null) {
            Adapty.openWebPaywall(activity, product, presentation, callback)
        } else {
            Adapty.openWebPaywall(activity, paywall, presentation, callback)
        }
    }


    private fun performMakePurchase(
        activity: Activity,
        product: AdaptyPaywallProduct,
        eventListener: EventCallback,
    ) {
        toggleLoading(true)
        log(VERBOSE) { "$LOG_PREFIX $flowKey makePurchase begin" }
        eventListener.onAwaitingPurchaseParams(product) { purchaseParams ->
            log(VERBOSE) { "$LOG_PREFIX $flowKey makePurchase onAwaitingPurchaseParams called" }
            activity.runOnUiThread {
                eventListener.onPurchaseStarted(product)
                Adapty.makePurchase(activity, product, purchaseParams) { result ->
                    toggleLoading(false)
                    when (result) {
                        is AdaptyResult.Success -> {
                            log(VERBOSE) { "$LOG_PREFIX $flowKey makePurchase success" }
                            eventListener.onPurchaseFinished(
                                result.value,
                                product,
                            )
                        }
                        is AdaptyResult.Error -> {
                            val error = result.error
                            log(ERROR) { "$LOG_PREFIX_ERROR $flowKey makePurchase error: ${error.message}" }
                            eventListener.onPurchaseFailure(
                                result.error,
                                product,
                            )
                        }
                    }
                }
            }
        }
    }

    fun onRestorePurchases(
        eventListener: EventCallback,
        observerModeHandler: AdaptyUiObserverModeHandler?,
    ) {
        if (!isObserverMode) {
            if (observerModeHandler != null)
                log(WARN) { "$LOG_PREFIX $flowKey You should not pass observerModeHandler if you're using Adapty in Full Mode" }
            performRestorePurchases(eventListener)
        } else {
            val restoreHandler = observerModeHandler?.getRestoreHandler()
            if (restoreHandler != null) {
                log(VERBOSE) { "$LOG_PREFIX $flowKey observerModeHandler: onRestoreInitiated begin" }
                restoreHandler.onRestoreInitiated(
                    {
                        log(VERBOSE) { "$LOG_PREFIX $flowKey observerModeHandler: onStartRestore called" }
                        toggleLoading(true)
                    },
                    {
                        log(VERBOSE) { "$LOG_PREFIX $flowKey observerModeHandler: onFinishRestore called" }
                        toggleLoading(false)
                    },
                )
            } else {
                log(VERBOSE) { "$LOG_PREFIX $flowKey To handle restore manually in Observer Mode, implement getRestoreHandler() in observerModeHandler" }
                performRestorePurchases(eventListener)
            }
        }
    }

    private fun performRestorePurchases(eventListener: EventCallback) {
        toggleLoading(true)
        log(VERBOSE) { "$LOG_PREFIX $flowKey restorePurchases begin" }
        eventListener.onRestoreStarted()
        Adapty.restorePurchases { result ->
            toggleLoading(false)
            when (result) {
                is AdaptyResult.Success -> {
                    log(VERBOSE) { "$LOG_PREFIX $flowKey restorePurchases success" }
                    eventListener.onRestoreSuccess(result.value)
                }
                is AdaptyResult.Error -> {
                    log(ERROR) { "$LOG_PREFIX_ERROR $flowKey restorePurchases error: ${result.error.message}" }
                    eventListener.onRestoreFailure(result.error)
                }
            }
        }
    }

    private suspend fun loadProducts(
        paywall: AdaptyPaywall,
        loadingFailureCallback: ProductLoadingFailureCallback,
    ): AdaptyResult<List<AdaptyPaywallProduct>> {
        suspend fun load(
            paywall: AdaptyPaywall,
        ): AdaptyResult<List<AdaptyPaywallProduct>> =
            suspendCancellableCoroutine { continuation ->
                log(VERBOSE) { "$LOG_PREFIX $flowKey loadProducts begin" }
                Adapty.getPaywallProducts(paywall) { result ->
                    when (result) {
                        is AdaptyResult.Success -> {
                            continuation.resumeWith(Result.success(result))
                            log(VERBOSE) { "$LOG_PREFIX $flowKey loadProducts success" }
                        }
                        is AdaptyResult.Error -> {
                            continuation.resumeWith(Result.success(result))
                            log(ERROR) { "$LOG_PREFIX_ERROR $flowKey loadProducts error: ${result.error.message}" }
                        }
                    }
                }
            }

        val productResult = load(paywall)
        return when (productResult) {
            is AdaptyResult.Success -> productResult
            is AdaptyResult.Error -> {
                val shouldRetry =
                    loadingFailureCallback.onLoadingProductsFailure(productResult.error)
                if (shouldRetry) {
                    delay(LOADING_PRODUCTS_RETRY_DELAY)
                    loadProducts(paywall, loadingFailureCallback)
                } else
                    productResult
            }
        }
    }

    fun logShowPaywall(viewConfig: AdaptyUI.LocalizedViewConfiguration) {
        log(VERBOSE) { "$LOG_PREFIX $flowKey logShowPaywall begin" }
        Adapty.logShowPaywall(
            viewConfig.paywall,
            mapOf("paywall_builder_id" to viewConfig.id)
        ) { error ->
            if (error != null) {
                log(ERROR) { "$LOG_PREFIX_ERROR $flowKey logShowPaywall error: ${error.message}" }
            } else {
                log(VERBOSE) { "$LOG_PREFIX $flowKey logShowPaywall success" }
            }
        }
    }

    private fun toggleLoading(show: Boolean) {
        isLoading.value = show
    }

    @Composable
    fun resolveText(stringId: StringId, textAttrs: Attributes?) =
        textResolver.resolve(stringId, textAttrs, texts, products, assets, state)

    fun getTimerStartTimestamp(placementId: String, timerId: String, isPersisted: Boolean): Long? {
        return cacheRepository.getLongValue(getTimerStartTimestampId(placementId, timerId), isPersisted)
    }

    fun setTimerStartTimestamp(placementId: String, timerId: String, value: Long, isPersisted: Boolean) {
        cacheRepository.setLongValue(getTimerStartTimestampId(placementId, timerId), value, isPersisted)
    }

    private fun getTimerStartTimestampId(placementId: String, timerId: String) =
        "${placementId}_timer_${timerId}_start"
}

internal class PaywallViewModelFactory(
    private val vmArgs: PaywallViewModelArgs,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(PaywallViewModel::class.java) -> {
                PaywallViewModel(
                    vmArgs.flowKey,
                    vmArgs.isObserverMode,
                    vmArgs.mediaFetchService,
                    vmArgs.cacheRepository,
                    vmArgs.textResolver,
                    vmArgs.userArgs,
                ) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

internal class PaywallViewModelArgs(
    val flowKey: String,
    val isObserverMode: Boolean,
    val mediaFetchService: MediaFetchService,
    val cacheRepository: CacheRepository,
    val textResolver: TextResolver,
    val userArgs: UserArgs?,
) {
    companion object {
        fun create(
            flowKey: String,
            userArgs: UserArgs?,
            locale: Locale,
        ) =
            runCatching {
                val mediaFetchService = Dependencies.injectInternal<MediaFetchService>()
                val cacheRepository = Dependencies.injectInternal<CacheRepository>()
                val isObserverMode = Dependencies.injectInternal<Boolean>(OBSERVER_MODE)
                val priceFormatter = Dependencies.injectInternal<PriceFormatter>(locale.toString()) {
                    DIObject({ PriceFormatter(locale) })
                }
                val priceConverter = PriceConverter()
                val tagResolver = TagResolver(
                    priceFormatter,
                    priceConverter,
                    userArgs?.tagResolver ?: AdaptyUiTagResolver.DEFAULT,
                )
                val textResolver = TextResolver(tagResolver)
                PaywallViewModelArgs(
                    flowKey,
                    isObserverMode,
                    mediaFetchService,
                    cacheRepository,
                    textResolver,
                    userArgs,
                )
            }.getOrElse { e ->
                log(ERROR) {
                    "$LOG_PREFIX_ERROR $flowKey rendering error: ${e.localizedMessage}"
                }
                null
            }
    }
}

internal class UserArgs(
    val viewConfig: AdaptyUI.LocalizedViewConfiguration,
    val eventListener: AdaptyUiEventListener,
    val userInsets: AdaptyPaywallInsets,
    val customAssets: AdaptyCustomAssets,
    val tagResolver: AdaptyUiTagResolver,
    val timerResolver: AdaptyUiTimerResolver,
    val observerModeHandler: AdaptyUiObserverModeHandler?,
    val products: List<AdaptyPaywallProduct>,
    val productLoadingFailureCallback: ProductLoadingFailureCallback,
) {
    companion object {
        fun create(
            viewConfig: AdaptyUI.LocalizedViewConfiguration,
            eventListener: AdaptyUiEventListener,
            userInsets: AdaptyPaywallInsets,
            customAssets: AdaptyCustomAssets,
            tagResolver: AdaptyUiTagResolver,
            timerResolver: AdaptyUiTimerResolver,
            observerModeHandler: AdaptyUiObserverModeHandler?,
            products: List<AdaptyPaywallProduct>?,
            productLoadingFailureCallback: ProductLoadingFailureCallback,
        ) =
            UserArgs(
                viewConfig,
                eventListener,
                userInsets,
                customAssets,
                tagResolver,
                timerResolver,
                observerModeHandler,
                products.orEmpty(),
                productLoadingFailureCallback,
            )
    }
}