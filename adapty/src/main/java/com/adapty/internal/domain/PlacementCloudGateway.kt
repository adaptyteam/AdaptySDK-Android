package com.adapty.internal.domain

import androidx.annotation.RestrictTo
import com.adapty.internal.data.cache.CacheRepository
import com.adapty.internal.data.cloud.CloudRepository
import com.adapty.internal.data.cloud.Response
import com.adapty.internal.data.models.BackendError.Companion.INCORRECT_SEGMENT_HASH_ERROR
import com.adapty.internal.data.models.ProfileDto
import com.adapty.internal.data.models.Variation
import com.adapty.internal.data.models.Variations

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class PlacementCloudGateway(
    private val cloudRepository: CloudRepository,
    private val cacheRepository: CacheRepository,
) {

    fun fetchVariations(
        placementId: String,
        locale: String,
        variationType: VariationType,
    ): Pair<Response<Variations>, ProfileDto> =
        withSegmentHashRetry { profile ->
            cloudRepository.getVariations(placementId, locale, profile.segmentId, variationType) to profile
        }

    fun fetchVariationById(
        placementId: String,
        locale: String,
        variationId: String,
        variationType: VariationType,
    ): Variation =
        withSegmentHashRetry { profile ->
            cloudRepository.getVariationById(placementId, locale, profile.segmentId, variationId, variationType)
                .persistRawSingleVariation(variationId, locale, variationType)
        }

    fun fetchFallbackVariations(
        placementId: String,
        locale: String,
        variationType: VariationType,
    ): Variations {
        val response = cloudRepository.getVariationsFallback(placementId, locale, variationType)
        val variations = response.data
        response.rawBody?.let { rawBody ->
            cacheRepository.saveRawVariations(placementId, variationType, rawBody, locale, null, variations.snapshotAt)
        }
        return variations
    }

    fun fetchFallbackVariationById(
        placementId: String,
        locale: String,
        variationId: String,
        variationType: VariationType,
    ): Variation {
        return cloudRepository.getVariationByIdFallback(placementId, locale, variationId, variationType)
            .persistRawSingleVariation(variationId, locale, variationType)
    }

    private fun <T> withSegmentHashRetry(call: (profile: ProfileDto) -> T): T {
        var profile = cacheRepository.getProfile() ?: cloudRepository.getProfile().data
        val segmentId = profile.segmentId
        try {
            return call(profile)
        } catch (error: Throwable) {
            val isIncorrectSegmentHash = error is Response.Error && error.backendError != null
                    && error.backendError.containsErrorCode(INCORRECT_SEGMENT_HASH_ERROR)
            if (!isIncorrectSegmentHash)
                throw error
            val cachedProfile = cacheRepository.getProfile()
            if (cachedProfile != null && segmentId != cachedProfile.segmentId)
                return withSegmentHashRetry(call)
            profile = cloudRepository.getProfile().data
            if (segmentId == profile.segmentId)
                throw error
            return withSegmentHashRetry(call)
        }
    }

    private fun Response<Variation>.persistRawSingleVariation(variationId: String, locale: String, variationType: VariationType): Variation {
        rawBody?.let { rawBody ->
            cacheRepository.saveRawSingleVariation(variationId, variationType, rawBody, locale, data.snapshotAt)
        }
        return data
    }
}
