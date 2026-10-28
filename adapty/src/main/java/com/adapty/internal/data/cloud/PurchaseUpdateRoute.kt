package com.adapty.internal.data.cloud

import androidx.annotation.RestrictTo
import com.android.billingclient.api.Purchase

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal enum class PurchaseUpdateRoute { PENDING_APPROVED, MAKE_PURCHASE, UNSOLICITED, UNSOLICITED_PENDING, SKIP }

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal fun resolvePurchaseUpdateRoute(
    purchaseState: Int,
    isStoredPending: Boolean,
    productId: String?,
    awaitedProductIds: Collection<String>,
): PurchaseUpdateRoute {
    val isPurchased = purchaseState == Purchase.PurchaseState.PURCHASED
    return when {
        isStoredPending -> if (isPurchased) PurchaseUpdateRoute.PENDING_APPROVED else PurchaseUpdateRoute.SKIP
        awaitedProductIds.isNotEmpty() && (productId == null || productId in awaitedProductIds) -> PurchaseUpdateRoute.MAKE_PURCHASE
        isPurchased -> PurchaseUpdateRoute.UNSOLICITED
        else -> PurchaseUpdateRoute.UNSOLICITED_PENDING
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal fun needsAwaitedPurchaseLookup(
    updates: Collection<Pair<PurchaseUpdateRoute, Long>>,
    isAwaiting: Boolean,
    launchedAt: Long,
): Boolean =
    isAwaiting && updates.none { (route, _) -> route == PurchaseUpdateRoute.MAKE_PURCHASE } &&
        updates.any { (route, purchaseTime) ->
            (route == PurchaseUpdateRoute.UNSOLICITED || route == PurchaseUpdateRoute.UNSOLICITED_PENDING) &&
                purchaseTime >= launchedAt
        }
