package com.adapty.api.entity.validate

import com.google.gson.annotations.SerializedName

class GoogleValidationResult {
    @SerializedName("kind")
    var kind: String? = null

    @SerializedName("startTimeMillis")
    var startTimeMillis: String? = null

    @SerializedName("expiryTimeMillis")
    var expiryTimeMillis: String? = null

    @SerializedName("autoResumeTimeMillis")
    var autoResumeTimeMillis: String? = null

    @SerializedName("autoRenewing")
    var autoRenewing: Boolean? = null

    @SerializedName("priceCurrencyCode")
    var priceCurrencyCode: String? = null

    @SerializedName("priceAmountMicros")
    var priceAmountMicros: String? = null

    @SerializedName("introductoryPriceInfo")
    var introductoryPriceInfo: IntroductoryPriceInfo? = null

    @SerializedName("countryCode")
    var countryCode: String? = null

    @SerializedName("developerPayload")
    var developerPayload: String? = null

    @SerializedName("paymentState")
    var paymentState: Int? = null

    @SerializedName("cancelReason")
    var cancelReason: Int? = null

    @SerializedName("userCancellationTimeMillis")
    var userCancellationTimeMillis: String? = null

    @SerializedName("cancelSurveyResult")
    var cancelSurveyResult: SubscriptionCancelSurveyResult? = null

    @SerializedName("orderId")
    var orderId: String? = null

    @SerializedName("linkedPurchaseToken")
    var linkedPurchaseToken: String? = null

    @SerializedName("purchaseType")
    var purchaseType: Int? = null

    @SerializedName("priceChange")
    var priceChange: SubscriptionPriceChange? = null

    @SerializedName("profileName")
    var profileName: String? = null

    @SerializedName("emailAddress")
    var emailAddress: String? = null

    @SerializedName("givenName")
    var givenName: String? = null

    @SerializedName("familyName")
    var familyName: String? = null

    @SerializedName("profileId")
    var profileId: String? = null

    @SerializedName("acknowledgementState")
    var acknowledgementState: Int? = null

    @SerializedName("externalAccountId")
    var externalAccountId: String? = null

    @SerializedName("promotionType")
    var promotionType: Int? = null

    @SerializedName("promotionCode")
    var promotionCode: String? = null

    @SerializedName("obfuscatedExternalAccountId")
    var obfuscatedExternalAccountId: String? = null

    @SerializedName("obfuscatedExternalProfileId")
    var obfuscatedExternalProfileId: String? = null

    @SerializedName("purchaseTimeMillis")
    var purchaseTimeMillis: String? = null

    @SerializedName("purchaseState")
    var purchaseState: Int? = null

    @SerializedName("consumptionState")
    var consumptionState: Int? = null

    @SerializedName("purchaseToken")
    var purchaseToken: String? = null

    @SerializedName("productId")
    var productId: String? = null

    @SerializedName("quantity")
    var quantity: Int? = null

    @SerializedName("regionCode")
    var regionCode: String? = null
}

class IntroductoryPriceInfo(
    @SerializedName("introductoryPriceCurrencyCode")
    val introductoryPriceCurrencyCode: String? = null,
    @SerializedName("introductoryPriceAmountMicros")
    val introductoryPriceAmountMicros: String? = null,
    @SerializedName("introductoryPricePeriod")
    val introductoryPricePeriod: String? = null,
    @SerializedName("introductoryPriceCycles")
    val introductoryPriceCycles: Int? = null
)

class SubscriptionCancelSurveyResult(
    @SerializedName("userInputCancelReason")
    val userInputCancelReason: String? = null,
    @SerializedName("cancelSurveyReason")
    val cancelSurveyReason: Int? = null
)

class SubscriptionPriceChange(
    @SerializedName("newPrice")
    val newPrice: Price? = null,
    @SerializedName("state")
    val state: Int? = null
)

class Price(
    @SerializedName("priceMicros")
    val priceMicros: String? = null,
    @SerializedName("currency")
    val currency: String? = null
)