package com.adapty.internal.data.models

import com.adapty.errors.AdaptyError
import com.android.billingclient.api.Purchase

internal sealed class PurchaseResult {

    class Success(
        val purchase: Purchase? = null,
        val state: State = State.PURCHASED,
    ): PurchaseResult() {

        val productId get() = purchase?.products?.firstOrNull()

        enum class State { PURCHASED, PENDING }
    }

    object Canceled: PurchaseResult()

    class Error(val error: AdaptyError): PurchaseResult()
}