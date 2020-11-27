package com.adapty.api

class AdaptyError internal constructor(
    val originalError: Throwable? = null,
    val message: String = "",
    val adaptyErrorCode: AdaptyErrorCode = AdaptyErrorCode.UNKNOWN
)

enum class AdaptyErrorCode(val value: Int) {
    UNKNOWN(0),
    ADAPTY_NOT_INITIALIZED(1),
    EMPTY_PARAMETER(2),
    NO_HISTORY_PURCHASES(3),
    NO_NEW_PURCHASES(4),
    PAYWALL_NOT_FOUND(5),
    PRODUCT_NOT_FOUND(6),

    BILLING_SERVICE_TIMEOUT(97),
    FEATURE_NOT_SUPPORTED(98),
    BILLING_SERVICE_DISCONNECTED(99),
    USER_CANCELED(101),
    BILLING_SERVICE_UNAVAILABLE(102),
    BILLING_UNAVAILABLE(103),
    ITEM_UNAVAILABLE(104),
    DEVELOPER_ERROR(105),
    BILLING_ERROR(106),
    ITEM_ALREADY_OWNED(107),
    ITEM_NOT_OWNED(108),

    AUTHENTICATION_ERROR(201), // You need to be authenticated first
    BAD_REQUEST(202), // Bad request
    REQUEST_OUTDATED(203), // The url you requested is outdated
    REQUEST_FAILED(204); // Network request failed

    internal companion object {
        internal fun fromNetwork(responseCode: Int): AdaptyErrorCode = when (responseCode) {
            in 401..499 -> AUTHENTICATION_ERROR
            in 500..599 -> BAD_REQUEST
            600 -> REQUEST_OUTDATED
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

        internal fun fromBilling(value: Int): AdaptyErrorCode = if (value == 0) BILLING_ERROR else {
            val value = value + 100
            billingErrors.firstOrNull { it.value == value } ?: BILLING_ERROR
        }
    }
}