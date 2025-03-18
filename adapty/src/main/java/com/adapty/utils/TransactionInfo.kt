package com.adapty.utils

public sealed class TransactionInfo {
    internal class Id(val transactionId: String): TransactionInfo() {
        override fun toString(): String {
            return "TransactionInfo(transactionId='$transactionId')"
        }
    }
    internal class Purchase(val purchase: com.android.billingclient.api.Purchase): TransactionInfo() {
        override fun toString(): String {
            return "TransactionInfo(purchase.orderId=${purchase.orderId})"
        }
    }

    public companion object {

        /**
         * @param[transactionId] A string identifier (`purchase.getOrderId()`) of the purchase,
         * where the purchase is an instance of the billing library [Purchase](https://developer.android.com/reference/com/android/billingclient/api/Purchase) class.
         *
         * @see <a href="https://adapty.io/docs/report-transactions-observer-mode">Report transactions in Observer mode</a>
         */
        @JvmStatic
        public fun fromId(transactionId: String): TransactionInfo =
            Id(transactionId)

        /**
         * @param[purchase] An instance of the billing library [Purchase](https://developer.android.com/reference/com/android/billingclient/api/Purchase) class.
         *
         * @see <a href="https://adapty.io/docs/report-transactions-observer-mode">Report transactions in Observer mode</a>
         */
        @JvmStatic
        public fun fromPurchase(purchase: com.android.billingclient.api.Purchase): TransactionInfo =
            Purchase(purchase)
    }
}