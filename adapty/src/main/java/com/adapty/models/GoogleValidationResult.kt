package com.adapty.models

import com.google.gson.annotations.SerializedName

public class GoogleValidationResult(
    @SerializedName("kind")
    public val kind: String?,
    @SerializedName("startTimeMillis")
    public val startTimeMillis: String?,
    @SerializedName("expiryTimeMillis")
    public val expiryTimeMillis: String?,
    @SerializedName("autoResumeTimeMillis")
    public val autoResumeTimeMillis: String?,
    @SerializedName("autoRenewing")
    public val autoRenewing: Boolean?,
    @SerializedName("priceCurrencyCode")
    public val priceCurrencyCode: String?,
    @SerializedName("priceAmountMicros")
    public val priceAmountMicros: String?,
    @SerializedName("introductoryPriceInfo")
    public val introductoryPriceInfo: IntroductoryPriceInfo?,
    @SerializedName("countryCode")
    public val countryCode: String?,
    @SerializedName("developerPayload")
    public val developerPayload: String?,
    @SerializedName("paymentState")
    public val paymentState: Int?,
    @SerializedName("cancelReason")
    public val cancelReason: Int?,
    @SerializedName("userCancellationTimeMillis")
    public val userCancellationTimeMillis: String?,
    @SerializedName("cancelSurveyResult")
    public val cancelSurveyResult: SubscriptionCancelSurveyResult?,
    @SerializedName("orderId")
    public val orderId: String?,
    @SerializedName("linkedPurchaseToken")
    public val linkedPurchaseToken: String?,
    @SerializedName("purchaseType")
    public val purchaseType: Int?,
    @SerializedName("priceChange")
    public val priceChange: SubscriptionPriceChange?,
    @SerializedName("profileName")
    public val profileName: String?,
    @SerializedName("emailAddress")
    public val emailAddress: String?,
    @SerializedName("givenName")
    public val givenName: String?,
    @SerializedName("familyName")
    public val familyName: String?,
    @SerializedName("profileId")
    public val profileId: String?,
    @SerializedName("acknowledgementState")
    public val acknowledgementState: Int?,
    @SerializedName("externalAccountId")
    public val externalAccountId: String?,
    @SerializedName("promotionType")
    public val promotionType: Int?,
    @SerializedName("promotionCode")
    public val promotionCode: String?,
    @SerializedName("obfuscatedExternalAccountId")
    public val obfuscatedExternalAccountId: String?,
    @SerializedName("obfuscatedExternalProfileId")
    public val obfuscatedExternalProfileId: String?,
    @SerializedName("purchaseTimeMillis")
    public val purchaseTimeMillis: String?,
    @SerializedName("purchaseState")
    public val purchaseState: Int?,
    @SerializedName("consumptionState")
    public val consumptionState: Int?,
    @SerializedName("purchaseToken")
    public val purchaseToken: String?,
    @SerializedName("productId")
    public val productId: String?,
    @SerializedName("quantity")
    public val quantity: Int?,
    @SerializedName("regionCode")
    public val regionCode: String?,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GoogleValidationResult

        if (kind != other.kind) return false
        if (startTimeMillis != other.startTimeMillis) return false
        if (expiryTimeMillis != other.expiryTimeMillis) return false
        if (autoResumeTimeMillis != other.autoResumeTimeMillis) return false
        if (autoRenewing != other.autoRenewing) return false
        if (priceCurrencyCode != other.priceCurrencyCode) return false
        if (priceAmountMicros != other.priceAmountMicros) return false
        if (introductoryPriceInfo != other.introductoryPriceInfo) return false
        if (countryCode != other.countryCode) return false
        if (developerPayload != other.developerPayload) return false
        if (paymentState != other.paymentState) return false
        if (cancelReason != other.cancelReason) return false
        if (userCancellationTimeMillis != other.userCancellationTimeMillis) return false
        if (cancelSurveyResult != other.cancelSurveyResult) return false
        if (orderId != other.orderId) return false
        if (linkedPurchaseToken != other.linkedPurchaseToken) return false
        if (purchaseType != other.purchaseType) return false
        if (priceChange != other.priceChange) return false
        if (profileName != other.profileName) return false
        if (emailAddress != other.emailAddress) return false
        if (givenName != other.givenName) return false
        if (familyName != other.familyName) return false
        if (profileId != other.profileId) return false
        if (acknowledgementState != other.acknowledgementState) return false
        if (externalAccountId != other.externalAccountId) return false
        if (promotionType != other.promotionType) return false
        if (promotionCode != other.promotionCode) return false
        if (obfuscatedExternalAccountId != other.obfuscatedExternalAccountId) return false
        if (obfuscatedExternalProfileId != other.obfuscatedExternalProfileId) return false
        if (purchaseTimeMillis != other.purchaseTimeMillis) return false
        if (purchaseState != other.purchaseState) return false
        if (consumptionState != other.consumptionState) return false
        if (purchaseToken != other.purchaseToken) return false
        if (productId != other.productId) return false
        if (quantity != other.quantity) return false
        if (regionCode != other.regionCode) return false

        return true
    }

    override fun hashCode(): Int {
        var result = kind?.hashCode() ?: 0
        result = 31 * result + (startTimeMillis?.hashCode() ?: 0)
        result = 31 * result + (expiryTimeMillis?.hashCode() ?: 0)
        result = 31 * result + (autoResumeTimeMillis?.hashCode() ?: 0)
        result = 31 * result + (autoRenewing?.hashCode() ?: 0)
        result = 31 * result + (priceCurrencyCode?.hashCode() ?: 0)
        result = 31 * result + (priceAmountMicros?.hashCode() ?: 0)
        result = 31 * result + (introductoryPriceInfo?.hashCode() ?: 0)
        result = 31 * result + (countryCode?.hashCode() ?: 0)
        result = 31 * result + (developerPayload?.hashCode() ?: 0)
        result = 31 * result + (paymentState ?: 0)
        result = 31 * result + (cancelReason ?: 0)
        result = 31 * result + (userCancellationTimeMillis?.hashCode() ?: 0)
        result = 31 * result + (cancelSurveyResult?.hashCode() ?: 0)
        result = 31 * result + (orderId?.hashCode() ?: 0)
        result = 31 * result + (linkedPurchaseToken?.hashCode() ?: 0)
        result = 31 * result + (purchaseType ?: 0)
        result = 31 * result + (priceChange?.hashCode() ?: 0)
        result = 31 * result + (profileName?.hashCode() ?: 0)
        result = 31 * result + (emailAddress?.hashCode() ?: 0)
        result = 31 * result + (givenName?.hashCode() ?: 0)
        result = 31 * result + (familyName?.hashCode() ?: 0)
        result = 31 * result + (profileId?.hashCode() ?: 0)
        result = 31 * result + (acknowledgementState ?: 0)
        result = 31 * result + (externalAccountId?.hashCode() ?: 0)
        result = 31 * result + (promotionType ?: 0)
        result = 31 * result + (promotionCode?.hashCode() ?: 0)
        result = 31 * result + (obfuscatedExternalAccountId?.hashCode() ?: 0)
        result = 31 * result + (obfuscatedExternalProfileId?.hashCode() ?: 0)
        result = 31 * result + (purchaseTimeMillis?.hashCode() ?: 0)
        result = 31 * result + (purchaseState ?: 0)
        result = 31 * result + (consumptionState ?: 0)
        result = 31 * result + (purchaseToken?.hashCode() ?: 0)
        result = 31 * result + (productId?.hashCode() ?: 0)
        result = 31 * result + (quantity ?: 0)
        result = 31 * result + (regionCode?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "GoogleValidationResult(kind=$kind, startTimeMillis=$startTimeMillis, expiryTimeMillis=$expiryTimeMillis, autoResumeTimeMillis=$autoResumeTimeMillis, autoRenewing=$autoRenewing, priceCurrencyCode=$priceCurrencyCode, priceAmountMicros=$priceAmountMicros, introductoryPriceInfo=$introductoryPriceInfo, countryCode=$countryCode, developerPayload=$developerPayload, paymentState=$paymentState, cancelReason=$cancelReason, userCancellationTimeMillis=$userCancellationTimeMillis, cancelSurveyResult=$cancelSurveyResult, orderId=$orderId, linkedPurchaseToken=$linkedPurchaseToken, purchaseType=$purchaseType, priceChange=$priceChange, profileName=$profileName, emailAddress=$emailAddress, givenName=$givenName, familyName=$familyName, profileId=$profileId, acknowledgementState=$acknowledgementState, externalAccountId=$externalAccountId, promotionType=$promotionType, promotionCode=$promotionCode, obfuscatedExternalAccountId=$obfuscatedExternalAccountId, obfuscatedExternalProfileId=$obfuscatedExternalProfileId, purchaseTimeMillis=$purchaseTimeMillis, purchaseState=$purchaseState, consumptionState=$consumptionState, purchaseToken=$purchaseToken, productId=$productId, quantity=$quantity, regionCode=$regionCode)"
    }
}

