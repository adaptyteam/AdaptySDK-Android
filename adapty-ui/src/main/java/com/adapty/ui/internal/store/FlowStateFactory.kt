@file:OptIn(InternalAdaptyApi::class)
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.adapty.ui.internal.store

import com.adapty.internal.domain.models.ProductType
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.extractProducts
import com.adapty.models.AdaptyFlow
import com.adapty.models.AdaptyPaywallProduct
import com.adapty.ui.AdaptyCustomAssets
import com.adapty.ui.AdaptyCustomColorAsset
import com.adapty.ui.AdaptyCustomFontAsset
import com.adapty.ui.AdaptyCustomGradientAsset
import com.adapty.ui.AdaptyCustomImageAsset
import com.adapty.ui.AdaptyCustomVideoAsset
import com.adapty.ui.AdaptyUI
import com.adapty.ui.AdaptyUI.FlowConfiguration.Asset
import com.adapty.ui.AdaptyUI.FlowConfiguration.Asset.Image
import com.adapty.ui.internal.ui.UserArgs
import com.adapty.ui.internal.utils.CUSTOM_ASSET_SUFFIX
import com.adapty.ui.internal.utils.DARK_THEME_ASSET_SUFFIX
import com.adapty.ui.internal.utils.isLive

internal fun buildInitialState(
    userArgs: UserArgs,
    isObserverMode: Boolean,
    previousState: FlowState?,
): FlowState {
    val viewConfig = userArgs.viewConfig
    val mode = viewConfig.mode

    val configState = ConfigState(
        viewConfig = viewConfig,
        isObserverMode = isObserverMode,
        placementId = mode.placementId,
        observerModeHandler = userArgs.observerModeHandler,
        eventListener = userArgs.eventListener,
        userInsets = userArgs.userInsets,
        customAssets = userArgs.customAssets,
        tagResolver = userArgs.tagResolver,
        timerResolver = userArgs.timerResolver,
        productLoadingFailureCallback = userArgs.productLoadingFailureCallback,
    )

    val assetsMap = buildLocalAssetsMap(viewConfig, userArgs.customAssets)

    val productsMap = if (mode.isLive()) {
        associateProductsToIds(userArgs.products, mode.flow)
    } else {
        userArgs.products.associateBy { resolveProductKey(it) }
    }

    return FlowState(
        config = configState,
        products = ProductsState(
            items = productsMap,
            loadingStatus = if (productsMap.isNotEmpty()) LoadingStatus.Loaded else LoadingStatus.Idle,
        ),
        assets = AssetsState(assetsMap),
        texts = TextsState(viewConfig.texts),
        purchase = PurchaseFlowState.Idle,
        restore = RestoreFlowState.Idle,
        navigation = previousState?.navigation ?: NavigationState(entries = emptyMap()),
        ui = UiState(isLoading = false, flowShown = previousState?.ui?.flowShown ?: false),
    )
}

internal fun buildLocalAssetsMap(
    viewConfig: AdaptyUI.FlowConfiguration,
    customAssets: AdaptyCustomAssets,
): Map<String, Asset> {
    val assetsMap = mutableMapOf<String, Asset>()
    viewConfig.assets.forEach { (id, asset) ->
        if (asset is Asset.RemoteImage)
            asset.preview?.let { assetsMap[id] = it }
        else
            assetsMap[id] = asset
    }

    viewConfig.assets.forEach { (id, asset) ->
        fun customAssetId(): String =
            if (id.endsWith(DARK_THEME_ASSET_SUFFIX))
                "${id.substringBeforeLast(DARK_THEME_ASSET_SUFFIX)}${CUSTOM_ASSET_SUFFIX}${DARK_THEME_ASSET_SUFFIX}"
            else
                "${id}${CUSTOM_ASSET_SUFFIX}"

        if (asset is Asset.RemoteImage) {
            when (val customAsset = asset.customId?.let(customAssets::getImage)) {
                is AdaptyCustomImageAsset.Local -> assetsMap[customAssetId()] = customAsset.value
                else -> Unit
            }
        }
        if (asset is Image) {
            when (val customAsset = asset.customId?.let(customAssets::getImage)) {
                is AdaptyCustomImageAsset.Local -> assetsMap[customAssetId()] = customAsset.value
                else -> Unit
            }
        }
        if (asset is Asset.Color) {
            val customAsset = asset.customId?.let { cid ->
                customAssets.getFirstAvailable(cid, listOf(AdaptyCustomAssets.AssetType.COLOR, AdaptyCustomAssets.AssetType.GRADIENT))
            }
            when (customAsset) {
                is AdaptyCustomColorAsset -> assetsMap[customAssetId()] = customAsset.value
                is AdaptyCustomGradientAsset -> assetsMap[customAssetId()] = customAsset.value
                else -> Unit
            }
        }
        if (asset is Asset.Gradient) {
            val customAsset = asset.customId?.let { cid ->
                customAssets.getFirstAvailable(cid, listOf(AdaptyCustomAssets.AssetType.GRADIENT, AdaptyCustomAssets.AssetType.COLOR))
            }
            when (customAsset) {
                is AdaptyCustomColorAsset -> assetsMap[customAssetId()] = customAsset.value
                is AdaptyCustomGradientAsset -> assetsMap[customAssetId()] = customAsset.value
                else -> Unit
            }
        }
        if (asset is Asset.Video) {
            when (val customAsset = asset.customId?.let(customAssets::getVideo)) {
                is AdaptyCustomVideoAsset -> {
                    assetsMap[customAssetId()] = customAsset.value
                    when (val customPreviewAsset = customAsset.preview) {
                        is AdaptyCustomImageAsset.Local -> assetsMap[customAssetId()] = customPreviewAsset.value
                        else -> Unit
                    }
                }
                else -> Unit
            }
        }
        if (asset is Asset.Font) {
            when (val customAsset = asset.customId?.let(customAssets::getFont)) {
                is AdaptyCustomFontAsset -> assetsMap[customAssetId()] = customAsset.value
                else -> Unit
            }
        }
    }
    return assetsMap
}

internal fun resolveProductKey(product: AdaptyPaywallProduct): String =
    product.payloadData.flowProductId ?: product.payloadData.adaptyProductId

internal fun associateProductsToIds(
    products: List<AdaptyPaywallProduct>,
    flow: AdaptyFlow,
): Map<String, AdaptyPaywallProduct> {
    if (products.isEmpty()) return mapOf()
    return extractProducts(flow)
        .mapNotNull { backendProduct ->
            val key = backendProduct.flowProductId ?: backendProduct.id
            val vendorProductId = backendProduct.vendorProductId
            val basePlanId = (backendProduct.type as? ProductType.Subscription)
                ?.subscriptionData?.basePlanId
            products
                .firstOrNull { product -> product.vendorProductId == vendorProductId && (product.subscriptionDetails?.let { it.basePlanId == basePlanId } ?: true) }
                ?.let { product -> key to product }
        }
        .toMap()
}
