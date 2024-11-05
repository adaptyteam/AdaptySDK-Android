package com.adapty.ui.internal.utils

import com.adapty.errors.AdaptyError
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.models.AdaptyPaywallProduct
import com.adapty.models.AdaptyProfile
import com.adapty.models.AdaptyPurchasedInfo
import com.adapty.models.AdaptySubscriptionUpdateParameters
import com.adapty.ui.internal.ui.element.Action
import java.util.Date

@InternalAdaptyApi
public interface EventCallback {
    public fun onRestoreStarted()
    public fun onRestoreSuccess(profile: AdaptyProfile)
    public fun onRestoreFailure(error: AdaptyError)
    public fun onAwaitingSubscriptionUpdateParams(product: AdaptyPaywallProduct): AdaptySubscriptionUpdateParameters?
    public fun onPurchaseStarted(product: AdaptyPaywallProduct)
    public fun onPurchaseSuccess(
        purchasedInfo: AdaptyPurchasedInfo?,
        product: AdaptyPaywallProduct,
    )
    public fun onPurchaseCanceled(product: AdaptyPaywallProduct)
    public fun onPurchaseFailure(
        error: AdaptyError,
        product: AdaptyPaywallProduct,
    )
    public fun onActions(actions: List<Action>)
    public fun getTimerStartTimestamp(timerId: String, isPersisted: Boolean): Long?
    public fun setTimerStartTimestamp(timerId: String, value: Long, isPersisted: Boolean)
    public fun timerEndAtDate(timerId: String): Date
}