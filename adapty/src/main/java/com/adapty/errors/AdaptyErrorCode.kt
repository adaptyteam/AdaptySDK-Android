package com.adapty.errors

import com.android.billingclient.api.BillingClient.BillingResponseCode

enum class AdaptyErrorCode(val value: Int) {
    UNKNOWN(0),
    USER_CANCELED(2),
    ITEM_UNAVAILABLE(5),
    ADAPTY_NOT_INITIALIZED(20),
    PAYWALL_NOT_FOUND(21),
    PRODUCT_NOT_FOUND(22),
    INVALID_JSON(23),
    CURRENT_SUBSCRIPTION_TO_UPDATE_NOT_FOUND_IN_HISTORY(24),
    PENDING_PURCHASE(25),
    BILLING_SERVICE_TIMEOUT(97),
    FEATURE_NOT_SUPPORTED(98),
    BILLING_SERVICE_DISCONNECTED(99),
    BILLING_SERVICE_UNAVAILABLE(102),
    BILLING_UNAVAILABLE(103),
    DEVELOPER_ERROR(105),
    BILLING_ERROR(106),
    ITEM_ALREADY_OWNED(107),
    ITEM_NOT_OWNED(108),
    NO_PURCHASES_TO_RESTORE(1004),
    FALLBACK_PAYWALLS_NOT_REQUIRED(1008),
    AUTHENTICATION_ERROR(2002),
    BAD_REQUEST(2003),
    SERVER_ERROR(2004),
    REQUEST_FAILED(2005),
    MISSING_PARAMETER(2007);

    internal companion object {
        @JvmSynthetic
        internal fun fromNetwork(responseCode: Int): AdaptyErrorCode = when (responseCode) {
            429, in 500..599 -> SERVER_ERROR
            401, 403 -> AUTHENTICATION_ERROR
            in 400..499 -> BAD_REQUEST
            else -> REQUEST_FAILED
        }

        private val billingErrors = listOf(
            BILLING_SERVICE_TIMEOUT,
            FEATURE_NOT_SUPPORTED,
            BILLING_SERVICE_DISCONNECTED,
            USER_CANCELED,
            BILLING_SERVICE_UNAVAILABLE,
            BILLING_UNAVAILABLE,
            ITEM_UNAVAILABLE,
            DEVELOPER_ERROR,
            BILLING_ERROR,
            ITEM_ALREADY_OWNED,
            ITEM_NOT_OWNED
        )

        @JvmSynthetic
        internal fun fromBilling(value: Int): AdaptyErrorCode = when (value) {
            BillingResponseCode.USER_CANCELED -> USER_CANCELED
            BillingResponseCode.ITEM_UNAVAILABLE -> ITEM_UNAVAILABLE
            else -> {
                val value = value + 100
                billingErrors.firstOrNull { it.value == value } ?: BILLING_ERROR
            }
        }
    }
}