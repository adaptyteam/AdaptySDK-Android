package com.adapty.internal.utils

import android.net.Uri
import androidx.annotation.RestrictTo
import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.data.cache.CacheRepository
import com.adapty.models.AdaptyPaywall
import com.adapty.models.AdaptyPaywallProduct
import com.adapty.models.AdaptyProductDiscountPhase
import java.util.Locale

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class WebPaywallUrlCreator(
    private val cacheRepository: CacheRepository,
) {

    fun create(paywall: AdaptyPaywall): Uri {
        val webPurchaseUrl = paywall.webPurchaseUrl
            ?: throw AdaptyError(
                message = "Web purchase URL is missing",
                adaptyErrorCode = AdaptyErrorCode.WRONG_PARAMETER
            )
        return buildUri(webPurchaseUrl, paywall.variationId, null)
    }

    fun create(product: AdaptyPaywallProduct): Uri {
        val webPurchaseUrl = product.webPurchaseUrl
            ?: throw AdaptyError(
                message = "Web purchase URL is missing",
                adaptyErrorCode = AdaptyErrorCode.WRONG_PARAMETER
            )
        return buildUri(webPurchaseUrl, product.variationId, product)
    }

    private fun buildUri(
        webPurchaseUrl: String,
        variationId: String,
        product: AdaptyPaywallProduct?,
    ): Uri {
        return runCatching { Uri.parse(webPurchaseUrl) }
            .getOrElse {
                throw AdaptyError(
                    message = "Web purchase URL is invalid",
                    adaptyErrorCode = AdaptyErrorCode.WRONG_PARAMETER
                )
            }
            .buildUpon()
            .apply {
                appendQueryParameter("adapty_profile_id", cacheRepository.getProfileId())
                appendQueryParameter("adapty_variation_id", variationId)

                product?.let {
                    appendQueryParameter("adapty_product_id", product.payloadData.adaptyProductId)
                    appendQueryParameter("adapty_chosen_product_idx", "${product.payloadData.paywallProductIndex}")

                    product.subscriptionDetails?.introductoryOfferPhases?.firstOrNull()?.let { offer ->
                        appendQueryParameter("adapty_offer_category", "introductory")
                        appendQueryParameter(
                            "adapty_offer_type",
                            when (offer.paymentMode) {
                                AdaptyProductDiscountPhase.PaymentMode.FREE_TRIAL -> "free_trial"
                                AdaptyProductDiscountPhase.PaymentMode.PAY_AS_YOU_GO -> "pay_as_you_go"
                                AdaptyProductDiscountPhase.PaymentMode.PAY_UPFRONT -> "pay_up_front"
                                AdaptyProductDiscountPhase.PaymentMode.UNKNOWN -> "unknown"
                            },
                        )
                        appendQueryParameter(
                            "adapty_offer_period_units",
                            offer.subscriptionPeriod.unit.name.lowercase(Locale.ENGLISH),
                        )
                        appendQueryParameter("adapty_offer_number_of_units", "${offer.subscriptionPeriod.numberOfUnits}")
                    }
                }
            }
            .build()
    }
}
