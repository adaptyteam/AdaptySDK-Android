package com.adapty.internal.utils

import androidx.annotation.RestrictTo
import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.data.models.PromoDto
import com.adapty.models.PaywallModel
import com.adapty.models.PromoModel

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal object PromoMapper {

    @JvmSynthetic
    fun map(promoDto: PromoDto, paywall: PaywallModel) = PromoModel(
        promoType = promoDto.promoType ?: throw AdaptyError(
            message = "promoType in PromoModel should not be null",
            adaptyErrorCode = AdaptyErrorCode.MISSING_PARAMETER
        ),
        variationId = promoDto.variationId ?: throw AdaptyError(
            message = "variationId in PromoModel should not be null",
            adaptyErrorCode = AdaptyErrorCode.MISSING_PARAMETER
        ),
        expiresAt = promoDto.expiresAt,
        paywall = paywall
    )
}