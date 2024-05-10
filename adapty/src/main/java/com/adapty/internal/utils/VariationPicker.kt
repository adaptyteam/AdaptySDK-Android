@file:OptIn(InternalAdaptyApi::class)

package com.adapty.internal.utils

import androidx.annotation.RestrictTo
import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.data.models.PaywallDto
import java.math.BigInteger

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class VariationPicker(
    private val hashingHelper: HashingHelper,
) {

    @JvmSynthetic
    fun pick(
        variations: Collection<PaywallDto>,
        profileId: String,
    ): PaywallDto? {
        val sortedVariations = variations.sortedWith(compareBy(PaywallDto::weight, PaywallDto::variationId))
        val placementAudienceVersionId = sortedVariations.first().placementAudienceVersionId ?: throw AdaptyError(
            message = "placementAudienceVersionId in Paywall should not be null",
            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
        )
        val desiredWeight = "$placementAudienceVersionId-$profileId".let { str ->
            val bytes = hashingHelper.hashBytes(str, HashingHelper.MD5).takeLast(8).toByteArray()
            val hexStr = hashingHelper.toHexString(bytes)
            (BigInteger(hexStr, 16) % BigInteger("100")).toInt()
        }
        var desiredVariation: PaywallDto? = null
        var cumulativeWeight = 0
        for (variation in sortedVariations) {
            val weight = variation.weight?.takeIf { it in 1..100 } ?: throw AdaptyError(
                message = "weight in Paywall should be between 1 and 100. Currently, it is ${variation.weight}",
                adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
            )
            cumulativeWeight += weight
            if (cumulativeWeight >= desiredWeight) {
                desiredVariation = variation
                break
            }
        }
        return desiredVariation
    }
}