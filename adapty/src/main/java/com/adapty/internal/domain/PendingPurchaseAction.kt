package com.adapty.internal.domain

import androidx.annotation.RestrictTo
import com.android.billingclient.api.Purchase
import java.util.concurrent.TimeUnit

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal enum class PendingPurchaseAction { VALIDATE, KEEP, DROP }

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal val PENDING_PURCHASE_TTL_MILLIS = TimeUnit.DAYS.toMillis(7)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal fun resolvePendingPurchaseAction(
    purchaseState: Int?,
    savedAt: Long,
    now: Long,
): PendingPurchaseAction =
    when (purchaseState) {
        Purchase.PurchaseState.PURCHASED -> PendingPurchaseAction.VALIDATE
        null -> if (now - savedAt > PENDING_PURCHASE_TTL_MILLIS) PendingPurchaseAction.DROP else PendingPurchaseAction.KEEP
        else -> PendingPurchaseAction.KEEP
    }
