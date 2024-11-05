package com.adapty.ui.internal.utils

import android.content.Context
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.models.AdaptyEligibility.ELIGIBLE
import com.adapty.models.AdaptyPaywallProduct
import com.adapty.models.AdaptyProductDiscountPhase
import com.adapty.utils.AdaptyLogLevel
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

private val logExecutor = Executors.newSingleThreadExecutor()

@OptIn(InternalAdaptyApi::class)
internal fun log(messageLogLevel: AdaptyLogLevel, msg: () -> String) {
    logExecutor.execute { com.adapty.internal.utils.log(messageLogLevel, msg) }
}