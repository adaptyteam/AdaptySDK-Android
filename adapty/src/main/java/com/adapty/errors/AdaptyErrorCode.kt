package com.adapty.errors

import com.android.billingclient.api.BillingClient.BillingResponseCode

public enum class AdaptyErrorCode(@get:JvmSynthetic internal val value: Int) {
    UNKNOWN(0),
    ITEM_UNAVAILABLE(5),
    ADAPTY_NOT_INITIALIZED(20),
    PRODUCT_NOT_FOUND(22),
    CURRENT_SUBSCRIPTION_TO_UPDATE_NOT_FOUND_IN_HISTORY(24),
    BILLING_SERVICE_TIMEOUT(97),
    FEATURE_NOT_SUPPORTED(98),
    BILLING_SERVICE_DISCONNECTED(99),
    BILLING_SERVICE_UNAVAILABLE(102),
    BILLING_UNAVAILABLE(103),
    DEVELOPER_ERROR(105),
    BILLING_ERROR(106),
    ITEM_ALREADY_OWNED(107),
    ITEM_NOT_OWNED(108),
    BILLING_NETWORK_ERROR(112),
    NO_PRODUCT_IDS_FOUND(1000),
    NO_PURCHASES_TO_RESTORE(1004),
    AUTHENTICATION_ERROR(2002),
    BAD_REQUEST(2003),
    SERVER_ERROR(2004),
    REQUEST_FAILED(2005),
    DECODING_FAILED(2006),
    ANALYTICS_DISABLED(3000),
    WRONG_PARAMETER(3001);

    public companion object {
        @Deprecated(
            "This constant has been deprecated, please replace it with 'WRONG_PARAMETER'",
            ReplaceWith("AdaptyErrorCode.WRONG_PARAMETER"),
            DeprecationLevel.ERROR,
        )
        @JvmField
        public val INVALID_JSON: AdaptyErrorCode = WRONG_PARAMETER

        @JvmSynthetic
        internal fun fromNetwork(responseCode: Int): AdaptyErrorCode = when (responseCode) {
            429, 499, in 500..599 -> SERVER_ERROR
            401, 403 -> AUTHENTICATION_ERROR
            in 400..499 -> BAD_REQUEST
            else -> REQUEST_FAILED
        }

        private val billingErrors = listOf(
            BILLING_SERVICE_TIMEOUT,
            FEATURE_NOT_SUPPORTED,
            BILLING_SERVICE_DISCONNECTED,
            BILLING_SERVICE_UNAVAILABLE,
            BILLING_UNAVAILABLE,
            ITEM_UNAVAILABLE,
            DEVELOPER_ERROR,
            BILLING_ERROR,
            ITEM_ALREADY_OWNED,
            ITEM_NOT_OWNED,
            BILLING_NETWORK_ERROR,
        )

        @JvmSynthetic
        internal fun fromBilling(value: Int): AdaptyErrorCode = when (value) {
            BillingResponseCode.ITEM_UNAVAILABLE -> ITEM_UNAVAILABLE
            else -> {
                val value = value + 100
                billingErrors.firstOrNull { it.value == value } ?: BILLING_ERROR
            }
        }
    }
}