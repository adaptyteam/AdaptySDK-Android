package com.adapty.internal.data.models

import androidx.annotation.RestrictTo
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.PurchaseHistoryRecord

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class PurchaseHistoryRecordModel(
    val purchase: PurchaseHistoryRecord,
    @BillingClient.SkuType val type: String,
    val transactionId: String?,
)