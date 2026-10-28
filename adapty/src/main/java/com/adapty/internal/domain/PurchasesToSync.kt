package com.adapty.internal.domain

import androidx.annotation.RestrictTo
import com.adapty.internal.data.models.PurchaseRecordModel
import com.adapty.internal.data.models.SyncedPurchase

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal fun selectPurchasesToSync(
    records: List<PurchaseRecordModel>,
    syncedPurchases: Collection<SyncedPurchase>,
    pendingPurchaseTokens: Set<String>,
    byUser: Boolean,
): List<PurchaseRecordModel> {
    if (byUser) return records
    return records.filter { record ->
        record.purchaseToken !in pendingPurchaseTokens &&
            syncedPurchases.none { purchase ->
                purchase.purchaseToken == record.purchaseToken && purchase.purchaseTime == record.purchaseTime
            }
    }
}