public class IntroductoryPriceInfo(
    @SerializedName("introductoryPriceCurrencyCode")
    public val introductoryPriceCurrencyCode: String?,
    @SerializedName("introductoryPriceAmountMicros")
    public val introductoryPriceAmountMicros: String?,
    @SerializedName("introductoryPricePeriod")
    public val introductoryPricePeriod: String?,
    @SerializedName("introductoryPriceCycles")
    public val introductoryPriceCycles: Int?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as IntroductoryPriceInfo

        if (introductoryPriceCurrencyCode != other.introductoryPriceCurrencyCode) return false
        if (introductoryPriceAmountMicros != other.introductoryPriceAmountMicros) return false
        if (introductoryPricePeriod != other.introductoryPricePeriod) return false
        if (introductoryPriceCycles != other.introductoryPriceCycles) return false

        return true
    }

    override fun hashCode(): Int {
        var result = introductoryPriceCurrencyCode?.hashCode() ?: 0
        result = 31 * result + (introductoryPriceAmountMicros?.hashCode() ?: 0)
        result = 31 * result + (introductoryPricePeriod?.hashCode() ?: 0)
        result = 31 * result + (introductoryPriceCycles ?: 0)
        return result
    }

    override fun toString(): String {
        return "IntroductoryPriceInfo(introductoryPriceCurrencyCode=$introductoryPriceCurrencyCode, introductoryPriceAmountMicros=$introductoryPriceAmountMicros, introductoryPricePeriod=$introductoryPricePeriod, introductoryPriceCycles=$introductoryPriceCycles)"
    }
}

