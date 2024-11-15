@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.models.AdaptyEligibility.ELIGIBLE
import com.adapty.models.AdaptyPaywallProduct
import com.adapty.models.AdaptyProductDiscountPhase
import com.adapty.ui.AdaptyUI.LocalizedViewConfiguration.Asset
import com.adapty.ui.internal.mapping.element.Assets
import com.adapty.utils.AdaptyLogLevel
import com.adapty.utils.AdaptyLogLevel.Companion.ERROR
import java.util.concurrent.Executors

internal fun Context.getCurrentLocale() =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        resources.configuration.locales.get(0)
    } else {
        resources.configuration.locale
    }

internal fun AdaptyPaywallProduct.firstDiscountOfferOrNull(): AdaptyProductDiscountPhase? {
    return subscriptionDetails?.let { subDetails ->
        subDetails.introductoryOfferPhases.firstOrNull()?.takeIf { subDetails.introductoryOfferEligibility == ELIGIBLE }
    }
}

internal fun getProductGroupKey(groupId: String) = "group_${groupId}"

internal inline fun <reified T> Map<*, *>.getAs(key: String) = this[key] as? T

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

@InternalAdaptyApi
@Composable
public fun Assets.getForCurrentSystemTheme(assetId: String): Asset? =
    if (!isSystemInDarkTheme())
        get(assetId)
    else
        get("${assetId}${DARK_THEME_ASSET_SUFFIX}") ?: get(assetId)

private val logExecutor = Executors.newSingleThreadExecutor()

@InternalAdaptyApi
public fun log(messageLogLevel: AdaptyLogLevel, msg: () -> String) {
    logExecutor.execute { com.adapty.internal.utils.log(messageLogLevel, msg) }
}