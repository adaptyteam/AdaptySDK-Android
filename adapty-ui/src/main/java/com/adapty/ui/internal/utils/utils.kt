@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.provider.Settings
import android.util.TypedValue
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.core.content.ContextCompat
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.models.AdaptyEligibility.ELIGIBLE
import com.adapty.models.AdaptyPaywallProduct
import com.adapty.models.AdaptyProductDiscountPhase
import com.adapty.ui.AdaptyUI
import com.adapty.ui.AdaptyUI.LocalizedViewConfiguration.Asset
import com.adapty.ui.R
import com.adapty.ui.internal.mapping.element.Assets
import com.adapty.utils.AdaptyLogLevel
import com.adapty.utils.AdaptyLogLevel.Companion.ERROR
import com.adapty.utils.AdaptyLogLevel.Companion.WARN
import java.util.Locale
import java.util.concurrent.Executors

internal fun Context.getCurrentLocale() =
    (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        resources.configuration.locales.get(0)
    } else {
        resources.configuration.locale
    })
        ?: Locale.getDefault().also {
            log(WARN) { "Failed to get locale from resources. Falling back to default." }
        }

internal fun Context.areAnimationsDisabled() =
    runCatching {
        Settings.Global.getFloat(
            contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    }.getOrDefault(false)

internal fun AdaptyPaywallProduct.firstDiscountOfferOrNull(): AdaptyProductDiscountPhase? {
    return subscriptionDetails?.let { subDetails ->
        subDetails.introductoryOfferPhases.firstOrNull()?.takeIf { subDetails.introductoryOfferEligibility == ELIGIBLE }
    }
}

internal fun getProductGroupKey(groupId: String) = "group_${groupId}"

@Composable
internal fun getScreenHeightDp(): Float {
    val insets = getInsets()
    return with(LocalDensity.current) {
        LocalConfiguration.current.screenHeightDp + (insets.getTop(this) + insets.getBottom(this)).toDp().value
    }
}

@Composable
internal fun getScreenWidthDp(): Float {
    val insets = getInsets()
    return with(LocalDensity.current) {
        val layoutDirection = LocalLayoutDirection.current
        LocalConfiguration.current.screenWidthDp + (insets.getLeft(this, layoutDirection) + insets.getRight(this, layoutDirection)).toDp().value
    }
}

internal fun Context.getActivityOrNull(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    log(ERROR) { "$LOG_PREFIX couldn't get Activity from $this" }
    return null
}

internal fun Context.getProgressCustomColorOrNull(): Int? {
    val typedValue = TypedValue()
    if (!theme.resolveAttribute(R.attr.adapty_progressIndicatorColor, typedValue, true))
        return null
    return when (typedValue.type) {
        in TypedValue.TYPE_FIRST_COLOR_INT..TypedValue.TYPE_LAST_COLOR_INT ->
            typedValue.data
        TypedValue.TYPE_REFERENCE ->
            kotlin.runCatching {
                ContextCompat.getColor(this, typedValue.resourceId)
            }.getOrNull()
        else -> null
    }
}

internal inline fun <T> withAdaptyUIActivated(body: () -> T): T {
    return runCatching { body() }.getOrElse { e ->
        if (e is NullPointerException) {
            log(WARN) { "$LOG_PREFIX Ensuring AdaptyUI initialization..." }
            ensureAdaptyUIInitialized()
            body()
        } else {
            throw e
        }
    }
}

private fun ensureAdaptyUIInitialized() {
    AdaptyUI
}

@InternalAdaptyApi
@Composable
public inline fun <reified T: Asset> Assets.getAsset(assetId: String): Asset.Composite<T>? {
    val customAsset = getAsset(assetId, true)?.let { it as? T }
    val defaultAsset = getAsset(assetId, false)?.let { it as? T }

    return when {
        customAsset != null && defaultAsset != null -> Asset.Composite(customAsset, defaultAsset)
        defaultAsset != null -> Asset.Composite(defaultAsset)
        else -> null
    }
}

@PublishedApi
@Composable
internal fun Assets.getAsset(assetId: String, isCustom: Boolean): Asset? {
    if (!isCustom) {
        return if (!isSystemInDarkTheme())
            get(assetId)
        else
            get("${assetId}${DARK_THEME_ASSET_SUFFIX}") ?: get(assetId)
    }
    return getAsset("${assetId}${CUSTOM_ASSET_SUFFIX}", false)
}

private val logExecutor = Executors.newSingleThreadExecutor()

@InternalAdaptyApi
public fun log(messageLogLevel: AdaptyLogLevel, msg: () -> String) {
    logExecutor.execute { com.adapty.internal.utils.log(messageLogLevel, msg) }
}