public class SubscriptionCancelSurveyResult(
    @SerializedName("userInputCancelReason")
    public val userInputCancelReason: String?,
    @SerializedName("cancelSurveyReason")
    public val cancelSurveyReason: Int?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SubscriptionCancelSurveyResult

        if (userInputCancelReason != other.userInputCancelReason) return false
        if (cancelSurveyReason != other.cancelSurveyReason) return false

        return true
    }

    override fun hashCode(): Int {
        var result = userInputCancelReason?.hashCode() ?: 0
        result = 31 * result + (cancelSurveyReason ?: 0)
        return result
    }

    override fun toString(): String {
        return "SubscriptionCancelSurveyResult(userInputCancelReason=$userInputCancelReason, cancelSurveyReason=$cancelSurveyReason)"
    }
}

public class SubscriptionPriceChange(
    @SerializedName("newPrice")
    public val newPrice: Price?,
    @SerializedName("state")
    public val state: Int?,
) {
    public class Price(
        @SerializedName("priceMicros")
        public val priceMicros: String?,
        @SerializedName("currency")
        public val currency: String?,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Price

            if (priceMicros != other.priceMicros) return false
            if (currency != other.currency) return false

            return true
        }

        override fun hashCode(): Int {
            var result = priceMicros?.hashCode() ?: 0
            result = 31 * result + (currency?.hashCode() ?: 0)
            return result
        }

        override fun toString(): String {
            return "Price(priceMicros=$priceMicros, currency=$currency)"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SubscriptionPriceChange

        if (newPrice != other.newPrice) return false
        if (state != other.state) return false

        return true
    }

    override fun hashCode(): Int {
        var result = newPrice?.hashCode() ?: 0
        result = 31 * result + (state ?: 0)
        return result
    }

    override fun toString(): String {
        return "SubscriptionPriceChange(newPrice=$newPrice, state=$state)"
    }
}